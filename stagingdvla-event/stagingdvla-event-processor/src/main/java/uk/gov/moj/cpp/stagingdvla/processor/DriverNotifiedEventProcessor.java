package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.EndorsementType.NEW;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.getEndorsementType;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.hasMultipleConvictingCourts;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.hasMultipleConvictionDates;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.CLIENT_CONTEXT;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.CLIENT_CONTEXT_PREFIX;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.Personalisation;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.exception.NotifyDrivingConvictionException;
import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConvictionResponse;
import uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService;
import uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService;
import uk.gov.moj.cpp.stagingdvla.service.NotifyDrivingConvictionService;
import uk.gov.moj.cpp.stagingdvla.service.scheduler.NotifyDrivingConvictionRetryScheduler;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class DriverNotifiedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverNotifiedEventProcessor.class.getCanonicalName());

    private static final String FIELD_NOTIFICATION_ID = "notificationId";
    private static final String FIELD_TEMPLATE_ID = "templateId";
    private static final String SEND_TO_ADDRESS = "sendToAddress";
    private static final String MATERIAL_URL = "materialUrl";
    private static final String PERSONALISATION = "personalisation";
    private static final String SUBJECT = "subject";
    private static final String CODE_FOR_SJP_CASE = "J";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DocumentGeneratorService documentGeneratorService;

    @Inject
    private NotificationNotifyService notificationNotifyService;

    @Inject
    private NotifyDrivingConvictionService notifyDrivingConvictionService;

    @Inject
    private NotifyDrivingConvictionRetryScheduler scheduler;

    @Inject
    private DvlaApimConfig dvlaApimConfig;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("stagingdvla.event.driver-notified")
    public void handleDriverNotifiedEvent(final JsonEnvelope envelope) {
        final JsonObject requestJson = envelope.payloadAsJsonObject();
        final DriverNotified driverNotified = jsonObjectToObjectConverter.convert(requestJson, DriverNotified.class);
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Driver Notified event - {}", envelope.toObfuscatedDebugString());
        }

        if (NEW.equals(getEndorsementType(driverNotified))
                && !hasMultipleConvictionDates(driverNotified)
                && !hasMultipleConvictingCourts(driverNotified)) {
            if (containsACaseWithOffence(driverNotified)) {
                final NotifyDrivingConvictionResponse response = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
                postProcessResponse(response, envelope, driverNotified);
            } else {
                LOGGER.info("No endorsement on remaining offence, D20 not attempted in API  ");
            }
        }
        if(Optional.ofNullable(driverNotified.getRetrySequence()).orElse(0) == 0) {
            LOGGER.info("DriverNotifiedEventProcessor - Calling to systemdoc for document generation");
            if (!featureControlGuard.isFeatureEnabled("dvlaFileStore")) {
                documentGeneratorService.generateDvlaDocument(envelope, userId, driverNotified);
            } else {
                final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(driverNotified);
                documentGeneratorService.generateDocument(envelope, driverNotified.getMaterialId(),
                        nowsDocumentOrderJson,
                        sjpCaseIdOf(driverNotified));
            }
        }
    }

    // same SJP test SystemDocGeneratorEventProcessor applies when filing the document (initiation code "J")
    private static UUID sjpCaseIdOf(final DriverNotified driverNotified) {
        return driverNotified.getCases().stream()
                .filter(c -> CODE_FOR_SJP_CASE.equalsIgnoreCase(c.getInitiationCode()))
                .map(Cases::getCaseId)
                .findFirst()
                .orElse(null);
    }

    private boolean containsACaseWithOffence(final DriverNotified driverNotified) {
        return isNotEmpty(driverNotified.getCases()) && driverNotified.getCases()
                .stream()
                .anyMatch(aCase -> isNotEmpty(aCase.getDefendantCaseOffences()));
    }

    private void postProcessResponse(final NotifyDrivingConvictionResponse response, final JsonEnvelope envelope, final DriverNotified driverNotified) {
        if (isSuccessResponse(response)) {
            scheduler.dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.SUCCESS);
        } else if (isRetryableErrorResponse(response)) {
            scheduler.dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);
            scheduleNextRetryOrThrowExceptionIfMaxRetryReached(envelope, response, driverNotified);
        } else {
            throwNotifyDrivingConvictionException(response, driverNotified);
        }
    }

    private boolean isSuccessResponse(final NotifyDrivingConvictionResponse response) {
        return nonNull(response) && nonNull(response.getStatus())
                && (SC_OK == response.getStatus() || SC_ACCEPTED == response.getStatus());
    }

    private boolean isRetryableErrorResponse(final NotifyDrivingConvictionResponse response) {
        return isNull(response) || isNull(response.getStatus())
                || SC_UNAUTHORIZED == response.getStatus()
                || SC_INTERNAL_SERVER_ERROR == response.getStatus();
    }

    private void scheduleNextRetryOrThrowExceptionIfMaxRetryReached(final JsonEnvelope envelope,
                                                                    final NotifyDrivingConvictionResponse response,
                                                                    final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getRetrySequence())
                && driverNotified.getRetrySequence() < Integer.valueOf(dvlaApimConfig.getDrivingConvictionMaxRetry())) {
            scheduleNextRetryForDriverNotified(envelope, driverNotified);
        } else {
            throwNotifyDrivingConvictionException(response, driverNotified);
        }
    }

    private void scheduleNextRetryForDriverNotified(final JsonEnvelope envelope, final DriverNotified driverNotified) {
        final JsonObject payload = createObjectBuilder()
                .add("convictionId", driverNotified.getIdentifier().toString())
                .add("masterDefendantId", driverNotified.getMasterDefendantId().toString())
                .build();

        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("stagingdvla.command.handler.schedule-next-retry-for-driver-notified")
                , payload));
    }

    private void throwNotifyDrivingConvictionException(final NotifyDrivingConvictionResponse response, final DriverNotified driverNotified) {
        throw new NotifyDrivingConvictionException(("DriverNotified to DVLA notify-driving-conviction API call failed."
                .concat(" response.status: ").concat(nonNull(response) ? Integer.toString(response.getStatus()) : EMPTY)
                .concat(" identifier: ").concat(nonNull(driverNotified.getIdentifier()) ? driverNotified.getIdentifier().toString() : EMPTY)
                .concat(" masterDefendantId: ").concat(nonNull(driverNotified.getMasterDefendantId()) ? driverNotified.getMasterDefendantId().toString() : EMPTY)));
    }

    @Handles("stagingdvla.event.email-notification-sent")
    public void handleSentEmailNotificationEvent(final JsonEnvelope envelope) {

        final JsonObject requestJson = envelope.payloadAsJsonObject();
        final EmailNotificationSent emailNotification = jsonObjectToObjectConverter.convert(requestJson, EmailNotificationSent.class);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("D20 email notification sent event - {}", envelope.toObfuscatedDebugString());
        }
        final EmailChannel emailChannel = emailNotification.getDetails().getEmailNotifications().get(0);
        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
        // only a blob-addressed material has its delivery tracked, so only its email needs the outcome
        // matched back. Branch on what this material actually carries rather than on the dvlaFileStore
        // toggle: a document rendered just before a toggle flip still has its delivery tracked
        if (nonNull(emailNotification.getDetails().getDestinationFileUri())) {
            notifyObjectBuilder.add(CLIENT_CONTEXT, CLIENT_CONTEXT_PREFIX + emailNotification.getDetails().getMaterialId());
        }
        notifyObjectBuilder.add(FIELD_NOTIFICATION_ID, randomUUID().toString());
        notifyObjectBuilder.add(FIELD_TEMPLATE_ID, emailChannel.getTemplateId().toString());
        notifyObjectBuilder.add(SEND_TO_ADDRESS, emailChannel.getSendToAddress());
        notifyObjectBuilder.add(MATERIAL_URL, emailChannel.getMaterialUrl());
        notifyObjectBuilder.add(PERSONALISATION, createObjectBuilder()
                .add(SUBJECT, getPersonalisationValue(emailChannel.getPersonalisation()))
                .build());
        this.notificationNotifyService.sendEmailNotification(envelope, notifyObjectBuilder.build());
    }

    private String getPersonalisationValue(final Personalisation personalisation) {
        return personalisation.getAdditionalProperties().get(SUBJECT).toString();
    }

}
