package uk.gov.moj.cpp.stagingdvla.event.listener;

import static com.google.common.io.Resources.getResource;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditReportRepository;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DriverAuditReportStatus;

import java.io.IOException;
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


@ExtendWith(MockitoExtension.class)
public class DriverSearchAuditReportEventListenerTest {

    private static final String ID = "a7e6d378-8cf1-11eb-8dcd-0242ac130003";
    private static final String USER_ID = "b3454b32-8cf1-11eb-8dcd-0242ac130003";

    @InjectMocks
    private DriverSearchAuditReportEventListener driverSearchAuditReportEventListener;

    @Mock
    private DriverAuditReportRepository driverAuditReportRepository;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<DriverAuditReportEntity> driverAuditReportEntityArgumentCaptor;


    public static <T> T readJson(final String jsonPath, final Class<T> clazz) {
        try {
            final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();
            return OBJECT_MAPPER.readValue(getResource(jsonPath), clazz);
        } catch (final IOException e) {
            throw new IllegalStateException("Resource " + jsonPath + " inaccessible: " + e.getMessage());
        }
    }

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreateRecordWhenDriverAuditReportRequested() {
        final JsonObject jsonObject = readJson("auditReportRequested.json", JsonObject.class);
        final DriverSearchAuditReportRequested auditReportRequestedEvent = jsonObjectToObjectConverter.convert(jsonObject, DriverSearchAuditReportRequested.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-requested"),
                objectToJsonObjectConverter.convert(auditReportRequestedEvent));

        driverSearchAuditReportEventListener.handleDriverSearchAuditReportRequested(jsonEnvelope);

        verify(driverAuditReportRepository).save(driverAuditReportEntityArgumentCaptor.capture());

        DriverAuditReportEntity driverAuditReportEntityArgumentCaptorValue = driverAuditReportEntityArgumentCaptor.getValue();

        assertThat(driverAuditReportEntityArgumentCaptorValue.getId().toString(), is(ID));
        assertThat(driverAuditReportEntityArgumentCaptorValue.getUserId().toString(), is(USER_ID));
        assertThat(driverAuditReportEntityArgumentCaptorValue.getStatus(), is(DriverAuditReportStatus.PENDING.getStatus()));
        verify(driverAuditReportRepository, times(1)).save(any(DriverAuditReportEntity.class));
    }

    @Test
    public void shouldUpdateRecordWhenDriverSearchAuditReportStored() {
        final JsonObject jsonObject = readJson("auditReport.json", JsonObject.class);
        final DriverSearchAuditReportStored event = jsonObjectToObjectConverter.convert(jsonObject, DriverSearchAuditReportStored.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-stored"),
                objectToJsonObjectConverter.convert(event));
        when(this.driverAuditReportRepository.findBy(event.getId())).thenReturn(DriverAuditReportEntity.DriverAuditReportEntityBuilder.builder()
                .withId(event.getId()).withUserId(UUID.fromString((USER_ID))).build());

        driverSearchAuditReportEventListener.handleDriverSearchAuditReportStored(jsonEnvelope);

        verify(driverAuditReportRepository).save(driverAuditReportEntityArgumentCaptor.capture());

        DriverAuditReportEntity driverAuditReportEntityArgumentCaptorValue = driverAuditReportEntityArgumentCaptor.getValue();

        assertThat(driverAuditReportEntityArgumentCaptorValue.getId().toString(), is(ID));
        assertThat(driverAuditReportEntityArgumentCaptorValue.getStatus(), is(DriverAuditReportStatus.COMPLETED.getStatus()));
        verify(driverAuditReportRepository, times(1)).save(any(DriverAuditReportEntity.class));
    }

    @Test
    public void shouldRemoveRecordWhenDriverSearchAuditReportDeleted() {
        final JsonObject jsonObject = readJson("auditReport.json", JsonObject.class);
        final DriverSearchAuditReportDeleted event = jsonObjectToObjectConverter.convert(jsonObject, DriverSearchAuditReportDeleted.class);
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(metadataWithRandomUUID("stagingdvla.event.driver-search-audit-report-deleted"),
                objectToJsonObjectConverter.convert(event));
        when(this.driverAuditReportRepository.findBy(event.getId())).thenReturn(DriverAuditReportEntity.DriverAuditReportEntityBuilder.builder()
                .withId(event.getId()).withUserId(UUID.fromString((USER_ID))).build());

        driverSearchAuditReportEventListener.handleDriverSearchAuditReportDeleted(jsonEnvelope);
        verify(driverAuditReportRepository, times(1)).remove(any(DriverAuditReportEntity.class));
    }

}
