package uk.gov.moj.cpp.stagingdvla.processor;


import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;
import uk.gov.moj.cpp.stagingdvla.service.MaterialService;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class DriverSearchAuditReportEventProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private FileStorer fileStorer;

    @InjectMocks
    private DriverSearchAuditReportEventProcessor driverSearchAuditReportEventProcessor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;


    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonObjectConverter objectToJsonObjectConverter = mock(ObjectToJsonObjectConverter.class);

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    @Mock
    private DriverAuditRepository driverAuditRepository;

    @Mock
    private MaterialService materialService;
    @Mock
    private SystemUserProvider userProvider;

    @Test
    public void shouldProcessDriverSearchAuditReportRequestedEvent() throws FileServiceException {
        // given
        final UUID id = randomUUID();
        final String startDate = now().toString();
        final String endDate = now().toString();
        final LocalDateTime startDateTime = LocalDateTime.now();
        final LocalDateTime endDateTime = LocalDateTime.now().plusDays(1);
        final UUID userId = randomUUID();
        final String drivingLicenseNumber = "DRIVERXXXX12XXX";
        final String userEmail = "pert21@gmail.co.uk";

        final DriverSearchAuditReportRequested driverSearchAuditReportRequested =
                new DriverSearchAuditReportRequested(LocalDateTime.now().toString(), id,
                        DriverAuditReportSearchCriteria.driverAuditReportSearchCriteria()
                                .withEmail(userEmail)
                                .withDriverNumber(drivingLicenseNumber)
                                .withStartDate(startDate)
                                .withEndDate(endDate)
                                .build(), userId);

        final JsonObject searchAuditReport = objectToJsonObjectConverter.convert(driverSearchAuditReportRequested);

        //when
        when(jsonObjectToObjectConverter.convert(searchAuditReport, DriverSearchAuditReportRequested.class)).thenReturn(driverSearchAuditReportRequested);
        final DriverAuditEntity driverAuditEntity =
                new DriverAuditEntity(id, userId, "", ZonedDateTime.now(),
                        "Auto Case Enquiry", "REF123", "DRIVER12XX",
                        "JOHN", "DOE", "MALE", "", now());
        final List<DriverAuditEntity> driverAuditEntityList = new ArrayList<>();
        driverAuditEntityList.add(driverAuditEntity);

        when(driverAuditRepository.
                findAllActiveDriverAuditRecords(startDateTime, endDateTime, drivingLicenseNumber, userEmail))
                .thenReturn(driverAuditEntityList);
        when(fileStorer.store(any(), any())).thenReturn(randomUUID());

        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-requested"),
                searchAuditReport);

        // then
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportRequested(requestMessage);
        verify(sender, times(0)).sendAsAdmin(envelopeCaptor.capture());
    }

    @Test
    public void shouldProcessDriverSearchAuditReportCreatedEvent() {
        //given
        final UUID id = randomUUID();
        final UUID reportFileId = randomUUID();
        final UUID materialId = randomUUID();
        final DriverSearchAuditReportCreated driverSearchAuditReportCreated = DriverSearchAuditReportCreated
                .driverSearchAuditReportCreated()
                .withId(id)
                .withReportFileId(reportFileId.toString())
                .withMaterialId(materialId)
                .build();

        final JsonObject searchAuditReportCreated = objectToJsonObjectConverter.convert(driverSearchAuditReportCreated);
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-created"),
                searchAuditReportCreated);
        // when
        when(jsonObjectToObjectConverter.convert(searchAuditReportCreated, DriverSearchAuditReportCreated.class)).thenReturn(driverSearchAuditReportCreated);
        when(userProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));

        // then
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportCreated(requestMessage);
        verify(sender, times(0)).send(envelopeCaptor.capture());
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

        final JsonObject searchAuditReportStored = objectToJsonObjectConverter.convert(driverSearchAuditReportStored);
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-stored"),
                searchAuditReportStored);
        // when
        when(jsonObjectToObjectConverter.convert(searchAuditReportStored, DriverSearchAuditReportStored.class)).thenReturn(driverSearchAuditReportStored);
        when(userProvider.getContextSystemUserId()).thenReturn(Optional.of(randomUUID()));

        // then
        driverSearchAuditReportEventProcessor.handleDriverSearchAuditReportStored(requestMessage);
        verify(sender, times(0)).send(envelopeCaptor.capture());

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

        final JsonObject searchAuditReportDeleted = objectToJsonObjectConverter.convert(driverSearchAuditReportDeleted);
        final JsonEnvelope requestMessage = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-deleted"),
                searchAuditReportDeleted);
        // when
        when(jsonObjectToObjectConverter.convert(searchAuditReportDeleted, DriverSearchAuditReportDeleted.class)).thenReturn(driverSearchAuditReportDeleted);

        // then
        driverSearchAuditReportEventProcessor.processDriverSearchAuditReportDeleted(requestMessage);
        verify(sender, times(0)).send(envelopeCaptor.capture());
    }

    @Test
    public void shouldProcessDriverSearchAuditReportDeletionFailed() {
        // given
        final UUID id = randomUUID();


        final JsonObject searchAuditReportDeletionFailed = Json.createObjectBuilder()
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
