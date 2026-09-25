package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus.PENDING;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.UUID;
import java.util.stream.Stream;

public class MaterialAggregate implements Aggregate {
    private static final long serialVersionUID = 101L;
    private MaterialDetails details;
    private boolean documentDeliveryTracked;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NowsMaterialRequestRecorded.class).apply(e ->
                        details = e.getContext()
                ),
                when(DvlaDocumentDeliveryRecorded.class).apply(e ->
                        documentDeliveryTracked = true
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
        return apply(Stream.of(DvlaDocumentDeliveryRecorded.dvlaDocumentDeliveryRecorded()
                .withMaterialId(materialId)
                .withMaterialStatus(materialStatus)
                .withPayloadBlobUri(payloadBlobUri)
                .withDocumentBlobUri(documentBlobUri)
                .withCaseId(caseId)
                .withSjpCorrelationId(sjpCorrelationId)
                .withSjpStatus(sjpStatus)
                .build()));
    }

    private static boolean isPendingWithBlob(final String materialStatus, final String payloadBlobUri) {
        return PENDING.name().equals(materialStatus) && nonNull(payloadBlobUri);
    }

}
