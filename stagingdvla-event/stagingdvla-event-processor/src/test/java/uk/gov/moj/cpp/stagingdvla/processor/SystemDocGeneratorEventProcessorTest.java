package uk.gov.moj.cpp.stagingdvla.processor;


import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

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
import uk.gov.moj.cpp.stagingdvla.service.UploadMaterialContext;
import uk.gov.moj.cpp.stagingdvla.service.UploadMaterialService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private Sender sender;

    @InjectMocks
    private SystemDocGeneratorEventProcessor systemDocGeneratorEventProcessor;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

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
    @Captor
    private ArgumentCaptor<UploadMaterialContext> uploadMaterialContextCaptor;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);


    @Test
    public void shouldProcessDriverSearchAuditReportDeletedEvent() {
        // given
        final UUID id = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final DriverRecordSearchAuditReportCreated driverSearchAuditReportCreated = DriverRecordSearchAuditReportCreated
                .driverRecordSearchAuditReportCreated()
                .withId(id)
                .withReportFileId(documentFileServiceId.toString())
                .build();
        final JsonObject jsonObject = Json.createObjectBuilder()
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

        final JsonObject documentAvailablePayload = Json.createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = Json.createObjectBuilder()
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
    }

    @Test
    public void shouldProcessDocumentAvailableEventForCcCase_NoEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = Json.createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = Json.createObjectBuilder()
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
    public void shouldProcessDocumentAvailableEventForSjpCase_SendEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = Json.createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = Json.createObjectBuilder()
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
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getValue();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("sjp.upload-case-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseId"), is(caseId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseDocument"), is(documentFileServiceId.toString()));

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext, notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), hasSize(1));

    }

    @Test
    public void shouldProcessDocumentAvailableEventForSjpCase_NoEmailNotification() throws FileServiceException, IOException {
        // given
        final UUID payloadFileId = randomUUID();
        final UUID sourceCorrelationId = randomUUID();
        final UUID documentFileServiceId = randomUUID();

        final JsonObject documentAvailablePayload = Json.createObjectBuilder()
                .add("originatingSource", DVLA_DOCUMENT_ORDER)
                .add("documentFileServiceId", documentFileServiceId.toString())
                .add("sourceCorrelationId", sourceCorrelationId.toString())
                .add("payloadFileServiceId", payloadFileId.toString())
                .build();

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                documentAvailablePayload);


        final JsonObject fileMetadata = Json.createObjectBuilder()
                .add("fileName", "DVLADocumentOrder_20250108114536")
                .build();
        // when
        when(fileService.retrieve(any())).thenReturn(Optional.of(payloadFileReference));
        when(payloadFileReference.getMetadata()).thenReturn(fileMetadata);
        when(payloadFileReference.getContentStream()).thenReturn(new ByteArrayInputStream(buildDriverNotifiedString("stagingdvla.event.driver-notified-sjp.json", 0).getBytes(StandardCharsets.UTF_8)));

        // then
        systemDocGeneratorEventProcessor.handleDocumentAvailable(requestMessage);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> addCourtDocumentRequestToProgression = envelopeCaptor.getValue();
        assertThat(addCourtDocumentRequestToProgression.metadata().name(), is("sjp.upload-case-document"));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseId"), is(caseId.toString()));
        assertThat(addCourtDocumentRequestToProgression.payload().getString("caseDocument"), is(documentFileServiceId.toString()));

        verify(uploadMaterialService).uploadFile(uploadMaterialContextCaptor.capture());
        final UploadMaterialContext uploadMaterialContext = uploadMaterialContextCaptor.getValue();
        assertThat(uploadMaterialContext, notNullValue());
        assertThat(uploadMaterialContext.getEmailNotifications(), nullValue());
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
