package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;
import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditReportRepository;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;
import uk.gov.moj.cpp.persistence.repository.DrivingConvictionRetryRepository;
import uk.gov.moj.cpp.stagingdvla.domain.DrivingConvictionRetry;
import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;
import uk.gov.moj.cpp.stagingdvla.query.view.converter.DrivingConvictionRetryConverter;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverAuditQueryParameters;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
class StagingdvlaQueryViewTest {

    private static final String STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY = "stagingdvla.query.driving-conviction-retry";
    private static final String DRIVING_CONVICTION_RETRIES = "drivingConvictionRetries";
    private static final String MAX_COUNT = "maxCount";

    @InjectMocks
    private StagingdvlaQueryView stagingdvlaQueryView;

    @Mock
    private DrivingConvictionRetryRepository drivingConvictionRetryRepository;

    @Mock
    private DrivingConvictionRetryConverter drivingConvictionRetryConverter;

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Mock
    private DriverAuditRepository driverAuditRepository;

    @Mock
    private DriverAuditReportRepository driverAuditReportRepository;

    @Mock
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Captor
    private ArgumentCaptor<Object> objectArgument;


    @BeforeEach
    public void init() {
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
     void shouldReturnRetryRecordsWhen1ItemRequested() throws IOException {
        List<DrivingConvictionRetryEntity> oneRetryEntity = getDrivingConvictionRetryEntities(1);
        List<DrivingConvictionRetryEntity> allRetryEntities = getDrivingConvictionRetryEntities(10);

        when(drivingConvictionRetryRepository.findAll(anyInt(), anyInt())).thenReturn(oneRetryEntity);
        when(drivingConvictionRetryConverter.convert(oneRetryEntity)).thenReturn(convertRetryEntities(oneRetryEntity));
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithRandomUUID(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY),
                createObjectBuilder().add(MAX_COUNT, 1).build());

        final Envelope<JsonObject> response = stagingdvlaQueryView.findDrivingConvictionRetries(requestEnvelope);

        assertThat(response.metadata().name(), is(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY));
        assertThat(response.payload().getJsonArray(DRIVING_CONVICTION_RETRIES), hasSize(1));
    }

    @Test
    void shouldReturnRetryRecordsWhenAllItemsRequested() throws IOException {
        List<DrivingConvictionRetryEntity> oneRetryEntity = getDrivingConvictionRetryEntities(1);
        List<DrivingConvictionRetryEntity> allRetryEntities = getDrivingConvictionRetryEntities(10);

        when(drivingConvictionRetryRepository.findAll()).thenReturn(allRetryEntities);

        when(drivingConvictionRetryConverter.convert(allRetryEntities)).thenReturn(convertRetryEntities(allRetryEntities));
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithRandomUUID(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY),
                createObjectBuilder().add(MAX_COUNT, 0).build());

        final Envelope<JsonObject> response = stagingdvlaQueryView.findDrivingConvictionRetries(requestEnvelope);

        assertThat(response.metadata().name(), is(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY));
        assertThat(response.payload().getJsonArray(DRIVING_CONVICTION_RETRIES), hasSize(10));
    }

    @Test
    void shouldReturnRetryRecordsWhenAllItemsRequestedButNoRecordExists() throws IOException {
        List<DrivingConvictionRetryEntity> oneRetryEntity = getDrivingConvictionRetryEntities(1);
        List<DrivingConvictionRetryEntity> allRetryEntities = getDrivingConvictionRetryEntities(10);

        when(drivingConvictionRetryRepository.findAll(anyInt(), anyInt())).thenReturn(oneRetryEntity);
        when(drivingConvictionRetryRepository.findAll()).thenReturn(allRetryEntities);

        when(drivingConvictionRetryConverter.convert(oneRetryEntity)).thenReturn(convertRetryEntities(oneRetryEntity));
        when(drivingConvictionRetryConverter.convert(allRetryEntities)).thenReturn(convertRetryEntities(allRetryEntities));
        when(drivingConvictionRetryRepository.findAll()).thenReturn(null);

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithRandomUUID(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY),
                createObjectBuilder().add(MAX_COUNT, 0).build());

        final Envelope<JsonObject> response = stagingdvlaQueryView.findDrivingConvictionRetries(requestEnvelope);

        assertThat(response.metadata().name(), is(STAGINGDVLA_QUERY_DRIVING_CONVICTION_RETRY));
        assertThat(response.payload().getJsonArray(DRIVING_CONVICTION_RETRIES), hasSize(0));
    }

    @Test
    void shouldReturnDvlaAuditRecordsWithEmail() {
        //given
        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = LocalDate.now().plusDays(1);
        final String email = "Perter21@co.uk";
        final DriverAuditQueryParameters driverAuditQueryParameters = new DriverAuditQueryParameters(null, email, startDate.toString(), endDate.toString());
        final Envelope<DriverAuditQueryParameters> jsonEnvelope = createDriverQueryParameter(driverAuditQueryParameters);

        //when
        DriverAuditEntity driverAuditEntity = new DriverAuditEntity(randomUUID(), randomUUID(), email,
                now(), "Case Enquiry", "reference123", null, "Peter", "Parker", "Male", "CR0 5UA", LocalDate.now());

        List<DriverAuditEntity> driverAuditEntityList = Collections.singletonList(driverAuditEntity);

        when(driverAuditRepository.
                findAllActiveDriverAuditRecords(any(), any(), eq(null), eq(email.toLowerCase()))).thenReturn(driverAuditEntityList);
        Envelope<JsonObject> response = stagingdvlaQueryView.getDriverAuditRecords(jsonEnvelope);
        JsonObject jsonObject = response.payload().getJsonArray("driverAuditRecords").getJsonObject(0);

        //then
        assertThat(response.metadata().name(), is("stagingdvla.query.driver-audit-records"));
        assertThat(response.payload().getJsonArray("driverAuditRecords"), hasSize(1));

        assertThat(jsonObject.getString("reason"), is("Case Enquiry"));
        assertThat(jsonObject.getString("reference"), is("reference123"));
        assertThat(jsonObject.getString("firstName"), is("Peter"));
        assertThat(jsonObject.getString("lastName"), is("Parker"));
        assertThat(jsonObject.getString("gender"), is("Male"));
        assertThat(jsonObject.getString("postcode"), is("CR0 5UA"));
        assertThat(jsonObject.getString("searchedBy"), is("Perter21@co.uk"));
    }
    @Test
    void shouldReturnDvlaAuditRecordsWithCaseInsensitiveEmail() {
        //given
        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = LocalDate.now().plusDays(1);
        final String email = "Perter21@co.uk";
        final DriverAuditQueryParameters driverAuditQueryParameters = new DriverAuditQueryParameters(null, email, startDate.toString(), endDate.toString());
        final Envelope<DriverAuditQueryParameters> jsonEnvelope = createDriverQueryParameter(driverAuditQueryParameters);

        //when
        DriverAuditEntity driverAuditEntity = new DriverAuditEntity(randomUUID(), randomUUID(), email,
                now(), "Case Enquiry", "reference123", null, "Peter", "Parker", "Male", "CR0 5UA", LocalDate.now());

        List<DriverAuditEntity> driverAuditEntityList = Collections.singletonList(driverAuditEntity);

        when(driverAuditRepository.
                findAllActiveDriverAuditRecords(any(), any(), eq(null), eq(email.toLowerCase()))).thenReturn(driverAuditEntityList);
        Envelope<JsonObject> response = stagingdvlaQueryView.getDriverAuditRecords(jsonEnvelope);
        JsonObject jsonObject = response.payload().getJsonArray("driverAuditRecords").getJsonObject(0);

        //then
        assertThat(response.metadata().name(), is("stagingdvla.query.driver-audit-records"));
        assertThat(response.payload().getJsonArray("driverAuditRecords"), hasSize(1));

        assertThat(jsonObject.getString("reason"), is("Case Enquiry"));
        assertThat(jsonObject.getString("reference"), is("reference123"));
        assertThat(jsonObject.getString("firstName"), is("Peter"));
        assertThat(jsonObject.getString("lastName"), is("Parker"));
        assertThat(jsonObject.getString("gender"), is("Male"));
        assertThat(jsonObject.getString("postcode"), is("CR0 5UA"));
        assertThat(jsonObject.getString("searchedBy"), is("Perter21@co.uk"));
    }

    @Test
    public void shouldReturnDvlaAuditRecordsWithDriverNumber() {
        //given
        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = LocalDate.now().plusDays(1);
        final String driverNumber = "Driver1234";
        final DriverAuditQueryParameters driverAuditQueryParameters = new DriverAuditQueryParameters(driverNumber, null, startDate.toString(), endDate.toString());
        final Envelope<DriverAuditQueryParameters> jsonEnvelope = createDriverQueryParameter(driverAuditQueryParameters);

        //when
        DriverAuditEntity driverAuditEntity = new DriverAuditEntity(randomUUID(), randomUUID(), null,
                now(), "Case Enquiry", "reference123", "Driver1234", null, null, null, null, null);

        List<DriverAuditEntity> driverAuditEntityList = Collections.singletonList(driverAuditEntity);
        when(driverAuditRepository.
                findAllActiveDriverAuditRecords(any(), any(), anyString(), eq(null))).thenReturn(driverAuditEntityList);
        Envelope<JsonObject> response = stagingdvlaQueryView.getDriverAuditRecords(jsonEnvelope);
        JsonObject jsonObject = response.payload().getJsonArray("driverAuditRecords").getJsonObject(0);
        //then
        assertThat(response.metadata().name(), is("stagingdvla.query.driver-audit-records"));
        assertThat(response.payload().getJsonArray("driverAuditRecords"), hasSize(1));

        assertThat(jsonObject.getString("date"), is(startDate.toString()));
        assertThat(jsonObject.getString("reason"), is("Case Enquiry"));
        assertThat(jsonObject.getString("reference"), is("reference123"));
        assertThat(jsonObject.getString("driverNumber"), is("Driver1234"));

    }

    @Test
    public void shouldVerifyDriverNumberAndEmailAsNullWhenPassedAsEmpty() {
        //given
        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = LocalDate.now().plusDays(1);
        final String driverNumber = "";
        final String userEmail = "";
        final DriverAuditQueryParameters driverAuditQueryParameters = new DriverAuditQueryParameters(driverNumber, userEmail, startDate.toString(), endDate.toString());
        final Envelope<DriverAuditQueryParameters> jsonEnvelope = createDriverQueryParameter(driverAuditQueryParameters);

        ArgumentCaptor<LocalDateTime> startDateCaptor = forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endDateCaptor = forClass(LocalDateTime.class);
        ArgumentCaptor<String> drivingLicenseNumberCaptor = forClass(String.class);
        ArgumentCaptor<String> userEmailCaptor = forClass(String.class);

        DriverAuditEntity driverAuditEntity = new DriverAuditEntity(randomUUID(), randomUUID(), null,
                now(), "Case Enquiry", "reference123", "Driver1234", null, null, null, null, null);

        List<DriverAuditEntity> driverAuditEntityList = Collections.singletonList(driverAuditEntity);
        //when
        when(driverAuditRepository.
                findAllActiveDriverAuditRecords(LocalDateTime.now(), LocalDateTime.now(), driverNumber, userEmail)).thenReturn(driverAuditEntityList);

        stagingdvlaQueryView.getDriverAuditRecords(jsonEnvelope);
        //then
        verify(driverAuditRepository).findAllActiveDriverAuditRecords(startDateCaptor.capture()
                , endDateCaptor.capture(),
                drivingLicenseNumberCaptor.capture(), userEmailCaptor.capture());

        assertNotNull(startDateCaptor.getValue());
        assertNotNull(startDateCaptor.getValue());
        assertNull(drivingLicenseNumberCaptor.getValue());
        assertNull(userEmailCaptor.getValue());


    }


    @Test
    public void shouldReturnDvlaAuditSearchReport() {
        //given
        final UUID userId = randomUUID();
        final UUID id = randomUUID();
        final UUID materialId = randomUUID();
        final UUID reportFileId = randomUUID();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(1);

        MetadataBuilder metadataBuilder = Envelope.metadataBuilder()
                .withName("stagingdvla.query.driver-search-audit-reports")
                .withId(randomUUID())
                .withUserId(userId.toString());

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataBuilder,
                createObjectBuilder().add("userId", userId.toString())
                        .build());

        //when
        final String reportSearchCriteria = "{\"email\":\"erica@test.hmcts.net\",\"endDate\":\"2024-01-16\",\"startDate\":\"2024-01-08\"}";

        DriverAuditReportEntity driverAuditReportEntity =
                new DriverAuditReportEntity(id, userId, now(), reportSearchCriteria, "In progress", reportFileId.toString(), materialId);

        List<DriverAuditReportEntity> driverAuditEntityList = Collections.singletonList(driverAuditReportEntity);
        when(driverAuditReportRepository.
                findByUserIdOrderByDateTimeDesc(userId)).thenReturn(driverAuditEntityList);
        Envelope<JsonObject> response = stagingdvlaQueryView.getDriverAuditSearchReports(requestEnvelope);
        JsonObject jsonObject = response.payload().getJsonArray("driverSearchAuditReports").getJsonObject(0);

        final DriverAuditReportSearchCriteria driverAuditReportSearchCriteria = DriverAuditReportSearchCriteria.driverAuditReportSearchCriteria()
                .withEmail("erica@test.hmcts.net")
                .withEndDate(startDate.toString())
                .withStartDate(endDate.toString())
                .build();

        when(jsonToObjectConverter
                .convert(stringToJsonObjectConverter.convert(driverAuditReportEntity.getReportSearchCriteria()),
                        DriverAuditReportSearchCriteria.class)).thenReturn(driverAuditReportSearchCriteria);
        //then
        assertThat(response.metadata().name(), is("stagingdvla.query.driver-search-audit-reports"));
        assertThat(response.payload().getJsonArray("driverSearchAuditReports"), hasSize(1));

        assertThat(jsonObject.getString("id"), is(id.toString()));
        assertThat(jsonObject.getString("status"), is("In progress"));
        assertThat(jsonObject.getString("reportFileId"), is(reportFileId.toString()));
        assertThat(jsonObject.getString("materialId"), is(materialId.toString()));


    }

    private Envelope<DriverAuditQueryParameters> createDriverQueryParameter(DriverAuditQueryParameters driverAuditQueryParameters) {

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(driverAuditQueryParameters)
                .withName("stagingdvla.query.driver-audit-records")
                .withMetadataFrom(requestEnvelope);
    }


    private List<DrivingConvictionRetryEntity> getDrivingConvictionRetryEntities(final int count) {
        final List<DrivingConvictionRetryEntity> entities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            DrivingConvictionRetryEntity entity = new DrivingConvictionRetryEntity();
            entity.setConvictionId(randomUUID());
            entity.setMasterDefendantId(randomUUID());
            entity.setCreatedDateTime(now());
            entities.add(entity);
        }
        return entities;
    }

    private List<DrivingConvictionRetry> convertRetryEntities(final List<DrivingConvictionRetryEntity> entities) {
        final List<DrivingConvictionRetry> modelObjects = new ArrayList<>();
        entities.forEach(entity ->
                modelObjects.add(DrivingConvictionRetry.drivingConvictionRetry()
                        .withConvictionId(entity.getConvictionId())
                        .withMasterDefendantId(entity.getMasterDefendantId())
                        .build()));
        return modelObjects;
    }
}