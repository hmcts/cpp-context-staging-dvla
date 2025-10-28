package uk.gov.moj.cpp.stagingdvla.handler.command.api;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.FeatureControl;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(COMMAND_API)
public class DriverNotificationApi {

    private static final Logger LOGGER = getLogger(DriverNotificationApi.class);

    protected static final String STAGINGDVLA_COMMAND_DRIVER_NOTIFICATION = "stagingdvla.command.driver-notification";
    protected static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION = "stagingdvla.command.handler.driver-notification";

    private final Sender sender;

    @Inject
    public DriverNotificationApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles(STAGINGDVLA_COMMAND_DRIVER_NOTIFICATION)
    public void notifyDriver(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Received request {} {}", STAGINGDVLA_COMMAND_DRIVER_NOTIFICATION, envelope.toObfuscatedDebugString());
        }
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION)
                .withMetadataFrom(envelope));
    }
}
