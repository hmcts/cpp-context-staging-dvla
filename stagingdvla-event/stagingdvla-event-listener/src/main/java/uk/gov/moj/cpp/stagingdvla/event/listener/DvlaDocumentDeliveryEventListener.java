package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDelivered;
import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDeliveryFailed;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;
import uk.gov.moj.cpp.stagingdvla.domain.constants.EmailDeliveryStatus;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Projects the D20 email delivery outcome onto {@code dvla_document_delivery.email_status}.
 */
@ServiceComponent(EVENT_LISTENER)
public class DvlaDocumentDeliveryEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DvlaDocumentDeliveryEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Handles("stagingdvla.event.email-notification-delivered")
    public void handleEmailNotificationDelivered(final JsonEnvelope jsonEnvelope) {
        final EmailNotificationDelivered event = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), EmailNotificationDelivered.class);
        updateEmailStatus(event.getMaterialId(), EmailDeliveryStatus.SUCCESS);
    }

    @Handles("stagingdvla.event.email-notification-delivery-failed")
    public void handleEmailNotificationDeliveryFailed(final JsonEnvelope jsonEnvelope) {
        final EmailNotificationDeliveryFailed event = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), EmailNotificationDeliveryFailed.class);
        LOGGER.warn("D20 email notification failed for materialId {}: {} (statusCode {})",
                event.getMaterialId(), event.getErrorMessage(), event.getStatusCode());
        updateEmailStatus(event.getMaterialId(), EmailDeliveryStatus.FAILED);
    }

    private void updateEmailStatus(final UUID materialId, final EmailDeliveryStatus status) {

        final DvlaDocumentDeliveryEntity entity = dvlaDocumentDeliveryRepository.findBy(materialId);

        if (isNull(entity)) {
            // Expected until row creation is implemented - nothing writes rows to
            // dvla_document_delivery yet. The outcome is still recorded on this context's event
            // stream, so a viewstore rebuild after row creation lands will backfill this column.
            LOGGER.warn("No dvla_document_delivery row for materialId {} - email status {} not projected",
                    materialId, status.getStatus());
            return;
        }

        if (EmailDeliveryStatus.isTerminal(entity.getEmailStatus())) {
            // Terminal states are sticky, which makes this idempotent under JMS redelivery and
            // harmless if the two outcome events ever arrive out of order.
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("email_status for materialId {} is already {} - leaving it unchanged",
                        materialId, entity.getEmailStatus());
            }
            return;
        }

        entity.setEmailStatus(status.getStatus());
        dvlaDocumentDeliveryRepository.save(entity);
    }
}
