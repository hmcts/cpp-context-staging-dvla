package uk.gov.moj.cpp.stagingdvla.processor;


import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;
import uk.gov.moj.cpp.stagingdvla.service.ConversionFormat;
import uk.gov.moj.cpp.stagingdvla.service.DocumentGeneratorService;
import uk.gov.moj.cpp.stagingdvla.service.MaterialService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class DriverSearchAuditReportEventProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private DriverSearchAuditReportEventProcessor driverSearchAuditReportEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;


    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Mock
    private DriverAuditRepository driverAuditRepository;

    @Mock
    private MaterialService materialService;
    @Mock
    private SystemUserProvider userProvider;
    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Test
    public void shouldProcessDriverSearchAuditReportRequestedEvent() throws FileServiceException {
        // given
        final DriverSearchAuditReportRequested auditReportRequested = givenAuditReportRequested();

        when(driverAuditRepository.findAllActiveDriverAuditRecords(any(), any(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        final JsonEnvelope requestMessage = givenRequestEnvelope(auditReportRequested);

        // when
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportRequested(requestMessage);

        // then
        final GeneratedDocumentCall generatedDocumentCall = capturedGenerateDocumentCall(requestMessage);
        assertThat(generatedDocumentCall.materialId, is(auditReportRequested.getId()));
        assertThat(generatedDocumentCall.templateName, is("DvlaAuditRecords"));
        assertThat(generatedDocumentCall.originatingSource, is("DvlaAuditRecords"));
        assertThat(generatedDocumentCall.format, is(ConversionFormat.CSV));
        assertThat(generatedDocumentCall.fileName.endsWith(".csv"), is(true));
        assertThat(generatedDocumentCall.payload.containsKey("driverAuditRecords"), is(true));
        // Document generation (including any file-store/blob-storage decision) is fully delegated
        // to documentGeneratorService, so the processor itself must never touch sender directly.
        verifyNoInteractions(sender);
    }

    @Test
    public void shouldBuildAuditReportPayloadRowsFromDriverAuditRecordsHandlingNullOptionalFields() throws FileServiceException {
        // Covers getAuditReportDocumentGeneratorPayload's per-field null-check branches, which
        // were previously untested because every existing test stubbed the repository to return
        // an empty list, so the row-building loop body never ran.
        final DriverSearchAuditReportRequested auditReportRequested = givenAuditReportRequested();

        final DriverAuditEntity populatedEntity = new DriverAuditEntity(randomUUID(), randomUUID(), "driver@example.com",
                ZonedDateTime.now().minusDays(1), "SEARCH", "REF-1", "DRIVER123", "Jane", "Doe", "FEMALE", "SW1A 1AA",
                LocalDate.of(1990, 1, 1));
        final DriverAuditEntity entityWithNullOptionalFields = new DriverAuditEntity(randomUUID(), randomUUID(), "other@example.com",
                ZonedDateTime.now(), "SEARCH", "REF-2", null, null, null, null, null, null);

        when(driverAuditRepository.findAllActiveDriverAuditRecords(any(), any(), anyString(), anyString()))
                .thenReturn(List.of(populatedEntity, entityWithNullOptionalFields));

        final JsonEnvelope requestMessage = givenRequestEnvelope(auditReportRequested);

        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportRequested(requestMessage);

        final GeneratedDocumentCall generatedDocumentCall = capturedGenerateDocumentCall(requestMessage);
        final JsonArray records = generatedDocumentCall.payload.getJsonArray("driverAuditRecords");
        assertThat(records.size(), is(2));

        final JsonObject populatedRow = records.getJsonObject(0);
        assertThat(populatedRow.getString("Driver number"), is("DRIVER123"));
        assertThat(populatedRow.getString("First name"), is("Jane"));
        assertThat(populatedRow.getString("Last name"), is("Doe"));
        assertThat(populatedRow.getString("Date of Birth"), is(LocalDate.of(1990, 1, 1).toString()));
        assertThat(populatedRow.getString("Gender"), is("FEMALE"));
        assertThat(populatedRow.getString("Postcode"), is("SW1A 1AA"));
        assertThat(populatedRow.getString("Searched By"), is("driver@example.com"));
        assertThat(populatedRow.getString("Reference"), is("REF-1"));

        final JsonObject rowWithNullOptionalFields = records.getJsonObject(1);
        assertThat(rowWithNullOptionalFields.getString("Driver number"), is(""));
        assertThat(rowWithNullOptionalFields.getString("First name"), is(""));
        assertThat(rowWithNullOptionalFields.getString("Last name"), is(""));
        assertThat(rowWithNullOptionalFields.getString("Date of Birth"), is(""));
        assertThat(rowWithNullOptionalFields.getString("Gender"), is(""));
        assertThat(rowWithNullOptionalFields.getString("Postcode"), is(""));
        // userEmail is a not-null column on DriverAuditEntity, so it is always populated -
        // unlike the other optional fields above, it is never defaulted to "".
        assertThat(rowWithNullOptionalFields.getString("Searched By"), is("other@example.com"));
        assertThat(rowWithNullOptionalFields.getString("Reference"), is("REF-2"));
    }

    private static final class GeneratedDocumentCall {
        private final UUID materialId;
        private final JsonObject payload;
        private final String fileName;
        private final String templateName;
        private final ConversionFormat format;
        private final String originatingSource;

        private GeneratedDocumentCall(final UUID materialId, final JsonObject payload, final String fileName,
                                       final String templateName, final ConversionFormat format, final String originatingSource) {
            this.materialId = materialId;
            this.payload = payload;
            this.fileName = fileName;
            this.templateName = templateName;
            this.format = format;
            this.originatingSource = originatingSource;
        }
    }

    private GeneratedDocumentCall capturedGenerateDocumentCall(final JsonEnvelope requestMessage) {
        final ArgumentCaptor<UUID> materialIdCaptor = ArgumentCaptor.forClass(UUID.class);
        final ArgumentCaptor<JsonObject> payloadCaptor = ArgumentCaptor.forClass(JsonObject.class);
        final ArgumentCaptor<String> fileNameCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> templateNameCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<ConversionFormat> formatCaptor = ArgumentCaptor.forClass(ConversionFormat.class);
        final ArgumentCaptor<String> originatingSourceCaptor = ArgumentCaptor.forClass(String.class);

        verify(documentGeneratorService).generateDocument(eq(requestMessage), materialIdCaptor.capture(), payloadCaptor.capture(),
                fileNameCaptor.capture(), templateNameCaptor.capture(), formatCaptor.capture(), originatingSourceCaptor.capture());

        return new GeneratedDocumentCall(materialIdCaptor.getValue(), payloadCaptor.getValue(), fileNameCaptor.getValue(),
                templateNameCaptor.getValue(), formatCaptor.getValue(), originatingSourceCaptor.getValue());
    }

    private JsonEnvelope givenRequestEnvelope(final DriverSearchAuditReportRequested auditReportRequested) {
        final JsonObject envelopePayload = createObjectBuilder().add("id", auditReportRequested.getId().toString()).build();
        when(jsonObjectToObjectConverter.convert(envelopePayload, DriverSearchAuditReportRequested.class)).thenReturn(auditReportRequested);

        return envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-requested"), envelopePayload);
    }

    private DriverSearchAuditReportRequested givenAuditReportRequested() {
        final UUID id = randomUUID();
        final UUID userId = randomUUID();
        final String date = now().toString();

        final DriverAuditReportSearchCriteria searchCriteria = DriverAuditReportSearchCriteria.driverAuditReportSearchCriteria()
                .withEmail("pert21@gmail.co.uk")
                .withDriverNumber("DRIVERXXXX12XXX")
                .withStartDate(date)
                .withEndDate(date)
                .build();

        return DriverSearchAuditReportRequested.driverSearchAuditReportRequested()
                .withId(id)
                .withDateTime(ZonedDateTime.now().toString())
                .withUserId(userId)
                .withReportSearchCriteria(searchCriteria)
                .build();
    }

    @Test
    public void shouldProcessDriverSearchAuditReportCreatedEvent() {
        //given
        final UUID id = randomUUID();
        final UUID reportFileId = randomUUID();
        final UUID materialId = randomUUID();
        final UUID systemUserId = randomUUID();
        final DriverSearchAuditReportCreated driverSearchAuditReportCreated = DriverSearchAuditReportCreated
                .driverSearchAuditReportCreated()
                .withId(id)
                .withReportFileId(reportFileId.toString())
                .withMaterialId(materialId)
                .build();

        // NOTE: previously this built the envelope payload via the unstubbed
        // objectToJsonObjectConverter mock, which returns null and made envelope.payloadIsNull()
        // short-circuit the handler body entirely - the test passed without ever exercising
        // processDriverSearchAuditReportCreated's real logic. Use a real (non-null) JsonObject
        // instead; its actual shape doesn't matter because jsonObjectToObjectConverter.convert(..)
        // is stubbed below to return the POJO directly.
        final JsonObject searchAuditReportCreated = createObjectBuilder().add("id", id.toString()).build();
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-created"),
                searchAuditReportCreated);
        // when
        when(jsonObjectToObjectConverter.convert(searchAuditReportCreated, DriverSearchAuditReportCreated.class)).thenReturn(driverSearchAuditReportCreated);
        when(userProvider.getContextSystemUserId()).thenReturn(Optional.of(systemUserId));

        // then
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportCreated(requestMessage);

        verify(materialService).uploadMaterial(eq(reportFileId), eq(materialId), eq(systemUserId), eq(MaterialService.AUDIT_REPORT_ORIGINATOR_VALUE), eq(id));
        verifyNoInteractions(sender);
    }

    @Test
    public void shouldProcessDriverSearchAuditReportStoredEvent() {
        // given
        final UUID id = randomUUID();
        final UUID materialId = randomUUID();
        final DriverSearchAuditReportStored driverSearchAuditReportStored = DriverSearchAuditReportStored
                .driverSearchAuditReportStored()
                .withId(id)
                .withMaterialId(materialId)
                .build();

        // See note in shouldProcessDriverSearchAuditReportCreatedEvent: build a real payload so
        // envelope.payloadIsNull() does not short-circuit the handler under test.
        final JsonObject searchAuditReportStored = createObjectBuilder().add("id", id.toString()).build();
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-stored"),
                searchAuditReportStored);
        // when
        when(jsonObjectToObjectConverter.convert(searchAuditReportStored, DriverSearchAuditReportStored.class)).thenReturn(driverSearchAuditReportStored);

        // then
        driverSearchAuditReportEventProcessor.handleDriverSearchAuditReportStored(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertEquals("public.stagingdvla.event.driver-search-audit-report-generated", publicEvent.metadata().name());
    }

    @Test
    public void shouldProcessDriverSearchAuditReportDeletedEvent() {
        // given
        final UUID id = randomUUID();
        final UUID materialId = randomUUID();
        final DriverSearchAuditReportDeleted driverSearchAuditReportDeleted = DriverSearchAuditReportDeleted
                .driverSearchAuditReportDeleted()
                .withId(id)
                .withMaterialId(materialId)
                .build();

        // See note in shouldProcessDriverSearchAuditReportCreatedEvent: build a real payload so
        // envelope.payloadIsNull() does not short-circuit the handler under test.
        final JsonObject searchAuditReportDeleted = createObjectBuilder().add("id", id.toString()).build();
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-deleted"),
                searchAuditReportDeleted);
        // when
        when(jsonObjectToObjectConverter.convert(searchAuditReportDeleted, DriverSearchAuditReportDeleted.class)).thenReturn(driverSearchAuditReportDeleted);

        // then
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportDeleted(requestMessage);

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertEquals("public.stagingdvla.event.driver-search-audit-report-deleted", publicEvent.metadata().name());
        verify(materialService).sendCommandToDeleteMaterial(requestMessage, materialId);
    }

    @Test
    public void shouldProcessDriverSearchAuditReportDeletionFailed() {
        // given
        final UUID id = randomUUID();


        final JsonObject searchAuditReportDeletionFailed = createObjectBuilder()
                .add("id", id.toString())
                .build();
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-deletion-failed"),
                searchAuditReportDeletionFailed);
        // when
        when(userProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));

        // then
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportDeletionFailed(requestMessage);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonObject> publicEvent = envelopeCaptor.getValue();
        assertEquals("public.stagingdvla.event.driver-search-audit-report-deletion-failed", publicEvent.metadata().name());
    }
}
