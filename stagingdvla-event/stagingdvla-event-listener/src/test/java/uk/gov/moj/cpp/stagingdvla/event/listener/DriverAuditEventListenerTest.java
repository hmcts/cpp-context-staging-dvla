package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.cpp.stagingdvla.DriverSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.DriverSearchReason;
import uk.gov.justice.cpp.stagingdvla.event.DriverAuditRecord;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

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
public class DriverAuditEventListenerTest {

    private static final UUID ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();

    @InjectMocks
    private DriverAuditEventListener driverAuditEventListener;

    @Mock
    DriverAuditRepository driverAuditRepository;


    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<DriverAuditEntity> driverAuditEntityArgumentCaptor;


    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

    }


    @Test
    public void shouldSaveRecord_WhenDriverNotifiedNextRetryScheduled() {

        DriverSearchReason driverSearchReason = DriverSearchReason.driverSearchReason()
                .withReasonType("CE")
                .withReference("CASEURN")
                .build();

        DriverSearchCriteria driverSearchCriteria = DriverSearchCriteria.driverSearchCriteria()
                .withDrivingLicenceNumber("LICENSE-1234")
                .withDateOfBirth(LocalDate.now().toString())
                .withFirstName("PETER")
                .withLastName("PARKER")
                .withGender("MALE")
                .withPostcode("ER1 05UA")
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("id", ID.toString())
                .add("userId", USER_ID.toString())
                .add("userEmail", "peter@gmail.com")
                .add("dateTime", DATE_TIME.toString())
                .add("driverSearchReason",driverSearchReason.toString())
                .add("driverSearchCriteria",driverSearchCriteria.toString())
                .build();


        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-audit-record"),
                payload);

        final DriverAuditRecord driverAuditRecord = DriverAuditRecord
                .driverAuditRecord()
                .withId(ID)
                .withUserId(USER_ID)
                .withUserEmail("peter@gmail.com")
                .withDateTime(DATE_TIME.toString())
                .withSearchReason(driverSearchReason)
                .withSearchCriteria(driverSearchCriteria)
                .build();


        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                DriverAuditRecord.class)).thenReturn(driverAuditRecord);

        driverAuditEventListener.handleDriverAuditDetails(event);

        verify(driverAuditRepository).save(driverAuditEntityArgumentCaptor.capture());

        DriverAuditEntity driverAuditEntityArgumentCaptorValue = driverAuditEntityArgumentCaptor.getValue();

        assertThat(driverAuditEntityArgumentCaptorValue.getId(), is(ID));
        assertThat(driverAuditEntityArgumentCaptorValue.getUserId(), is(USER_ID));
        assertThat(driverAuditEntityArgumentCaptorValue.getReasonType(), is("CE"));
        assertThat(driverAuditEntityArgumentCaptorValue.getReference(), is("CASEURN"));
        assertThat(driverAuditEntityArgumentCaptorValue.getUserEmail(), is("peter@gmail.com"));
        assertThat(driverAuditEntityArgumentCaptorValue.getDrivingLicenseNumber(), is("LICENSE-1234"));
        assertThat(driverAuditEntityArgumentCaptorValue.getGender(), is("MALE"));
        assertThat(driverAuditEntityArgumentCaptorValue.getFirstNames(), is("PETER"));
        assertThat(driverAuditEntityArgumentCaptorValue.getLastName(), is("PARKER"));
        assertThat(driverAuditEntityArgumentCaptorValue.getPostcode(), is("ER1 05UA"));

        verify(driverAuditRepository).save(any(DriverAuditEntity.class));
        verifyNoMoreInteractions(driverAuditRepository);
    }

    @Test
    public void shouldSaveRecord_WhenDriverNotifiedDetailsAreIncomplete() {

        DriverSearchReason driverSearchReason = DriverSearchReason.driverSearchReason()
                .withReasonType("CE")
                .withReference("CASEURN")
                .build();

        DriverSearchCriteria driverSearchCriteria = DriverSearchCriteria.driverSearchCriteria()
                .withDrivingLicenceNumber("LICENSE-1234")
                .withDateOfBirth(LocalDate.now().toString())
                .withFirstName("PETER")
                .withLastName("PARKER")
                .withGender("MALE")
                .withPostcode("ER1 05UA")
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("id", ID.toString())
                .add("userId", USER_ID.toString())
                .add("userEmail", "peter@gmail.com")
                .add("dateTime", "")
                .add("driverSearchReason",driverSearchReason.toString())
                .add("driverSearchCriteria",driverSearchCriteria.toString())
                .build();


        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("stagingdvla.event.driver-audit-record"),
                payload);

        final DriverAuditRecord driverAuditRecord = DriverAuditRecord
                .driverAuditRecord()

                .withUserEmail("peter@gmail.com")
                .withSearchReason(driverSearchReason)
                .withSearchCriteria(driverSearchCriteria)
                .build();


        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                DriverAuditRecord.class)).thenReturn(driverAuditRecord);

        driverAuditEventListener.handleDriverAuditDetails(event);

        verify(driverAuditRepository).save(driverAuditEntityArgumentCaptor.capture());

        DriverAuditEntity driverAuditEntityArgumentCaptorValue = driverAuditEntityArgumentCaptor.getValue();

        assertThat(driverAuditEntityArgumentCaptorValue.getReasonType(), is("CE"));
        assertThat(driverAuditEntityArgumentCaptorValue.getReference(), is("CASEURN"));
        assertThat(driverAuditEntityArgumentCaptorValue.getUserEmail(), is("peter@gmail.com"));
        assertThat(driverAuditEntityArgumentCaptorValue.getDrivingLicenseNumber(), is("LICENSE-1234"));
        assertThat(driverAuditEntityArgumentCaptorValue.getGender(), is("MALE"));
        assertThat(driverAuditEntityArgumentCaptorValue.getFirstNames(), is("PETER"));
        assertThat(driverAuditEntityArgumentCaptorValue.getLastName(), is("PARKER"));
        assertThat(driverAuditEntityArgumentCaptorValue.getPostcode(), is("ER1 05UA"));

        verify(driverAuditRepository).save(any(DriverAuditEntity.class));
        verifyNoMoreInteractions(driverAuditRepository);
    }
}
