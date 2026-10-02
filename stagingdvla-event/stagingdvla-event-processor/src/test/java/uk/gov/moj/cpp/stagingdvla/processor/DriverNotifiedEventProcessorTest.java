package uk.gov.moj.cpp.stagingdvla.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService.DVLA_DOCUMENT_ORDER;
import static uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService.DVLA_DOCUMENT_TEMPLATE_NAME;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.CLIENT_CONTEXT;
import static uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService.CLIENT_CONTEXT_PREFIX;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.stagingdvla.exception.NotifyDrivingConvictionException;
import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConvictionResponse;
import uk.gov.moj.cpp.stagingdvla.service.ApplicationParameters;
import uk.gov.moj.cpp.stagingdvla.service.ConversionFormat;
import uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService;
import uk.gov.moj.cpp.stagingdvla.service.NotificationNotifyService;
import uk.gov.moj.cpp.stagingdvla.service.NotifyDrivingConvictionService;
import uk.gov.moj.cpp.stagingdvla.service.scheduler.NotifyDrivingConvictionRetryScheduler;

import java.io.IOException;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverNotifiedEventProcessorTest {

    private final String UPDATED_DRIVER_NOTIFIED_JSON = "stagingdvla.event.driver-notified-updated-endorsement.json";
    private final String DRIVER_NOTIFIED_NO_OFFENCE_JSON = "stagingdvla.event.driver-notified-no-offence.json";
    private final String DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON = "stagingdvla.event.driver-notified-new-endorsement.json";
    private final String DRIVER_NOTIFIED_UPDATE_ENDORSEMENT_JSON = "stagingdvla.event.email-notification-sent.json";
    private final String STAGINGDVLA_EVENT_DRIVER_NOTIFIED = "stagingdvla.event.driver-notified";
    private final String DRIVER_NOTIFIED_SJP_JSON = "stagingdvla.event.driver-notified-sjp.json";
    private final String EMAIL_NOTIFICATION_SENT_JSON = "stagingdvla.event.email-notification-sent-details.json";
    private final String EMAIL_NOTIFICATION_SENT_BLOB_JSON = "stagingdvla.event.email-notification-sent-blob-details.json";
    private final String STAGINGDVLA_EVENT_EMAIL_NOTIFICATION_SENT = "stagingdvla.event.email-notification-sent";

    @InjectMocks
    private DriverNotifiedEventProcessor driverNotifiedEventProcessor;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private NotifyDrivingConvictionService notifyDrivingConvictionService;

    @Mock
    private NotifyDrivingConvictionResponse notifyDrivingConvictionResponse;

    @Mock
    private NotifyDrivingConvictionRetryScheduler scheduler;

    @Mock
    private DriverNotified driverNotified;

    @Mock
    DvlaApimConfig dvlaApimConfig;

    @Mock
    Sender sender;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    private final String identifier = randomUUID().toString();
    private final String masterDefendantId = randomUUID().toString();
    private final String materialId = randomUUID().toString();
    private final String templateId = randomUUID().toString();
    private final String caseId = randomUUID().toString();

    @Test
    public void shouldProcessDriverNotifiedMessage() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        driverNotifiedEventProcessor.handleDriverNotifiedEvent(getRequestPayload(UPDATED_DRIVER_NOTIFIED_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));

        verify(documentGeneratorService, times(1)).generateDvlaDocument(any(), any(), any());
        verify(notifyDrivingConvictionService, times(0)).notifyDrivingConviction(any());
    }

    @Test
    public void shouldProcessDriverNotifiedMessageNoOffence() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        driverNotifiedEventProcessor.handleDriverNotifiedEvent(getRequestPayload(DRIVER_NOTIFIED_NO_OFFENCE_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));

        verify(documentGeneratorService, times(1)).generateDvlaDocument(any(), any(), any());
        verify(notifyDrivingConvictionService, times(0)).notifyDrivingConviction(any());
    }

    @Test
    public void shouldCallDvlaNotifyApi_WhenNewEndorsement() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_OK);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(documentGeneratorService, times(1)).generateDvlaDocument(any(), any(), any());
    }

    @Test
    public void shouldScheduleRetry_WhenNewEndorsementReturned401Error() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(dvlaApimConfig.getDrivingConvictionMaxRetry()).thenReturn("10");
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_UNAUTHORIZED);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(scheduler, times(1)).dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);
        verify(documentGeneratorService, times(1)).generateDvlaDocument(any(), any(), any());
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("stagingdvla.command.handler.schedule-next-retry-for-driver-notified"));
    }

    @Test
    public void shouldScheduleRetry_WhenNewEndorsementReturned500Error() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(dvlaApimConfig.getDrivingConvictionMaxRetry()).thenReturn("10");
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(scheduler, times(1)).dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);
        verify(documentGeneratorService, times(1)).generateDvlaDocument(any(), any(), any());
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("stagingdvla.command.handler.schedule-next-retry-for-driver-notified"));
    }

    @Test
    public void shouldNotCreateTheDocumentDuringRetry() throws IOException {
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(dvlaApimConfig.getDrivingConvictionMaxRetry()).thenReturn("10");
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 1));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(scheduler, times(1)).dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);
        verify(documentGeneratorService, never()).generateDvlaDocument(any(), any(), any());
        verify(sender).sendAsAdmin(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("stagingdvla.command.handler.schedule-next-retry-for-driver-notified"));
    }

    @SuppressWarnings("java:S5778")
    @Test
    public void shouldNotScheduleRetryAndThrowException_WhenNewEndorsementReturned400Error() throws IOException {
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_BAD_REQUEST);

        final NotifyDrivingConvictionException notifyDrivingConvictionException = assertThrows(NotifyDrivingConvictionException.class, () -> {
            driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                    getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));
        });
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(Integer.toString(SC_BAD_REQUEST)), is(true));
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(masterDefendantId), is(true));
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(identifier), is(true));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(scheduler, times(0)).dvlaResponseReceived(any());
        verify(documentGeneratorService, times(0)).generateDvlaDocument(any(), any(), any());
        verify(sender, times(0)).sendAsAdmin(envelopeArgumentCaptor.capture());
    }

    @SuppressWarnings("java:S5778")
    @Test
    public void shouldThrowException_WhenNewEndorsementReturned500AndReachedMaxRetry() throws IOException {
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(dvlaApimConfig.getDrivingConvictionMaxRetry()).thenReturn("10");
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);

        final NotifyDrivingConvictionException notifyDrivingConvictionException = assertThrows(NotifyDrivingConvictionException.class, () -> {
            driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                    getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 10));
        });
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(Integer.toString(SC_INTERNAL_SERVER_ERROR)), is(true));
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(masterDefendantId), is(true));
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(identifier), is(true));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(scheduler, times(1)).dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);
        verify(documentGeneratorService, times(0)).generateDvlaDocument(any(), any(), any());
        verify(sender, times(0)).sendAsAdmin(any());
    }

    @Test
    public void shouldNotCallDvlaNotifyApiApi_WhenUpdateEndorsement() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                getRequestPayload(DRIVER_NOTIFIED_UPDATE_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0));

        verify(notifyDrivingConvictionService, times(0)).notifyDrivingConviction(any());
        verify(documentGeneratorService, times(1)).generateDvlaDocument(any(), any(), any());
    }

    @Test
    public void shouldPassTheEventUserIdToDvlaDocumentGenerationWhenDvlaFileStoreDisabled() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(false);
        final UUID userId = randomUUID();
        final JsonEnvelope envelope = getRequestPayload(UPDATED_DRIVER_NOTIFIED_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0, userId);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(envelope);

        final ArgumentCaptor<DriverNotified> driverNotifiedCaptor = ArgumentCaptor.forClass(DriverNotified.class);
        verify(documentGeneratorService).generateDvlaDocument(eq(envelope), eq(userId), driverNotifiedCaptor.capture());
        assertThat(driverNotifiedCaptor.getValue().getMaterialId().toString(), is(materialId));
        verify(documentGeneratorService, never()).generateDocument(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void shouldGenerateDocumentViaBlobStorageWhenDvlaFileStoreEnabled() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(true);
        final JsonObject nowsDocumentOrderJson = Json.createObjectBuilder().add("materialId", materialId).build();
        final String fileName = DVLA_DOCUMENT_ORDER + "_" + materialId + ".pdf";
        when(objectToJsonObjectConverter.convert(any(DriverNotified.class))).thenReturn(nowsDocumentOrderJson);
        when(documentGeneratorService.getMaterialIdAmendedFileName(DVLA_DOCUMENT_ORDER, materialId)).thenReturn(fileName);
        final JsonEnvelope envelope = getRequestPayload(UPDATED_DRIVER_NOTIFIED_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(envelope);

        verify(documentGeneratorService).generateDocument(eq(envelope), eq(UUID.fromString(materialId)), eq(nowsDocumentOrderJson),
                eq(fileName), eq(DVLA_DOCUMENT_TEMPLATE_NAME), eq(ConversionFormat.PDF), eq(DVLA_DOCUMENT_ORDER), isNull());
        verify(documentGeneratorService, never()).generateDvlaDocument(any(), any(), any());
        verify(notifyDrivingConvictionService, times(0)).notifyDrivingConviction(any());
    }

    @Test
    public void shouldPassTheSjpCaseIdToBlobDocumentGenerationForSjpCase() throws IOException {
        when(featureControlGuard.isFeatureEnabled("dvlaFileStore")).thenReturn(true);
        lenient().when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        lenient().when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_OK);
        final JsonEnvelope envelope = getRequestPayload(DRIVER_NOTIFIED_SJP_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 0);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(envelope);

        verify(documentGeneratorService).generateDocument(eq(envelope), eq(UUID.fromString(materialId)), any(),
                any(), eq(DVLA_DOCUMENT_TEMPLATE_NAME), eq(ConversionFormat.PDF), eq(DVLA_DOCUMENT_ORDER), eq(UUID.fromString(caseId)));
    }

    @Test
    public void shouldNotCreateTheDocumentDuringRetryWhenDvlaFileStoreEnabled() throws IOException {
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(notifyDrivingConvictionResponse);
        when(dvlaApimConfig.getDrivingConvictionMaxRetry()).thenReturn("10");
        when(notifyDrivingConvictionResponse.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);

        driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 1));

        verify(featureControlGuard, never()).isFeatureEnabled(any());
        verify(documentGeneratorService, never()).generateDocument(any(), any(), any(), any(), any(), any(), any());
        verify(documentGeneratorService, never()).generateDvlaDocument(any(), any(), any());
    }

    @Test
    public void shouldThrowExceptionWhenUserIdIsMissingFromTheEvent() throws IOException {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUID(STAGINGDVLA_EVENT_DRIVER_NOTIFIED),
                buildDriverNotifiedJsonObject(UPDATED_DRIVER_NOTIFIED_JSON, 0));

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> driverNotifiedEventProcessor.handleDriverNotifiedEvent(envelope));

        assertThat(exception.getMessage(), is("UserId missing from event."));
        verify(documentGeneratorService, never()).generateDvlaDocument(any(), any(), any());
        verify(documentGeneratorService, never()).generateDocument(any(), any(), any(), any(), any(), any(), any());
    }

    @SuppressWarnings("java:S5778")
    @Test
    public void shouldThrowExceptionWithEmptyStatusPlaceholder_WhenNotifyDrivingConvictionResponseIsNull() throws IOException {
        // notifyDrivingConviction returning null (as opposed to a response with a null/error
        // status) drives isRetryableErrorResponse's isNull(response) branch, and then - once max
        // retry is reached - the nonNull(response) ? ... : EMPTY fallback in the exception
        // message, neither of which the other retry/exception tests (which always stub a
        // non-null response) exercise.
        when(notifyDrivingConvictionService.notifyDrivingConviction(isA(DriverNotified.class))).thenReturn(null);
        when(dvlaApimConfig.getDrivingConvictionMaxRetry()).thenReturn("10");

        final NotifyDrivingConvictionException notifyDrivingConvictionException = assertThrows(NotifyDrivingConvictionException.class, () -> {
            driverNotifiedEventProcessor.handleDriverNotifiedEvent(
                    getRequestPayload(DRIVER_NOTIFIED_NEW_ENDORSEMENT_JSON, STAGINGDVLA_EVENT_DRIVER_NOTIFIED, 10));
        });

        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains("response.status: "), is(true));
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(masterDefendantId), is(true));
        assertThat(notifyDrivingConvictionException.getLocalizedMessage().contains(identifier), is(true));

        verify(notifyDrivingConvictionService, times(1)).notifyDrivingConviction(any(DriverNotified.class));
        verify(scheduler, times(1)).dvlaResponseReceived(NotifyDrivingConvictionRetryScheduler.DvlaResponseType.FAIL);
        verify(documentGeneratorService, times(0)).generateDvlaDocument(any(), any(), any());
        verify(sender, times(0)).sendAsAdmin(any());
    }

    @Test
    public void shouldSendEmailNotificationWithClientContextForBlobAddressedMaterial() throws IOException {
        final JsonEnvelope envelope = getRequestPayload(EMAIL_NOTIFICATION_SENT_BLOB_JSON, STAGINGDVLA_EVENT_EMAIL_NOTIFICATION_SENT, 0);

        driverNotifiedEventProcessor.handleSentEmailNotificationEvent(envelope);

        final JsonObject notifyPayload = captureEmailNotificationPayload(envelope);
        assertThat(notifyPayload.getString(CLIENT_CONTEXT), is(CLIENT_CONTEXT_PREFIX + materialId));
        assertEmailNotificationFields(notifyPayload);
        // decided by the material itself, not by the dvlaFileStore toggle
        verifyNoInteractions(featureControlGuard);
    }

    @Test
    public void shouldSendEmailNotificationWithoutClientContextForFileServiceMaterial() throws IOException {
        final JsonEnvelope envelope = getRequestPayload(EMAIL_NOTIFICATION_SENT_JSON, STAGINGDVLA_EVENT_EMAIL_NOTIFICATION_SENT, 0);

        driverNotifiedEventProcessor.handleSentEmailNotificationEvent(envelope);

        final JsonObject notifyPayload = captureEmailNotificationPayload(envelope);
        assertThat(notifyPayload.containsKey(CLIENT_CONTEXT), is(false));
        assertEmailNotificationFields(notifyPayload);
        verifyNoInteractions(featureControlGuard);
    }

    private JsonObject captureEmailNotificationPayload(final JsonEnvelope envelope) {
        final ArgumentCaptor<JsonObject> notifyPayloadCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(notificationNotifyService).sendEmailNotification(eq(envelope), notifyPayloadCaptor.capture());
        return notifyPayloadCaptor.getValue();
    }

    private void assertEmailNotificationFields(final JsonObject notifyPayload) {
        assertThat(UUID.fromString(notifyPayload.getString("notificationId")).toString(), is(notifyPayload.getString("notificationId")));
        assertThat(notifyPayload.getString("templateId"), is(templateId));
        assertThat(notifyPayload.getString("sendToAddress"), is("dvla-test@example.com"));
        assertThat(notifyPayload.getString("materialUrl"), is("http://localhost/material/" + materialId));
        assertThat(notifyPayload.getJsonObject("personalisation").getString("subject"), is("DVLA Driver Notification - New Endorsement - TEST"));
    }

    private JsonEnvelope getRequestPayload(final String fileName, final String eventName, final int retrySequence, final UUID userId) throws IOException {
        return JsonEnvelope.envelopeFrom(MetadataBuilderFactory
                        .metadataWithRandomUUID(eventName)
                        .withUserId(userId.toString()),
                buildDriverNotifiedJsonObject(fileName, retrySequence));
    }

    private JsonEnvelope getRequestPayload(final String fileName, final String eventName, final int retrySequence) throws IOException {
        return JsonEnvelope.envelopeFrom(MetadataBuilderFactory
                        .metadataWithRandomUUID(eventName)
                        .withUserId(randomUUID().toString()),
                buildDriverNotifiedJsonObject(fileName, retrySequence));
    }

    private JsonObject buildDriverNotifiedJsonObject(final String resourcename, final int retrySequence) throws IOException {
        String inputPayload = Resources.toString(getResource(resourcename), defaultCharset());
        inputPayload = inputPayload.replace("MASTER_DEFENDANT_ID", masterDefendantId)
                .replace("MATERIAL_ID", materialId)
                .replace("IDENTIFIER", identifier)
                .replace("RETRY_SEQUENCE", Integer.toString(retrySequence))
                .replace("TEMPLATE_ID", templateId)
                .replace("CASE_ID", caseId);
        return stringToJsonObjectConverter.convert(inputPayload);
    }
}
