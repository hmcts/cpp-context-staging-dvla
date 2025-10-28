package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryCancelled;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryScheduled;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;
import uk.gov.moj.cpp.persistence.repository.DrivingConvictionRetryRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class DriverNotifiedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverNotifiedEventListener.class);

    @Inject
    DrivingConvictionRetryRepository notificationRetryRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("stagingdvla.event.driver-notified")
    public void handleDriverNotified(final JsonEnvelope event) {
        final DriverNotified payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), DriverNotified.class);

        if (nonNull(payload) && nonNull(payload.getRetrySequence()) && payload.getRetrySequence() > 0) {
            removeEntity(payload.getIdentifier());
            LOGGER.info("driver-notified retry entity removed (if it exists) for convictionId: {}, masterDefendantId: {}", payload.getIdentifier(), payload.getMasterDefendantId());
        }
    }

    @Handles("stagingdvla.event.driver-notified-next-retry-scheduled")
    public void handleDriverNotifiedNextRetryScheduled(final JsonEnvelope event) {
        final DriverNotifiedNextRetryScheduled payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), DriverNotifiedNextRetryScheduled.class);

        if (nonNull(payload) && nonNull(payload.getConvictionId())) {
            notificationRetryRepository.save(new DrivingConvictionRetryEntity(payload.getConvictionId(), payload.getMasterDefendantId(), ZonedDateTime.now()));
            LOGGER.info("driver-notified-next-retry-scheduled for convictionId: {}, masterDefendantId: {}", payload.getConvictionId(), payload.getMasterDefendantId());
        }
    }

    @Handles("stagingdvla.event.driver-notified-next-retry-cancelled")
    public void handleDriverNotifiedNextRetryCancelled(final JsonEnvelope event) {
        final DriverNotifiedNextRetryCancelled payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), DriverNotifiedNextRetryCancelled.class);

        if (nonNull(payload) && nonNull(payload.getConvictionId())) {
            removeEntity(payload.getConvictionId());
            LOGGER.info("driver-notified-next-retry-cancelled for convictionId: {}, masterDefendantId: {}", payload.getConvictionId(), payload.getMasterDefendantId());
        }
    }

    private void removeEntity(final UUID convictionId) {
        if (nonNull(convictionId)) {
            final DrivingConvictionRetryEntity entity = notificationRetryRepository.findBy(convictionId);
            if (nonNull(entity)) {
                notificationRetryRepository.remove(entity);
            }
        }
    }
}
