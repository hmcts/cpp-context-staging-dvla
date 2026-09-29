package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.PENDING;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.cpp.stagingdvla.event.DocumentDeletedFromBlob;
import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus;

import java.util.UUID;
import java.util.stream.Stream;

public class MaterialAggregate implements Aggregate {
    private static final long serialVersionUID = 102L;
    private MaterialDetails details;
    private boolean documentDeliveryTracked;
    private boolean isSjpCase = false;
    private boolean isMaterialStatusSuccess;
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
                            if(DvlaDocumentDeliveryMaterialStatus.SUCCESS.name().equals(e.getMaterialStatus())){
                                this.isMaterialStatusSuccess = true;
                            }
                        }
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
                                                 final UUID caseId, final UUID sjpCorrelationId, final String sjpStatus) {
        // tracking starts with the blob PENDING record; anything else is ignored until then
        if (!documentDeliveryTracked && !isPendingWithBlob(materialStatus, payloadBlobUri)) {
            return Stream.empty();
        }
        final Stream.Builder<Object> events = Stream.builder();
        events.add(DvlaDocumentDeliveryRecorded.dvlaDocumentDeliveryRecorded()
                .withMaterialId(materialId)
                .withMaterialStatus(materialStatus)
                .withPayloadBlobUri(payloadBlobUri)
                .withDocumentBlobUri(documentBlobUri)
                .withCaseId(caseId)
                .withSjpCorrelationId(sjpCorrelationId)
                .withSjpStatus(sjpStatus)
                .build());
        if((!this.isSjpCase && DvlaDocumentDeliveryMaterialStatus.SUCCESS.name().equals(materialStatus)) ||
                (this.isSjpCase && DvlaDocumentDeliveryMaterialStatus.SUCCESS.name().equals(sjpStatus))){
            events.add(DocumentDeletedFromBlob.documentDeletedFromBlob()
                    .withMaterialId(materialId)
                    .withPayloadBlobUri(this.payloadBlobUri)
                    .withDocumentBlobUri(this.documentBlobUri)
                    .build());
        }

        return apply(events.build());
    }

    private static boolean isPendingWithBlob(final String materialStatus, final String payloadBlobUri) {
        return PENDING.name().equals(materialStatus) && nonNull(payloadBlobUri);
    }

}
