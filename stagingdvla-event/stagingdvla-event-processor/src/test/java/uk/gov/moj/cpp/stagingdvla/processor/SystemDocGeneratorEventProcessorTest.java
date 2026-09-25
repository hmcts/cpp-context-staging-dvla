package uk.gov.moj.cpp.stagingdvla.processor;


import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.DVLA_AUDIT_RECORDS;

import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.client.FileService;
import uk.gov.justice.services.fileservice.domain.FileReference;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.stagingdvla.service.ApplicationParameters;
import uk.gov.moj.cpp.stagingdvla.service.DocumentDeliveryStatusService;
import uk.gov.moj.cpp.stagingdvla.service.UploadMaterialContext;
import uk.gov.moj.cpp.stagingdvla.service.UploadMaterialService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SystemDocGeneratorEventProcessorTest {
    public static final String DVLA_DOCUMENT_ORDER = "DVLADocumentOrder";
    private final String DEFAULT_DRIVER_NOTIFIED_JSON = "stagingdvla.event.driver-notified.json";

    private final String identifier = randomUUID().toString();
    private final String masterDefendantId = randomUUID().toString();
    private final String materialId = randomUUID().toString();
    private final String templateId = randomUUID().toString();
    private final String caseId = randomUUID().toString();

    private static final String PAYLOAD_URI = "https://sadevfilestore.blob.core.windows.net/stack-stagingdvla/internal/DVLADocumentOrder_20250108114536";
    private static final String DESTINATION_URI = PAYLOAD_URI + ".pdf";

    @Mock
    private Sender sender;

    @InjectMocks
    private SystemDocGeneratorEventProcessor systemDocGeneratorEventProcessor;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> sendAsAdminEnvelopeCaptor;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    private final ObjectToJsonObjectConverter testConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private FileService fileService;

    @Mock
    private FileReference payloadFileReference;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private UploadMaterialService uploadMaterialService;

    @Spy
    private DocumentDeliveryStatusService documentDeliveryStatusService = new DocumentDeliveryStatusService();
    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextCaptor;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @BeforeEach
    public void setUp() {
        setField(documentDeliveryStatusService, "sender", sender);
    }


    @Test
    public void shouldProcessDriverSearchAuditReportDeletedEvent() throws FileServiceException {
        // given
        final UUID id = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final DriverRecordSearchAuditReportCreated driverSearchAuditReportCreated = DriverRecordSearchAuditReportCreated
                .driverRecordSearchAuditReportCreated()
                .withId(id)
                .withReportFileId(documentFileServiceId.toString())
                .build();
        final JsonObject jsonObject = createObjectBuilder()
                .add("originatingSource", "DvlaAuditRecords")
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .build();

        final JsonObject systemDoc = testConverter.convert(driverSearchAuditReportCreated);
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-deleted"),
                jsonObject);

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), is("stagingdvla.command.handler.driver-record-search-audit-report-created"));
        verify(objectToJsonObjectConverter).convert(any(DriverRecordSearchAuditReportCreated.class));
    }

    @Test
    public void shouldProcessDocumentAvailableEventForCcCase_SendEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();
        // when
        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(applicationParameters.getDvlaEmailTemplateId()).thenReturn(randomUUID().toString());
        when(materialUrlGenerator.pdfFileStreamUrlFor(isA(UUID.class))).thenReturn("template.pdf");
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString("stagingdvla.event.driver-notified-updated-endorsement.json", 0).getBytes(StandardCharsets.UTF_8)));

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getValue();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("progression.add-court-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("materialId"), is(materialId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getJsonObject("courtDocument"), notNullValue());

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext, notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), hasSize(1));

        verify(sender, never()).sendAsAdmin(any());
    }

    @Test
    public void shouldProcessDocumentAvailableEventForCcCase_NoEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();
        // when
        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString(DEFAULT_DRIVER_NOTIFIED_JSON, 0).getBytes(StandardCharsets.UTF_8)));

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getValue();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("progression.add-court-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("materialId"), is(materialId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getJsonObject("courtDocument"), notNullValue());

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext, notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), nullValue());
    }

    @Test
    public void shouldCarryTheBlobUrisOnToTheMaterialUploadContext() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        // Both addressing modes at once, which the event schema's oneOf forbids in production. It is
        // used here only because the payload is still recovered through payloadFileServiceId - once
        // that read moves to the blob, this becomes an ordinary uri-only event. The assertions below
        // are about the uris being threaded onward, which is independent of how the payload is read.
        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .add("payloadFileUri", PAYLOAD_URI)
                .add("destinationFileUri", DESTINATION_URI)
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);

        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();

        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString(DEFAULT_DRIVER_NOTIFIED_JSON, 0).getBytes(StandardCharsets.UTF_8)));

        // when
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);

        // then
        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();

        assertThat(uploadMaterialContext.getDestinationFileUri(), is(DESTINATION_URI));
        assertThat(uploadMaterialContext.getPayloadFileUri(), is(PAYLOAD_URI));
    }

    @Test
    public void shouldLeaveTheBlobUrisNullForAFileServiceAddressedDocument() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);

        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();

        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString(DEFAULT_DRIVER_NOTIFIED_JSON, 0).getBytes(StandardCharsets.UTF_8)));

        // when
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);

        // then
        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();

        assertThat(uploadMaterialContext.getDestinationFileUri(), nullValue());
        assertThat(uploadMaterialContext.getPayloadFileUri(), nullValue());
        assertThat(uploadMaterialContext.getFileId(), is(documentFileServiceId));
    }

    @Test
    public void shouldProcessDocumentAvailableEventForSjpCase_SendEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();
        // when
        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(applicationParameters.getDvlaEmailTemplateId()).thenReturn(randomUUID().toString());
        when(materialUrlGenerator.pdfFileStreamUrlFor(isA(UUID.class))).thenReturn("template.pdf");
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString("stagingdvla.event.driver-notified-update-sjp.json", 0).getBytes(StandardCharsets.UTF_8)));

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getAllValues().stream()
                .filter(envelope -> "sjp.upload-case-document".equals(envelope.metadata().name()))
                .findFirst().orElseThrow();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("sjp.upload-case-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseId"), is(caseId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseDocument"), is(documentFileServiceId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().containsKey("caseDocumentUri"), is(false));

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext, notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), hasSize(1));

        verify(sender, times(1)).sendAsAdmin(sendAsAdminEnvelopeCaptor.capture());
        final Envelope<JsonObject> documentDeliverySjpCaseCommand = sendAsAdminEnvelopeCaptor.getAllValues().stream()
                .filter(envelope -> "stagingdvla.command.handler.driver-notification-document-delivery".equals(envelope.metadata().name()))
                .findFirst().orElseThrow();
        assertThat(documentDeliverySjpCaseCommand.metadata().name(), is("stagingdvla.command.handler.driver-notification-document-delivery"));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("materialId"), is(materialId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("caseId"), is(caseId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("sjpCorrelationId"), is(documentFileServiceId.toString()));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("sjpStatus"), is("PENDING"));
    }

    @Test
    public void shouldProcessDocumentAvailableEventForSjpCase_NoEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();
        // when
        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString("stagingdvla.event.driver-notified-sjp.json", 0).getBytes(StandardCharsets.UTF_8)));

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getAllValues().stream()
                .filter(envelope -> "sjp.upload-case-document".equals(envelope.metadata().name()))
                .findFirst().orElseThrow();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("sjp.upload-case-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseId"), is(caseId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseDocument"), is(documentFileServiceId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().containsKey("caseDocumentUri"), is(false));

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext, notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), nullValue());

        verify(sender, times(1)).sendAsAdmin(sendAsAdminEnvelopeCaptor.capture());
        final Envelope<JsonObject> documentDeliverySjpCaseCommand = sendAsAdminEnvelopeCaptor.getValue();
        assertThat(documentDeliverySjpCaseCommand.metadata().name(), is("stagingdvla.command.handler.driver-notification-document-delivery"));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("materialId"), is(materialId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("caseId"), is(caseId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("sjpCorrelationId"), is(documentFileServiceId.toString()));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("sjpStatus"), is("PENDING"));
    }
    // The document-available contract's other oneOf branch (see document-available.json): when
    // DocumentGeneratorService uploaded the driverNotified payload to Azure blob storage instead
    // of the file-service (dvlaFileStore=false), systemdocgenerator echoes back
    // payloadFileUri/destinationFileUri instead of payloadFileServiceId/documentFileServiceId.
    private static final String AZURE_BLOB_BASE_URL = "http://cpp-azurite:10000/devstoreaccount1/stagingdvla-files/";

    @Test
    public void shouldProcessDocumentAvailableEventForCcCaseViaAzureBlob() throws FileServiceException, IOException {
        // given
        final UUID sourceCorrelationId = randomUUID();
        final String blobName = "internal/DVLADocumentOrder_" + randomUUID();
        final String payloadFileUri = AZURE_BLOB_BASE_URL + blobName.replace("/", "%2F");
        final String destinationFileUri = payloadFileUri + ".pdf";

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("payloadFileUri", payloadFileUri)
                .add("destinationFileUri", destinationFileUri)
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);

        // when
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString(buildDriverNotifiedString(DEFAULT_DRIVER_NOTIFIED_JSON, 0)));
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("filename", "DVLADocumentOrder_20250108114536"));

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);

        // BlobUrlParts strips scheme/host/account/container and %2F-decodes the rest, leaving
        // exactly the relative blob name blobContainerClient.getBlobClient(String) expects
        verify(blobContainerClient).getBlobClient(blobName);
        verify(fileService, never()).retrieve(any());

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getValue();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("progression.add-court-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("materialId"), is(materialId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getJsonObject("courtDocument"), notNullValue());

        // no documentFileServiceId exists in this branch, so Material is uploaded with the blob
        // location instead of a file-service fileId (materialDetails.json's own oneOf(fileId |
        // payloadFileUri+destinationFileUri) contract)
        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext.getFileId(), nullValue());
        assertThat(uploadMaterialContext.getPayloadFileUri(), is(payloadFileUri));
        assertThat(uploadMaterialContext.getDestinationFileUri(), is(destinationFileUri));

        verify(sender, never()).sendAsAdmin(any());
    }

    @Test
    public void shouldProcessDocumentAvailableEventForSjpCaseViaAzureBlob() throws FileServiceException, IOException {
        // given
        final UUID sourceCorrelationId = randomUUID();
        final String blobName = "internal/DVLADocumentOrder_" + randomUUID();
        final String payloadFileUri = AZURE_BLOB_BASE_URL + blobName.replace("/", "%2F");
        final String destinationFileUri = payloadFileUri + ".pdf";

        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("payloadFileUri", payloadFileUri)
                .add("destinationFileUri", destinationFileUri)
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);

        // when
        when(blobContainerClient.getBlobClient(blobName)).thenReturn(blobClient);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString(buildDriverNotifiedString("stagingdvla.event.driver-notified-sjp.json", 0)));
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getMetadata()).thenReturn(Map.of("filename", "DVLADocumentOrder_20250108114536"));

        // then: no documentFileServiceId exists in this branch - the document is filed with SJP by
        // its blob uri, and the delivery correlation is the v3 UUID of destinationFileUri, the same
        // id SJP derives for a blob-addressed case document
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        final String expectedCaseDocumentId = UUID.nameUUIDFromBytes(destinationFileUri.getBytes(StandardCharsets.UTF_8)).toString();

        verify(blobContainerClient).getBlobClient(blobName);
        verify(fileService, never()).retrieve(any());

        verify(sender, times(1)).send(envelopeCaptor.capture());
        final Envelope<JsonObject> sjpUploadCaseDocument = envelopeCaptor.getAllValues().stream()
                .filter(envelope -> "sjp.upload-case-document".equals(envelope.metadata().name()))
                .findFirst().orElseThrow();
        assertThat(sjpUploadCaseDocument.payload().getString("caseId"), is(caseId.toString()));
        assertThat(sjpUploadCaseDocument.payload().getString("caseDocumentUri"), is(destinationFileUri));
        assertThat(sjpUploadCaseDocument.payload().containsKey("caseDocument"), is(true));

        verify(sender, times(1)).sendAsAdmin(sendAsAdminEnvelopeCaptor.capture());
        final Envelope<JsonObject> documentDeliverySjpCaseCommand = sendAsAdminEnvelopeCaptor.getValue();
        assertThat(documentDeliverySjpCaseCommand.metadata().name(), is("stagingdvla.command.handler.driver-notification-document-delivery"));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("materialId"), is(materialId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("caseId"), is(caseId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("sjpCorrelationId"), is(expectedCaseDocumentId));
        assertThat(documentDeliverySjpCaseCommand.payload().getString("sjpStatus"), is("PENDING"));

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        assertThat(uploadMaterialContextCaptor.getValue().getFileId(), nullValue());
    }

    @Test
    public void shouldFailFastWhenDocumentAvailableEventHasNeitherPayloadFileIdNorPayloadFileUri() {
        // given: malformed relative to the document-available schema's oneOf - neither
        // payloadFileServiceId nor payloadFileUri present
        final JsonObject documentAvailablePayload = createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("sourceCorrelationId", randomUUID().toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);

        // then: getDriverNotifiedFromDocument's else branch unconditionally treats a missing
        // payloadFileId as "read from blob" and calls BlobUrlParts.parse(null) - pinning today's
        // actual failure mode rather than asserting it's the ideal one
        assertThrows(IllegalArgumentException.class, () -> systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage));
    }

    @Test
    public void shouldRecordDeliveryStatusFailedWhenDocumentGenerationFailsForDvlaDocumentOrder() {
        systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(
                generationFailedEvent(DVLA_DOCUMENT_ORDER, AZURE_BLOB_BASE_URL + "internal%2FDVLADocumentOrder_" + randomUUID()));

        assertDeliveryStatusFailedRecorded();
    }

    @Test
    public void shouldRecordDeliveryStatusFailedWhenDocumentGenerationFailsForDriverAuditReport() {
        systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(
                generationFailedEvent(DVLA_AUDIT_RECORDS, AZURE_BLOB_BASE_URL + "internal%2FDvlaAuditRecords_" + randomUUID()));

        assertDeliveryStatusFailedRecorded();
    }

    @Test
    public void shouldMatchOriginatingSourceIgnoringCaseWhenDocumentGenerationFails() {
        systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(
                generationFailedEvent(DVLA_DOCUMENT_ORDER.toUpperCase(), AZURE_BLOB_BASE_URL + "internal%2FDVLADocumentOrder_" + randomUUID()));

        assertDeliveryStatusFailedRecorded();
    }

    @Test
    public void shouldNotRecordDeliveryStatusWhenDocumentGenerationFailsWithoutPayloadFileUri() {
        // file-service path: no blob-backed PENDING record exists, so the material is not tracked
        systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(generationFailedEvent(DVLA_DOCUMENT_ORDER, null));

        verifyNoInteractions(sender);
    }

    @Test
    public void shouldNotRecordDeliveryStatusWhenDocumentGenerationFailsForOtherOriginatingSource() {
        systemDocGeneratorEventProcessor.handleDocumentGenerationFailedEvent(
                generationFailedEvent("OtherContextDocument", AZURE_BLOB_BASE_URL + "internal%2FOther_" + randomUUID()));

        verifyNoInteractions(sender);
    }

    private JsonEnvelope generationFailedEvent(final String originatingSource, final String payloadFileUri) {
        final JsonObjectBuilder payload = createObjectBuilder()
                .add("originatingSource", originatingSource)
                .add("sourceCorrelationId", materialId);
        if (payloadFileUri != null) {
            payload.add("payloadFileUri", payloadFileUri);
        } else {
            payload.add("payloadFileServiceId", randomUUID().toString());
        }
        return envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.generation-failed"), payload.build());
    }

    private void assertDeliveryStatusFailedRecorded() {
        verify(sender).sendAsAdmin(sendAsAdminEnvelopeCaptor.capture());
        final Envelope<JsonObject> documentDeliveryFailedCommand = sendAsAdminEnvelopeCaptor.getValue();
        assertThat(documentDeliveryFailedCommand.metadata().name(), is("stagingdvla.command.handler.driver-notification-document-delivery"));
        assertThat(documentDeliveryFailedCommand.payload().getString("materialId"), is(materialId));
        assertThat(documentDeliveryFailedCommand.payload().getString("materialStatus"), is("FAILED"));

        verify(sender, never()).send(any());
    }

    private String buildDriverNotifiedString(final String resourcename, final int retrySequence) throws IOException {
        String inputPayload = Resources.toString(getResource(resourcename), defaultCharset());
        inputPayload = inputPayload.replace("MASTER_DEFENDANT_ID", masterDefendantId)
                .replace("MATERIAL_ID", materialId)
                .replace("CASE_ID", caseId)
                .replace("IDENTIFIER", identifier)
                .replace("RETRY_SEQUENCE", Integer.toString(retrySequence))
                .replace("TEMPLATE_ID", templateId);
        return inputPayload;
    }
}
