package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DvlaDocumentDeliveryMaterialStatus;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;

public class DocumentDeliveryStatusService {

    public static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY = "stagingdvla.command.handler.driver-notification-document-delivery";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    public void record(final Metadata originatingMetadata, final DocumentDelivery documentDelivery) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("materialId", documentDelivery.materialId().toString());
        addIfPresent(payload, "materialStatus", documentDelivery.materialStatus());
        addIfPresent(payload, "payloadBlobUri", documentDelivery.payloadBlobUri());
        addIfPresent(payload, "documentBlobUri", documentDelivery.documentBlobUri());
        addIfPresent(payload, "caseId", documentDelivery.caseId());
        addIfPresent(payload, "sjpCorrelationId", documentDelivery.sjpCorrelationId());
        addIfPresent(payload, "sjpStatus", documentDelivery.sjpStatus());

        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(originatingMetadata)
                        .withName(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY),
                payload.build()));
    }

    private static void addIfPresent(final JsonObjectBuilder builder, final String key, final Object value) {
        if (nonNull(value)) {
            builder.add(key, value.toString());
        }
    }

    public record DocumentDelivery(UUID materialId,
                                   DvlaDocumentDeliveryMaterialStatus materialStatus,
                                   String payloadBlobUri,
                                   String documentBlobUri,
                                   UUID caseId,
                                   UUID sjpCorrelationId,
                                   DvlaDocumentDeliveryMaterialStatus sjpStatus) {

        public static DocumentDelivery material(final UUID materialId, final DvlaDocumentDeliveryMaterialStatus materialStatus) {
            return new DocumentDelivery(materialId, materialStatus, null, null, null, null, null);
        }

        public static DocumentDelivery material(final UUID materialId, final DvlaDocumentDeliveryMaterialStatus materialStatus,
                                                final String payloadBlobUri, final String documentBlobUri) {
            return new DocumentDelivery(materialId, materialStatus, payloadBlobUri, documentBlobUri, null, null, null);
        }

        public static DocumentDelivery sjpCase(final UUID materialId, final UUID caseId, final UUID sjpCorrelationId,
                                               final DvlaDocumentDeliveryMaterialStatus sjpStatus) {
            return new DocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, sjpStatus);
        }
    }
}
