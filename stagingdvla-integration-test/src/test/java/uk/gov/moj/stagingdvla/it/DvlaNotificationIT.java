package uk.gov.moj.stagingdvla.it;

import static com.google.common.collect.ImmutableMap.of;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;
import static uk.gov.moj.stagingdvla.stubs.DVLANotificationStub.verifyDVLANotificationCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.awaitGenerateDocumentRequest;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.generateDocumentRequestCount;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.latestGenerateDocumentRequest;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.latestGenerateDocumentRequests;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.publishDocumentAvailableEvent;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.publishDocumentAvailableEventForAzureBlob;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubGenerateDocument;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.verifyGenerateDocumentStubCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.ORIGINATOR;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.awaitMaterialUploadRequest;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.publishFailedToAddMaterialEvent;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.publishMaterialAddedEvent;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.stagingdvla.stubs.NotifyGatewayStub.bodyOf;
import static uk.gov.moj.stagingdvla.stubs.NotifyGatewayStub.publishNotificationFailed;
import static uk.gov.moj.stagingdvla.stubs.NotifyGatewayStub.publishNotificationSent;
import static uk.gov.moj.stagingdvla.stubs.NotifyGatewayStub.verifyEmailNotificationSentToGateway;
import static uk.gov.moj.stagingdvla.stubs.NotifyStub.awaitLegacyEmailNotificationSent;
import static uk.gov.moj.stagingdvla.stubs.NotifyStub.publishNotificationSentEvent;
import static uk.gov.moj.stagingdvla.stubs.NotifyStub.verifyEmailNotificationSent;
import static uk.gov.moj.stagingdvla.stubs.NotifyStub.verifyNoEmailNotificationSent;
import static uk.gov.moj.stagingdvla.stubs.ProgressionStub.stubProgressionAddCourtDocument;
import static uk.gov.moj.stagingdvla.stubs.ProgressionStub.verifyProgressionAddCourtDocumentInvoked;
import static uk.gov.moj.stagingdvla.stubs.ProgressionStub.verifyProgressionAddCourtDocumentInvokedAsUser;
import static uk.gov.moj.stagingdvla.stubs.SjpStub.publishCaseDocumentAddedEvent;
import static uk.gov.moj.stagingdvla.stubs.SjpStub.stubSjpUploadCaseDocument;
import static uk.gov.moj.stagingdvla.util.QueueUtil.EventListener;
import static uk.gov.moj.stagingdvla.util.QueueUtil.listenFor;
import static uk.gov.moj.stagingdvla.util.QueueUtil.privateEvents;
import static uk.gov.moj.stagingdvla.util.QueueUtil.retrieveMessage;
import static uk.gov.moj.stagingdvla.util.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.stagingdvla.util.RestHelper.pollForResponse;
import static uk.gov.moj.stagingdvla.util.RestHelper.postCommandWithUserId;
import static uk.gov.moj.stagingdvla.util.StubUtil.stubUser;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.stagingdvla.util.FileUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DvlaNotificationIT extends AbstractIntegrationTest {

    public static final String USER_GROUP = UUID.randomUUID().toString();

    private String hearingId;
    private String defendantId;
    private String caseId;
    private String caseId2;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private final MessageConsumer consumerForDriverNotified = privateEvents.createPrivateConsumer("stagingdvla.event.driver-notified");
    private final MessageConsumer consumerForDvlaDocumentDeliveryRecorded = privateEvents.createPrivateConsumer("stagingdvla.event.dvla-document-delivery-recorded");
    private final MessageConsumer consumerForDocumentDeletedFromBlob = privateEvents.createPrivateConsumer("stagingdvla.event.document-deleted-from-blob");

    private final String DRIVER_NOTIFICATION_MEDIA_TYPE = "application/vnd.stagingdvla.command.driver-notification+json";
    private final String DVLA_DOCUMENT_DELIVERY_MEDIA_TYPE = "application/vnd.stagingdvla.query.dvla-document-delivery+json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD = "stagingdvla.command.driver-notification.json";
    private final String DRIVER_D20REMOVAL_NOTIFICATION_COMMAND_PAYLOAD = "stagingdvla.command.driver-d20removal-notification.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT = "stagingdvla.command.driver-notification-with-convicting-court.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES = "stagingdvla.command.driver-notification-with-convicting-court-multiple-offences.json";
    private final String DRIVER_NO_NOTIFICATION_COMMAND_PAYLOAD = "stagingdvla.command.driver-no-notification.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_NO_ENDORSEMENT = "stagingdvla.command.driver-notification-with-no-endorsement.json";
    private final String DRIVER_NOTIFICATION_NEW_ENDORSEMENT_COMMAND_PAYLOAD = "stagingdvla.driver-notified-new-endorsement.dvla-api.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_1 = "stagingdvla.command.driver-notification-linkedcase-1.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_2 = "stagingdvla.command.driver-notification-linkedcase-2.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_3 = "stagingdvla.command.driver-notification-linkedcase-3.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_GENERATE_D20 = "stagingdvla.command.driver-notification-sjp-generate-d20.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_APPLICATION_GRANTED = "stagingdvla.command.driver-notification-sjp-application-granted.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_CASE = "stagingdvla.command.driver-notification-sjp-case.json";
    private final String DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_CASE_MULTIPLE_CONVICTING_COURTS = "stagingdvla.command.driver-notification-sjp-case-multiple-convicting-courts.json";

    private static final String STAGINGDVLA_CONTEXT = "stagingdvla";
    private static final String CLIENT_CONTEXT_PREFIX = "STAGINGDVLA_"; // NotificationNotifyService.CLIENT_CONTEXT_PREFIX

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP), "stub-data/usersgroups.get-specific-groups-by-user.json");
        stubDocumentCreate("Dummy");
        stubGenerateDocument("Dummy");
        stubProgressionAddCourtDocument();
        stubSjpUploadCaseDocument();
        final ImmutableMap<String, Boolean> features = of("driverOut", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);
    }

    @Override
    @BeforeEach
    public void setup() {
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", false);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);
        hearingId = randomUUID().toString();
        defendantId = randomUUID().toString();
        caseId = randomUUID().toString();
        caseId2 = randomUUID().toString();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldSendDvlaNotification() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        assertThat(driverNotified, is(notNullValue()));

        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

        // verify the actual generate-document request body, not just that it was called -
        // values mirror what DocumentGeneratorService/SystemDocGeneratorService build for a
        // driver-notification document order
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        assertThat(generateDocumentRequest.getString("originatingSource"), equalTo("DVLADocumentOrder"));
        assertThat(generateDocumentRequest.getString("templateIdentifier"), equalTo("EDT_DriverOutNotification"));
        assertThat(generateDocumentRequest.getString("conversionFormat"), equalTo("pdf"));
        assertThat(generateDocumentRequest.getString("sourceCorrelationId"), is(notNullValue()));
        assertThat(generateDocumentRequest.getString("payloadFileServiceId"), is(notNullValue()));
    }

    @Test
    public void shouldSendDvlaNotificationWithAzureBlob() throws IOException {
        // dvlaFileStore=true -> DocumentGeneratorService uses the Azure blob upload path
        // instead of the real file-service, so the generate-document request carries
        // payloadFileUri/destinationFileUri rather than payloadFileServiceId
        // dvlaFileStoreDelete=true -> DocumentDeletedFromBlobEventProcessor deletes both blobs once delivery completes
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        assertThat(driverNotified, is(notNullValue()));

        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

        // verify the actual generate-document request body reflects the Azure blob path
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        assertThat(generateDocumentRequest.getString("originatingSource"), equalTo("DVLADocumentOrder"));
        assertThat(generateDocumentRequest.getString("templateIdentifier"), equalTo("EDT_DriverOutNotification"));
        assertThat(generateDocumentRequest.getString("conversionFormat"), equalTo("pdf"));
        // the user who raised the driver notification, as on the file-service path
        assertThat(generateDocumentRequest.getString("sourceCorrelationId"), equalTo(USER_GROUP));
        assertThat(generateDocumentRequest.getString("payloadFileServiceId"), is(nullValue()));
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        assertThat(payloadFileUri, is(notNullValue()));
        final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");
        // the blob SDK percent-encodes the folder separators (sdg%2Frequests%2F...), so compare decoded
        assertThat(decode(payloadFileUri, UTF_8).contains("/sdg/requests/"), is(true));
        assertThat(decode(destinationFileUri, UTF_8), equalTo(decode(payloadFileUri, UTF_8).replace("/sdg/requests/", "/sdg/documents/") + ".pdf"));

        // stagingdvla.command.handler.driver-notification-document-delivery is invoked internally
        // (once per document generation/upload step of the flow) - verify the flow's final
        // stagingdvla.event.dvla-document-delivery-recorded event
        final DvlaDocumentDeliveryRecorded finalDocumentDeliveryRecorded = retrieveFinalDvlaDocumentDeliveryRecordedEvent();
        assertThat(finalDocumentDeliveryRecorded, is(notNullValue()));
        assertThat(finalDocumentDeliveryRecorded.getMaterialId(), is(equalTo(driverNotified.getMaterialId())));

        // verify the read side (dvla-document-delivery query API) reflects what the event stream just recorded -
        // this is a non-SJP notification, so the delivery should carry no sjp case fields
        final String queryUserId = randomUUID().toString();
        stubUser(queryUserId);
        final String materialId = driverNotified.getMaterialId().toString();
        pollForResponse("/dvla-document-deliveries?materialId=" + materialId + "&materialStatus=PENDING",
                DVLA_DOCUMENT_DELIVERY_MEDIA_TYPE, queryUserId,
                allOf(
                        withJsonPath("$.documentDeliveries[0].materialId", equalTo(materialId)),
                        withJsonPath("$.documentDeliveries[0].materialStatus"),
                        withJsonPath("$.documentDeliveries[0].payloadBlobUri", equalTo(payloadFileUri)),
                        withJsonPath("$.documentDeliveries[0].documentBlobUri", equalTo(destinationFileUri)),
                        hasNoJsonPath("$.documentDeliveries[0].caseId")
                ));

        //When: the real systemdocgenerator isn't deployed here (only its command-api is stubbed
        // above), so simulate its response - for the Azure blob path the document-available
        // contract's other oneOf branch applies (payloadFileUri/destinationFileUri, no
        // payloadFileServiceId/documentFileServiceId - see the schema's document-available.json),
        // echoing back the same URIs DocumentGeneratorService uploaded the payload to/asked the
        // rendered document to be written to
        publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri,
                generateDocumentRequest.getString("sourceCorrelationId"));

        //Then: SystemDocGeneratorEventProcessor.handleDvlaDocumentAvailable delivers the generated
        // document onward as a court document for this (non-SJP) case, as the user who raised the
        // driver notification - progression authorises it against that user's groups
        verifyProgressionAddCourtDocumentInvokedAsUser(USER_GROUP);

        //Then: NowsMaterialStatusEventProcessor asks the material context to store the rendered
        // document via material.command.upload-file - on the Azure blob path it references the
        // document by fileUri (the destination blob), never by fileServiceId, since the command's
        // payload is an exclusive oneOf over the two
        final JsonPath materialUploadRequest = awaitMaterialUploadRequest(materialId);
        assertThat(materialUploadRequest.getString("fileUri"), equalTo(destinationFileUri));
        assertThat(materialUploadRequest.getString("fileServiceId"), is(nullValue()));
        // the originator travels in _metadata and is echoed back by the material context on
        // material.material-added - MaterialAddedProcessor only reacts to "d20" for this flow
        assertThat(materialUploadRequest.getString("_metadata.originator"), equalTo(ORIGINATOR));

        // the real material context isn't deployed here either, so simulate its own eventual,
        // asynchronous confirmation that the material was stored (material.material-added) -
        // addDocumentToMaterial above already asked it to via
        // stagingdvla.command.record-nows-material-request, with fileId=null and
        // payloadFileUri/destinationFileUri instead (the Azure-blob branch of
        // materialDetails.json's own oneOf(fileId | payloadFileUri+destinationFileUri) contract)
        publishMaterialAddedEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));

        //Then: MaterialAddedProcessor.recordDocumentDeliveryStatus records the flow's final SUCCESS status -
        // a NEW endorsement from a single court/conviction date goes to DVLA by API, so no D20 email is needed
        final DvlaDocumentDeliveryRecorded successDocumentDeliveryRecorded = retrieveFinalDvlaDocumentDeliveryRecordedEvent();
        assertThat(successDocumentDeliveryRecorded, is(notNullValue()));
        assertThat(successDocumentDeliveryRecorded.getMaterialId(), is(equalTo(driverNotified.getMaterialId())));
        assertThat(successDocumentDeliveryRecorded.getEmailStatus(), is("NOT_REQUIRED"));

        // verify the read side reflects the completed delivery
        pollForResponse("/dvla-document-deliveries?materialId=" + materialId + "&materialStatus=SUCCESS",
                DVLA_DOCUMENT_DELIVERY_MEDIA_TYPE, queryUserId,
                allOf(
                        withJsonPath("$.documentDeliveries[0].materialId", equalTo(materialId)),
                        withJsonPath("$.documentDeliveries[0].materialStatus", equalTo("SUCCESS")),
                        withJsonPath("$.documentDeliveries[0].emailStatus", equalTo("NOT_REQUIRED")),
                        withJsonPath("$.documentDeliveries[0].payloadBlobUri", equalTo(payloadFileUri)),
                        withJsonPath("$.documentDeliveries[0].documentBlobUri", equalTo(destinationFileUri))
                ));

        //Then: material SUCCESS with no email needed completes a non-SJP delivery, so MaterialAggregate
        // raises document-deleted-from-blob for the payload and rendered document blobs
        verifyDocumentDeletedFromBlob(materialId, payloadFileUri, destinationFileUri);
        verifyNoEmailNotificationSent(CLIENT_CONTEXT_PREFIX + materialId);
    }

    @Test
    public void shouldRecordMaterialFailedAndKeepBlobsWhenMaterialFailsToAddWithAzureBlob() throws IOException {
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        //Given: a driver notification on the Azure blob path, its delivery tracked from the blob PENDING record
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD), USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        final String materialId = driverNotified.getMaterialId().toString();

        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");
        retrieveDocumentDeliveryEvent(materialId, "materialStatus", "PENDING");

        publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri,
                generateDocumentRequest.getString("sourceCorrelationId"));
        final JsonPath materialUploadRequest = awaitMaterialUploadRequest(materialId);
        assertThat(materialUploadRequest.getString("_metadata.originator"), equalTo(ORIGINATOR));

        //When: the material context cannot store the rendered document and raises
        // public.events.material.failed-to-add-material, echoing back our originator
        publishFailedToAddMaterialEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));

        //Then: MaterialAddedProcessor records the material as FAILED
        final JsonPath materialFailedDocumentDeliveryEvent = retrieveDocumentDeliveryEvent(materialId, "materialStatus", "FAILED");
        assertThat(materialFailedDocumentDeliveryEvent.getString("materialId"), equalTo(materialId));

        final String queryUserId = randomUUID().toString();
        stubUser(queryUserId);
        pollForResponse("/dvla-document-deliveries?materialId=" + materialId + "&materialStatus=FAILED",
                DVLA_DOCUMENT_DELIVERY_MEDIA_TYPE, queryUserId,
                allOf(
                        withJsonPath("$.documentDeliveries[0].materialId", equalTo(materialId)),
                        withJsonPath("$.documentDeliveries[0].materialStatus", equalTo("FAILED")),
                        withJsonPath("$.documentDeliveries[0].payloadBlobUri", equalTo(payloadFileUri)),
                        withJsonPath("$.documentDeliveries[0].documentBlobUri", equalTo(destinationFileUri))
                ));

        //Then: a failed material does not complete the delivery - the blobs are kept and no D20 email goes out
        assertNoDocumentDeletedFromBlob(materialId);
        verifyNoEmailNotificationSent(CLIENT_CONTEXT_PREFIX + materialId);
    }

    // Deliberate pair with shouldSendDvlaEmailViaLegacyNotifyWhenFileServiceMaterial below:
    // dvlaFileStore=true routes the D20 email through the notify gateway's ASB queue (this test,
    // against the real Service Bus emulator - DVLAFS-12); dvlaFileStore=false keeps the legacy
    // notificationnotify REST send. Blob-deletion-after-email-success is DVLAFS-19, now covered
    // below by publishing the gateway's reply onto staging-dvla's own dedicated result queue.
    @Test
    public void shouldSendDvlaEmailToNotifyGatewayWhenAzureBlobMaterial() throws IOException {
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        // two convicting courts forces the D20 email instead of the DVLA API
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        assertThat(driverNotified, is(notNullValue()));
        final String materialId = driverNotified.getMaterialId().toString();
        final String clientContext = CLIENT_CONTEXT_PREFIX + materialId;

        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");
        assertThat(payloadFileUri, is(notNullValue()));

        // triggers stagingdvla.event.email-notification-sent
        publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri,
                generateDocumentRequest.getString("sourceCorrelationId"));
        verifyProgressionAddCourtDocumentInvoked();
        publishMaterialAddedEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));

        final ServiceBusReceivedMessage message = verifyEmailNotificationSentToGateway(clientContext);

        // wire contract: command-send-email-notification.schema.json
        final JsonObject messageBody = bodyOf(message);
        assertThat(messageBody.getString("fileUri"), equalTo(destinationFileUri));
        assertThat(messageBody.getString("clientContext"), equalTo(clientContext));
        assertThat(messageBody.containsKey("materialUrl"), is(false));
        assertThat(messageBody.getString("templateId"), is(notNullValue()));
        assertThat(messageBody.getString("sendToAddress"), is(notNullValue()));
        assertThat(messageBody.getJsonObject("personalisation").getString("subject"), is(notNullValue()));
        assertThat(message.getSubject(), equalTo("send-email-notification"));
        assertThat(message.getReplyTo(), equalTo("stagingdvla-notification-result"));

        // DVLAFS-19: simulate the gateway's reply landing on staging-dvla's own dedicated result
        // queue; NotifyGatewayResultConsumer (the real consumer under test, already running) picks
        // it up and records the email leg SUCCESS
        publishNotificationSent(clientContext, randomUUID());
        final JsonPath emailSuccessEvent = retrieveEmailStatusDocumentDeliveryEvent(materialId, "SUCCESS");
        assertThat(emailSuccessEvent.getString("materialId"), equalTo(materialId));
    }

    // DVLAFS-19 AC-5: the gateway can legitimately republish the same notificationId's result (a
    // task transaction rollback-then-retry, per the gateway's own engineering notes - not
    // hypothetical). The deletion gate must not double-fire on the duplicate.
    @Test
    public void shouldNotDoubleApplyDuplicateNotifyGatewayResult() throws IOException {
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        final String materialId = driverNotified.getMaterialId().toString();
        final String clientContext = CLIENT_CONTEXT_PREFIX + materialId;

        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");

        publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri,
                generateDocumentRequest.getString("sourceCorrelationId"));
        verifyProgressionAddCourtDocumentInvoked();
        publishMaterialAddedEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));
        verifyEmailNotificationSentToGateway(clientContext);

        final UUID notificationId = randomUUID();

        //When: the same notificationId's notification-sent is published twice
        publishNotificationSent(clientContext, notificationId);
        verifyDocumentDeletedFromBlob(materialId, payloadFileUri, destinationFileUri);
        publishNotificationSent(clientContext, notificationId);

        //Then: no second document-deleted-from-blob for this materialId - one delivery, one deletion
        assertThat(retrieveMessage(consumerForDocumentDeletedFromBlob,
                        isJson(withJsonPath("$.materialId", equalTo(materialId)))),
                is(nullValue()));
    }

    // DVLAFS-19 AC-3, failed half: a failed delivery marks the email leg FAILED and retains the
    // blobs (the "alertable" half of AC-3 has no alerting mechanism anywhere in this CR or the
    // gateway CR yet - not asserted here, tracked as a known gap).
    @Test
    public void shouldRecordEmailFailedAndRetainBlobsWhenNotifyGatewayReportsFailure() throws IOException {
        final ImmutableMap<String, Boolean> features = of("dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        final String materialId = driverNotified.getMaterialId().toString();
        final String clientContext = CLIENT_CONTEXT_PREFIX + materialId;

        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");

        publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri,
                generateDocumentRequest.getString("sourceCorrelationId"));
        verifyProgressionAddCourtDocumentInvoked();
        publishMaterialAddedEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));
        verifyEmailNotificationSentToGateway(clientContext);

        //When
        publishNotificationFailed(clientContext, randomUUID(), "Gov.Notify rejected the request");

        //Then
        final JsonPath emailFailedEvent = retrieveEmailStatusDocumentDeliveryEvent(materialId, "FAILED");
        assertThat(emailFailedEvent.getString("materialId"), equalTo(materialId));
        assertNoDocumentDeletedFromBlob(materialId);
    }

    // dvlaFileStore=false counterpart (the class default - not re-stubbed here): legacy file-service
    // material, email via the legacy notificationnotify REST call. No delivery tracking or reply
    // correlation on this path - see the comment further down - so none is asserted here.
    @Test
    public void shouldSendDvlaEmailViaLegacyNotifyWhenFileServiceMaterial() throws IOException {
        // two convicting courts forces the D20 email instead of the DVLA API
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        assertThat(driverNotified, is(notNullValue()));
        final String materialId = driverNotified.getMaterialId().toString();

        verifyGenerateDocumentStubCommandInvoked();
        final JsonPath generateDocumentRequest = latestGenerateDocumentRequest();
        final String payloadFileServiceId = generateDocumentRequest.getString("payloadFileServiceId");
        assertThat(payloadFileServiceId, is(notNullValue()));
        assertThat(generateDocumentRequest.getString("payloadFileUri"), is(nullValue()));
        assertThat(generateDocumentRequest.getString("destinationFileUri"), is(nullValue()));

        // documentFileServiceId is the rendered document's own id, not the input payload's
        final String documentFileServiceId = publishDocumentAvailableEvent(payloadFileServiceId,
                generateDocumentRequest.getString("sourceCorrelationId"));
        verifyProgressionAddCourtDocumentInvoked();

        // must wait for the real upload request before simulating material-added - progression and
        // material-upload are independent branches, so publishing material-added too early races it
        final JsonPath materialUploadRequest = awaitMaterialUploadRequest(materialId);
        assertThat(materialUploadRequest.getString("fileServiceId"), equalTo(documentFileServiceId));
        assertThat(materialUploadRequest.getString("fileUri"), is(nullValue()));

        publishMaterialAddedEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));

        // Delivery tracking only ever initialises from a blob-addressed PENDING record
        // (MaterialAggregate.recordDocumentDelivery / isPendingWithBlob) - on this file-service path
        // that never happens, so material-added's SUCCESS record is a silent no-op, and reply
        // correlation (clientContext-based) never triggers either. Confirmed by reading production
        // code: genuine pre-existing scope, not a gap - nothing to poll or assert on that side.
        final JsonPath emailNotificationSent = awaitLegacyEmailNotificationSent();
        assertThat(emailNotificationSent.getString("templateId"), is(notNullValue()));
        assertThat(emailNotificationSent.getString("sendToAddress"), is(notNullValue()));
        assertThat(emailNotificationSent.getString("materialUrl"), is(notNullValue()));
        assertThat(emailNotificationSent.getString("personalisation.subject"), is(notNullValue()));

        assertNoDocumentDeletedFromBlob(materialId);
    }

    private DvlaDocumentDeliveryRecorded retrieveFinalDvlaDocumentDeliveryRecordedEvent() {
        DvlaDocumentDeliveryRecorded lastEvent = null;
        Optional<JsonObject> jsonObject;
        while ((jsonObject = retrieveMessageAsJsonObject(consumerForDvlaDocumentDeliveryRecorded)).isPresent()) {
            lastEvent = jsonToObjectConverter.convert(jsonObject.get(), DvlaDocumentDeliveryRecorded.class);
        }
        return lastEvent;
    }

    @Test
    public void shouldNotSendDvlaNotificationWhenNoEndorsement() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_NO_ENDORSEMENT);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        //Then
        verifyEventIsNotCreated();
    }

    @Test
    public void shouldSendDvlaD20RemovalNotification() throws IOException {
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyMaterialCreated();

        final String d20RemovalBody = getPayload(DRIVER_D20REMOVAL_NOTIFICATION_COMMAND_PAYLOAD);
        final Response d20RemovalResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, d20RemovalBody, USER_GROUP);
        assertThat(d20RemovalResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationWithConvictingCourtCode() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT)
                .replaceAll("%HEARING_ID%", hearingId);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        try (final EventListener driverNotifiedEventListener = listenFor("stagingdvla.event.driver-notified")
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].code", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].startDate", CoreMatchers.is("1996-05-04"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].endDate", CoreMatchers.is("2005-05-05"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].mainOffenceCode", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictingCourtCode", CoreMatchers.is("2577"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictionDate", CoreMatchers.is("2021-10-04"))))
        ) {
            driverNotifiedEventListener.expectNoneWithin(10000);
        }

        //Then
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationWithConvictingCourtCodeForMultipleOffences() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_WITH_CONVICTING_COURT_MULTIPLE_OFFENCES)
                .replaceAll("%HEARING_ID%", hearingId);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        try (final EventListener driverNotifiedEventListener = listenFor("stagingdvla.event.driver-notified")
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].code", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].mainOffenceCode", CoreMatchers.is("CA03014"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].startDate", CoreMatchers.is("1996-05-04"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].endDate", CoreMatchers.is("2005-05-05"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictingCourtCode", CoreMatchers.is("2577"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[0].convictionDate", CoreMatchers.is("2021-03-10"))))

                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].code", CoreMatchers.is("CA03012"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].mainOffenceCode", CoreMatchers.is("CA03012"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].startDate", CoreMatchers.is("2000-11-12"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].endDate", CoreMatchers.is("2021-11-05"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].convictingCourtCode", CoreMatchers.is("1365"))))
                .withFilter(isJson(withJsonPath("$.cases[0].defendantCaseOffences[1].convictionDate", CoreMatchers.is("2021-10-04"))))
        ) {
            driverNotifiedEventListener.expectNoneWithin(20000);
        }

        //Then
        verifyMaterialCreated();
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationForNewEndorsement() throws IOException {
        //Given
        final String body = getPayload(DRIVER_NOTIFICATION_NEW_ENDORSEMENT_COMMAND_PAYLOAD);

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        //Then
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldSendDvlaNotificationForUpdatedCase() throws IOException {
        // Case 1 resulted
        final String body1 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_1);
        final Response writeResponse1 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body1, USER_GROUP);
        assertThat(writeResponse1.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyEventIsCreated();

        // Case 2 resulted
        final String body2 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_2);
        final Response writeResponse2 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body2, USER_GROUP);
        assertThat(writeResponse2.getStatusCode(), equalTo(SC_ACCEPTED));
        verifyEventIsCreated();

        // Case 1 is updated
        final String body3 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_LINKED_CASE_3);

        final Response writeResponse3 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body3, USER_GROUP);

        assertThat(writeResponse3.getStatusCode(), equalTo(SC_ACCEPTED));

        // No removal for case 1
        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);

        assertThat(driverNotified.getCases().size(), equalTo(1));
        assertThat(driverNotified.getCases().get(0).getReference(), equalTo("CASE0000001"));
        assertThat(driverNotified.getRemovedEndorsements(), is(nullValue()));
        assertThat(driverNotified.getUpdatedEndorsements().size(), equalTo(4));

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    void shouldGenerateAndUpdateD20ForSJPCase() throws IOException {
        // Generate D20 for SJP case
        final String body1 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_GENERATE_D20);
        final Response writeResponse1 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body1, USER_GROUP);

        assertThat(writeResponse1.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified1 = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);

        assertThat(driverNotified1, is(notNullValue()));
        assertThat(driverNotified1.getCases().size(), equalTo(1));
        assertThat(driverNotified1.getCases().get(0).getReference(), equalTo("25JAN000011"));
        assertThat(driverNotified1.getRemovedEndorsements(), is(nullValue()));
        assertThat(driverNotified1.getUpdatedEndorsements(), is(nullValue()));

        // Application Granted for above SJP case
        final String body2 = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_APPLICATION_GRANTED);
        final Response writeResponse2 = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body2, USER_GROUP);
        assertThat(writeResponse2.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified2 = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);

        assertThat(driverNotified2, is(notNullValue()));
        assertThat(driverNotified2.getCases().size(), equalTo(1));
        assertThat(driverNotified2.getCases().get(0).getReference(), equalTo("25JAN000011"));
        assertThat(driverNotified2.getRemovedEndorsements(), is(nullValue()));
        assertThat(driverNotified2.getUpdatedEndorsements().size(), equalTo(1));
        assertThat(driverNotified2.getUpdatedEndorsements().get(0), is(equalTo("SP50")));

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void shouldRecordDvlaDocumentDeliverySjpCaseWhenDocumentAvailableForSjpCase() throws IOException {
        // driverOut=false flow: dvlaFileStore=true -> DocumentGeneratorService uploads the payload to Azure
        // blob storage, and only a PENDING record carrying the blob URIs starts document-delivery tracking
        // (MaterialAggregate.recordDocumentDelivery)
        // dvlaFileStoreDelete=true -> DocumentDeletedFromBlobEventProcessor deletes both blobs once delivery completes
        final ImmutableMap<String, Boolean> features = of("driverOut", false, "dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        //Given: create the driver notification for an SJP case (initiationCode "J") carrying two cases.
        // DefendantAggregate/DriverNotifiedEngine creates one DriverNotified event PER incoming case
        // (its own materialId, its own document-generation cycle) - so this posts a single command
        // but two independent materials flow through the pipeline below.
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_CASE);

        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        // two separate driver-notified events come back, one per case/material
        final Map<String, String> materialIdByCaseId = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            final DriverNotified driverNotified = jsonToObjectConverter.convert(
                    retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
            assertThat(driverNotified.getCases(), hasSize(1));
            materialIdByCaseId.put(driverNotified.getCases().get(0).getCaseId().toString(), driverNotified.getMaterialId().toString());
        }
        assertThat(materialIdByCaseId.keySet(), containsInAnyOrder(caseId, caseId2));

        final String queryUserId = randomUUID().toString();
        stubUser(queryUserId);

        // drain the PENDING status raised by the document generation step for each material, before
        // triggering document-available - on the blob path it carries the blob URIs, which is what
        // starts tracking for that material
        for (int i = 0; i < 2; i++) {
            final JsonPath documentGeneratedEvent = retrieveMessage(consumerForDvlaDocumentDeliveryRecorded);
            assertThat(documentGeneratedEvent, is(notNullValue()));
            assertThat(materialIdByCaseId.values(), hasItem(documentGeneratedEvent.getString("materialId")));
            assertThat(documentGeneratedEvent.getString("materialStatus"), equalTo("PENDING"));
            assertThat(documentGeneratedEvent.getString("payloadBlobUri"), is(notNullValue()));
            assertThat(documentGeneratedEvent.getString("documentBlobUri"), is(notNullValue()));
        }

        // capture the two genuine document-generation requests (one per material), so each manually
        // published document-available event points at the blob DocumentGeneratorService really uploaded
        final List<JsonPath> generateDocumentRequests = latestGenerateDocumentRequests(2);

        //When/Then: simulate systemdocgenerator raising document-available (Azure blob branch of the
        // contract) for each generated document in turn, and verify the resulting SJP case
        // document-delivery event matches that document's own material/case
        for (final JsonPath generateDocumentRequest : generateDocumentRequests) {
            assertThat(generateDocumentRequest.getString("payloadFileServiceId"), is(nullValue()));
            final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
            final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");
            final String sourceCorrelationId = generateDocumentRequest.getString("sourceCorrelationId");
            assertThat(payloadFileUri, is(notNullValue()));
            // the blob SDK percent-encodes the folder separators (sdg%2Frequests%2F...), so compare decoded
            assertThat(decode(payloadFileUri, UTF_8).contains("/sdg/requests/"), is(true));
            assertThat(decode(destinationFileUri, UTF_8), equalTo(decode(payloadFileUri, UTF_8).replace("/sdg/requests/", "/sdg/documents/") + ".pdf"));

            publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri, sourceCorrelationId);

            // no documentFileServiceId exists on the blob path, so handleDvlaDocumentAvailable derives the
            // SJP case-document / correlation id deterministically from destinationFileUri
            final String expectedSjpCorrelationId = UUID.nameUUIDFromBytes(destinationFileUri.getBytes(StandardCharsets.UTF_8)).toString();

            // drain until the SJP-case event (identified by carrying a caseId) rather than assuming
            // it's the very next message on this consumer
            final JsonPath sjpCaseDocumentDeliveryEvent = retrieveSjpCaseDocumentDeliveryEvent();
            final String eventCaseId = sjpCaseDocumentDeliveryEvent.getString("caseId");
            assertThat(materialIdByCaseId, hasKey(eventCaseId));
            assertThat(sjpCaseDocumentDeliveryEvent.getString("materialId"), equalTo(materialIdByCaseId.get(eventCaseId)));
            assertThat(sjpCaseDocumentDeliveryEvent.getString("sjpCorrelationId"), equalTo(expectedSjpCorrelationId));
            assertThat(sjpCaseDocumentDeliveryEvent.getString("sjpStatus"), equalTo("PENDING"));

            // verify the read side (dvla-document-delivery query API), filtering by caseId, joins back
            // to the right material, carries this case's own sjp status - not the other case's - and
            // keeps the blob URIs recorded by the earlier PENDING step
            pollForResponse("/dvla-document-deliveries?caseId=" + eventCaseId,
                    DVLA_DOCUMENT_DELIVERY_MEDIA_TYPE, queryUserId,
                    allOf(
                            withJsonPath("$.documentDeliveries[0].materialId", equalTo(materialIdByCaseId.get(eventCaseId))),
                            withJsonPath("$.documentDeliveries[0].caseId", equalTo(eventCaseId)),
                            withJsonPath("$.documentDeliveries[0].sjpCorrelationId", equalTo(expectedSjpCorrelationId)),
                            withJsonPath("$.documentDeliveries[0].sjpStatus", equalTo("PENDING")),
                            withJsonPath("$.documentDeliveries[0].payloadBlobUri", equalTo(payloadFileUri)),
                            withJsonPath("$.documentDeliveries[0].documentBlobUri", equalTo(destinationFileUri))
                    ));

            //When: the real material context isn't deployed here either, so simulate its confirmation
            // that the generated document was stored (material.material-added) - addDocumentToMaterial
            // asked it to on document-available
            final String caseMaterialId = materialIdByCaseId.get(eventCaseId);
            publishMaterialAddedEvent(UUID.fromString(caseMaterialId), UUID.fromString(USER_GROUP));

            //Then: MaterialAddedProcessor records this material's delivery as materialStatus SUCCESS
            final JsonPath materialSuccessDocumentDeliveryEvent = retrieveMaterialSuccessDocumentDeliveryEvent(caseMaterialId);
            assertThat(materialSuccessDocumentDeliveryEvent.getString("materialStatus"), equalTo("SUCCESS"));
            assertThat(materialSuccessDocumentDeliveryEvent.getString("emailStatus"), equalTo("NOT_REQUIRED"));
            // ...but on an SJP case material SUCCESS alone must not delete the blobs - SJP still reads the document
            // from documentBlobUri (a deletion would be appended with the SUCCESS record, so a short wait suffices)
            assertThat(retrieveMessage(consumerForDocumentDeletedFromBlob), is(nullValue()));

            //When: the real sjp context isn't deployed here (only its command-api is stubbed), so
            // simulate it filing the document on the case - public.sjp.case-document-added echoes the
            // uploaded blob uri as documentUri, which SJPMaterialProcessor matches on caseId + documentBlobUri
            publishCaseDocumentAddedEvent(eventCaseId, expectedSjpCorrelationId, destinationFileUri, UUID.fromString(USER_GROUP));

            //Then: the delivery for this case's own material moves to sjpStatus SUCCESS
            final JsonPath sjpSuccessDocumentDeliveryEvent = retrieveSjpCaseDocumentDeliveryEvent();
            assertThat(sjpSuccessDocumentDeliveryEvent.getString("materialId"), equalTo(caseMaterialId));
            assertThat(sjpSuccessDocumentDeliveryEvent.getString("caseId"), equalTo(eventCaseId));
            assertThat(sjpSuccessDocumentDeliveryEvent.getString("sjpCorrelationId"), equalTo(expectedSjpCorrelationId));
            assertThat(sjpSuccessDocumentDeliveryEvent.getString("sjpStatus"), equalTo("SUCCESS"));

            pollForResponse("/dvla-document-deliveries?caseId=" + eventCaseId,
                    DVLA_DOCUMENT_DELIVERY_MEDIA_TYPE, queryUserId,
                    allOf(
                            withJsonPath("$.documentDeliveries[0].materialId", equalTo(caseMaterialId)),
                            withJsonPath("$.documentDeliveries[0].materialStatus", equalTo("SUCCESS")),
                            withJsonPath("$.documentDeliveries[0].sjpCorrelationId", equalTo(expectedSjpCorrelationId)),
                            withJsonPath("$.documentDeliveries[0].sjpStatus", equalTo("SUCCESS")),
                            withJsonPath("$.documentDeliveries[0].documentBlobUri", equalTo(destinationFileUri))
                    ));

            //Then: for an SJP case the delivery completes on sjpStatus SUCCESS (not on material SUCCESS
            // above), so only now does MaterialAggregate raise document-deleted-from-blob for this material
            verifyDocumentDeletedFromBlob(caseMaterialId, payloadFileUri, destinationFileUri);
            verifyNoEmailNotificationSent(CLIENT_CONTEXT_PREFIX + caseMaterialId);
        }
    }

    // Email verification is ASB-gateway here too (consistent with the blob-path pairing above), not
    // the legacy stub this test used before. Email-SUCCESS and the final deletion are DVLAFS-19,
    // now covered below. Renamed from
    // shouldSendDvlaEmailAndDeleteBlobsOnlyOnceEmailAndSjpBothSucceedForSjpCaseWithAzureBlob.
    @Test
    public void shouldSendDvlaEmailToNotifyGatewayAndFileSjpDocumentForSjpCaseWithAzureBlob() throws IOException {
        final ImmutableMap<String, Boolean> features = of("driverOut", false, "dvlaFileStore", true, "dvlaFileStoreDelete", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        // single SJP case, two convicting courts - forces the D20 email; SJP also files the document
        final String body = getPayload(DRIVER_NOTIFICATION_COMMAND_PAYLOAD_SJP_CASE_MULTIPLE_CONVICTING_COURTS);
        final int generateDocumentRequestsBefore = generateDocumentRequestCount();

        //When
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);
        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final DriverNotified driverNotified = jsonToObjectConverter.convert(
                retrieveMessageAsJsonObject(consumerForDriverNotified).get(), DriverNotified.class);
        assertThat(driverNotified.getCases(), hasSize(1));
        assertThat(driverNotified.getCases().get(0).getCaseId().toString(), equalTo(caseId));
        final String materialId = driverNotified.getMaterialId().toString();
        final String clientContext = CLIENT_CONTEXT_PREFIX + materialId;

        final JsonPath generateDocumentRequest = awaitGenerateDocumentRequest("DVLADocumentOrder", generateDocumentRequestsBefore);
        assertThat(generateDocumentRequest.getString("sourceCorrelationId"), equalTo(USER_GROUP));
        final String payloadFileUri = generateDocumentRequest.getString("payloadFileUri");
        final String destinationFileUri = generateDocumentRequest.getString("destinationFileUri");
        assertThat(payloadFileUri, is(notNullValue()));

        final JsonPath blobPendingDocumentDeliveryEvent = retrieveDocumentDeliveryEvent(materialId, "materialStatus", "PENDING");
        assertThat(blobPendingDocumentDeliveryEvent.getString("caseId"), equalTo(caseId));
        assertThat(blobPendingDocumentDeliveryEvent.getString("payloadBlobUri"), equalTo(payloadFileUri));

        publishDocumentAvailableEventForAzureBlob(payloadFileUri, destinationFileUri, generateDocumentRequest.getString("sourceCorrelationId"));
        final String expectedSjpCorrelationId = UUID.nameUUIDFromBytes(destinationFileUri.getBytes(StandardCharsets.UTF_8)).toString();

        final JsonPath sjpPendingDocumentDeliveryEvent = retrieveDocumentDeliveryEvent(materialId, "sjpStatus", "PENDING");
        assertThat(sjpPendingDocumentDeliveryEvent.getString("caseId"), equalTo(caseId));
        assertThat(sjpPendingDocumentDeliveryEvent.getString("sjpCorrelationId"), equalTo(expectedSjpCorrelationId));

        publishMaterialAddedEvent(driverNotified.getMaterialId(), UUID.fromString(USER_GROUP));

        final JsonPath materialSuccessDocumentDeliveryEvent = retrieveMaterialSuccessDocumentDeliveryEvent(materialId);
        assertThat(materialSuccessDocumentDeliveryEvent.getString("emailStatus"), equalTo("PENDING"));

        final ServiceBusReceivedMessage gatewayMessage = verifyEmailNotificationSentToGateway(clientContext);
        final JsonObject gatewayMessageBody = bodyOf(gatewayMessage);
        assertThat(gatewayMessageBody.getString("fileUri"), equalTo(destinationFileUri));
        assertThat(gatewayMessage.getSubject(), equalTo("send-email-notification"));
        assertThat(gatewayMessage.getReplyTo(), equalTo("stagingdvla-notification-result"));
        assertNoDocumentDeletedFromBlob(materialId);

        // SJP filing is independent of the email transport, so it's fully provable regardless
        publishCaseDocumentAddedEvent(caseId, expectedSjpCorrelationId, destinationFileUri, UUID.fromString(USER_GROUP));

        // email still PENDING - material+sjp SUCCESS alone must not delete, proving the gate still
        // requires email too
        retrieveDocumentDeliveryEvent(materialId, "sjpStatus", "SUCCESS");
        assertNoDocumentDeletedFromBlob(materialId);

        // DVLAFS-19: the gateway's reply is the last condition the gate needs - material SUCCESS
        // and sjp SUCCESS are already recorded above, so this email SUCCESS should be what flips
        // the gate and deletes both blobs
        publishNotificationSent(clientContext, randomUUID());
        retrieveEmailStatusDocumentDeliveryEvent(materialId, "SUCCESS");
        verifyDocumentDeletedFromBlob(materialId, payloadFileUri, destinationFileUri);
    }

    // matched on materialId, as other materials' deletions may be on the same consumer
    private void assertNoDocumentDeletedFromBlob(final String materialId) {
        assertThat(retrieveMessage(consumerForDocumentDeletedFromBlob, isJson(withJsonPath("$.materialId", equalTo(materialId)))),
                is(nullValue()));
    }

    // matched on materialId, as other materials' deletions may be on the same consumer
    private void verifyDocumentDeletedFromBlob(final String materialId, final String payloadBlobUri, final String documentBlobUri) {
        final JsonPath documentDeletedFromBlob = retrieveMessage(consumerForDocumentDeletedFromBlob,
                isJson(withJsonPath("$.materialId", equalTo(materialId))));
        assertThat(documentDeletedFromBlob, is(notNullValue()));
        assertThat(documentDeletedFromBlob.getString("payloadBlobUri"), equalTo(payloadBlobUri));
        assertThat(documentDeletedFromBlob.getString("documentBlobUri"), equalTo(documentBlobUri));
    }

    private JsonPath retrieveSjpCaseDocumentDeliveryEvent() {
        JsonPath event;
        do {
            event = retrieveMessage(consumerForDvlaDocumentDeliveryRecorded);
            assertThat(event, is(notNullValue()));
        } while (event.getString("caseId") == null);
        return event;
    }

    // the material SUCCESS record carries no caseId (DocumentDelivery.material), so it is picked out
    // by its own materialId and status instead
    private JsonPath retrieveMaterialSuccessDocumentDeliveryEvent(final String materialId) {
        return retrieveDocumentDeliveryEvent(materialId, "materialStatus", "SUCCESS");
    }

    private JsonPath retrieveEmailStatusDocumentDeliveryEvent(final String materialId, final String emailStatus) {
        return retrieveDocumentDeliveryEvent(materialId, "emailStatus", emailStatus);
    }

    // drains the consumer until this material's record with the given status field value
    private JsonPath retrieveDocumentDeliveryEvent(final String materialId, final String statusField, final String status) {
        JsonPath event;
        do {
            event = retrieveMessage(consumerForDvlaDocumentDeliveryRecorded);
            assertThat(event, is(notNullValue()));
        } while (!(materialId.equals(event.getString("materialId")) && status.equals(event.getString(statusField))));
        return event;
    }

    private String getPayload(String fileName) {
        String body = FileUtil.getPayload(fileName);
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%CASE_ID_2%", caseId2)
                .replaceAll("%CASE_ID%", caseId)
                .replaceAll("%DEFENDANT_ID%", defendantId);
        return body;
    }

    private void verifyEventIsCreated() {
        final JsonPath jsonResponse = retrieveMessage(consumerForDriverNotified);
        assertThat(jsonResponse, is(notNullValue()));
    }

    private void verifyEventIsNotCreated() {
        final JsonPath jsonResponse = retrieveMessage(consumerForDriverNotified);
        assertThat(jsonResponse, is(nullValue()));
    }
}

