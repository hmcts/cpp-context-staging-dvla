package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.FAILED;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.PENDING;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.SUCCESS;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.cpp.stagingdvla.event.DocumentDeletedFromBlob;
import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryEmailStatus;

import java.util.UUID;
import java.util.stream.Stream;

public class MaterialAggregate implements Aggregate {
    private static final long serialVersionUID = 102L;
    private MaterialDetails details;
    private boolean documentDeliveryTracked;
    private boolean isSjpCase = false;
    private boolean isMaterialStatusSuccess;
    private boolean isSjpStatusSuccess;
    private boolean isSjpStatusCompleted;
    private String emailStatus;
    private boolean documentDeletedFromBlob;
    private String payloadBlobUri;
    private String documentBlobUri;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NowsMaterialRequestRecorded.class).apply(e ->
                        details = e.getContext()
                ),
                when(DvlaDocumentDeliveryRecorded.class).apply(e -> {
                            this.documentDeliveryTracked = true;
                            if(nonNull(e.getPayloadBlobUri())) {
                                this.payloadBlobUri = e.getPayloadBlobUri();
                            }
                            if(nonNull(e.getDocumentBlobUri())) {
                                this.documentBlobUri = e.getDocumentBlobUri();
                            }
                            if (!this.isSjpCase && nonNull(e.getCaseId())){
                                this.isSjpCase = true;
                            }
                            if(SUCCESS.name().equals(e.getMaterialStatus())){
                                this.isMaterialStatusSuccess = true;
                            }
                            if (SUCCESS.name().equals(e.getSjpStatus())) {
                                this.isSjpStatusSuccess = true;
                            }
                            if (SUCCESS.name().equals(e.getSjpStatus()) || FAILED.name().equals(e.getSjpStatus())) {
                                this.isSjpStatusCompleted = true;
                            }
                            if (nonNull(e.getEmailStatus())) {
                                this.emailStatus = e.getEmailStatus();
                            }
                        }
                ),
                when(DocumentDeletedFromBlob.class).apply(e ->
                        this.documentDeletedFromBlob = true
                ),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> create(final MaterialDetails materialDetails) {
        return apply(Stream.of(NowsMaterialRequestRecorded
                .nowsMaterialRequestRecorded()
                .withContext(materialDetails).build()));
    }

    public Stream<Object> dvlaMaterialAdded() {
        if(nonNull(details) && nonNull(details.getEmailNotifications())) {
            return Stream.of(new EmailNotificationSent(this.details));
        }
        return null;
    }

    public Stream<Object> recordDocumentDelivery(final UUID materialId, final String materialStatus,
                                                 final String payloadBlobUri, final String documentBlobUri,
                                                 final UUID caseId, final UUID sjpCorrelationId, final String sjpStatus,
                                                 final String emailStatus) {
        // tracking starts with the blob PENDING record; anything else is ignored until then
        if (!documentDeliveryTracked && !isPendingWithBlob(materialStatus, payloadBlobUri)) {
            return Stream.empty();
        }
        final Stream.Builder<Object> events = Stream.builder();
        final DvlaDocumentDeliveryRecorded.Builder builder = DvlaDocumentDeliveryRecorded.dvlaDocumentDeliveryRecorded();
        builder.withMaterialId(materialId)
                .withMaterialStatus(materialStatus)
                .withPayloadBlobUri(payloadBlobUri)
                .withDocumentBlobUri(documentBlobUri)
                .withCaseId(caseId)
                .withSjpCorrelationId(sjpCorrelationId)
                .withSjpStatus(sjpStatus)
                .withEmailStatus(emailStatus);

        // the sjp PENDING record is sent right after the SJP upload; if SJP's outcome (SUCCESS/FAILED)
        // got recorded first, a late PENDING must not take the view's sjp status back to PENDING
        if (PENDING.name().equals(sjpStatus) && this.isSjpStatusCompleted) {
            builder.withSjpStatus(null);
        }

        // material SUCCESS is recorded on material-added, which is also what triggers the D20 email
        // to DVLA (dvlaMaterialAdded) - so the email status is decided here: PENDING when the material
        // carries email notifications, NOT_REQUIRED otherwise. An email outcome (SUCCESS/FAILED) already
        // recorded - the material SUCCESS record was delayed past the email round trip - is kept, not reset
        if (SUCCESS.name().equals(materialStatus) && isNull(this.emailStatus)) {
            final boolean emailNotificationRequired = nonNull(details) && nonNull(details.getEmailNotifications());
            builder.withEmailStatus(emailNotificationRequired
                    ? DvlaDocumentDeliveryEmailStatus.PENDING.name()
                    : DvlaDocumentDeliveryEmailStatus.NOT_REQUIRED.name());
        }
        final DvlaDocumentDeliveryRecorded recorded = builder.build();
        events.add(recorded);
        if (isDeliveryCompleteAfter(recorded)) {
            events.add(DocumentDeletedFromBlob.documentDeletedFromBlob()
                    .withMaterialId(materialId)
                    .withPayloadBlobUri(this.payloadBlobUri)
                    .withDocumentBlobUri(this.documentBlobUri)
                    .build());
        }

        return apply(events.build());
    }

    // the blob is deleted once delivery is complete: material SUCCESS, email NOT_REQUIRED or SUCCESS,
    // and - for an SJP case - sjp SUCCESS. The statuses arrive on separate records in any order, so the
    // aggregate's state is merged with the incoming record (not yet applied) and checked as a whole
    private boolean isDeliveryCompleteAfter(final DvlaDocumentDeliveryRecorded recorded) {
        if (this.documentDeletedFromBlob) {
            return false;
        }
        final boolean materialSucceeded = this.isMaterialStatusSuccess || SUCCESS.name().equals(recorded.getMaterialStatus());
        final String currentEmailStatus = nonNull(recorded.getEmailStatus()) ? recorded.getEmailStatus() : this.emailStatus;
        final boolean emailCompleted = DvlaDocumentDeliveryEmailStatus.NOT_REQUIRED.name().equals(currentEmailStatus)
                || DvlaDocumentDeliveryEmailStatus.SUCCESS.name().equals(currentEmailStatus);
        final boolean sjpCase = this.isSjpCase || nonNull(recorded.getCaseId());
        final boolean sjpSucceeded = this.isSjpStatusSuccess || SUCCESS.name().equals(recorded.getSjpStatus());
        return materialSucceeded && emailCompleted && (!sjpCase || sjpSucceeded);
    }

    private static boolean isPendingWithBlob(final String materialStatus, final String payloadBlobUri) {
        return PENDING.name().equals(materialStatus) && nonNull(payloadBlobUri);
    }

}
