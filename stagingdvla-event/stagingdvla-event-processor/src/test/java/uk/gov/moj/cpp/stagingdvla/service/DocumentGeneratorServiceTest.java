package uk.gov.moj.cpp.stagingdvla.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import java.util.Collections;
import java.util.UUID;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DocumentGeneratorServiceTest {

    @InjectMocks
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private SystemDocGeneratorService systemDocGeneratorService;

    @Mock
    private FileService fileService;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private Sender sender;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static final UUID ORDER_HEARING_ID = randomUUID();
    private static final String COURT_CENTER_NAME = "Liverpool Crown Court";
    private static final String PROGRESSION_ADD_COURT_DOCUMENT = "progression.add-court-document";
    private static final String SJP_UPLOAD_CASE_DOCUMENT = "sjp.upload-case-document" ;

    @Test
    public void shouldGenerateDvlaDocument() throws Exception {
        String code = "C" ;
        final DriverNotified driverNotified = generateDriverNotified(code);

        final UUID userId = randomUUID();

        final UUID payloadFileId = randomUUID();
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn(payloadFileId);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(nowsDocumentOrderJson1);
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor =  ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);
        documentGeneratorService.generateDvlaDocument(envelope, userId, driverNotified);
        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), eq(envelope));

        DocumentGenerationRequest request = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(request.getConversionFormat(),is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(),is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(),is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(),is(userId.toString()));
        assertThat(request.getPayloadFileServiceId(),is(payloadFileId));
    }

    @Test
    public void shouldGenerateDvlaDocumentForSjpCode() throws Exception {
        String code = "J" ;
        final DriverNotified driverNotified = generateDriverNotified(code);

        final UUID userId = randomUUID();

        final UUID payloadFileId = randomUUID();
        when(fileService.storePayload(any(JsonObject.class), anyString(), anyString())).thenReturn(payloadFileId);

        String inputPayload = Resources.toString(getResource("stagingdvla.command.driver-notification.json"), defaultCharset());
        final JsonObject nowsDocumentOrderJson1 = stringToJsonObjectConverter.convert(inputPayload);

        when(objectToJsonObjectConverter.convert(any())).thenReturn(nowsDocumentOrderJson1);
        ArgumentCaptor<DocumentGenerationRequest> documentGenerationRequestArgumentCaptor =  ArgumentCaptor.forClass(DocumentGenerationRequest.class);
        doNothing().when(systemDocGeneratorService).generateDocument(any(DocumentGenerationRequest.class), any(JsonEnvelope.class));
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID("public.systemdocgenerator.events.document-available"),
                nowsDocumentOrderJson1);
        documentGeneratorService.generateDvlaDocument(envelope, userId, driverNotified);
        verify(systemDocGeneratorService).generateDocument(documentGenerationRequestArgumentCaptor.capture(), eq(envelope));

        DocumentGenerationRequest request = documentGenerationRequestArgumentCaptor.getValue();
        assertThat(request.getConversionFormat(),is(ConversionFormat.PDF));
        assertThat(request.getOriginatingSource(),is("DVLADocumentOrder"));
        assertThat(request.getTemplateIdentifier(),is("EDT_DriverOutNotification"));
        assertThat(request.getSourceCorrelationId(),is(userId.toString()));
        assertThat(request.getPayloadFileServiceId(),is(payloadFileId));
    }


    public static DriverNotified generateDriverNotified(String initiationCode) {
        DriverNotified driverNotified = DriverNotified.driverNotified()
                .withOrderingCourt(CourtCentre.courtCentre()
                        .withName(COURT_CENTER_NAME).build())
                .withOrderingHearingId(ORDER_HEARING_ID)
                .withMaterialId(randomUUID())
                .withCases(Collections.singletonList(Cases.cases().withCaseId(randomUUID()).withInitiationCode(initiationCode).build()))
                .build();

        return driverNotified;
    }


}
