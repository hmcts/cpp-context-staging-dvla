package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportStored;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeletionFailed;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuditReportAggregateTest {

    private static final UUID REPORT_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String REPORT_FILE_ID = "REPORT_FILE_ID";

    @InjectMocks
    private AuditReportAggregate aggregate;


    @Before
    public void setUp() {
        aggregate = new AuditReportAggregate();
    }

    @Test
    public void shouldRequestGenerateDriverSearchAuditReport() {
        final DriverAuditReportSearchCriteria driverAuditReportSearchCriteria = DriverAuditReportSearchCriteria.driverAuditReportSearchCriteria()
                .withStartDate(LocalDate.now().minusDays(1).toString())
                .withEmail("peter@gmail.com")
                .withEndDate(LocalDate.now().toString())
                .withDriverNumber("DRIVER1234")
                .build();

        final DriverSearchAuditReportRequested reportRequested = DriverSearchAuditReportRequested.driverSearchAuditReportRequested()
                .withId(REPORT_ID)
                .withDateTime(ZonedDateTime.now().toString())
                .withUserId(USER_ID)
                .withReportSearchCriteria(driverAuditReportSearchCriteria)
                .build();

        aggregate.apply(reportRequested);
        final List<Object> eventStream = aggregate.generateAuditReport(REPORT_ID, USER_ID, ZonedDateTime.now().toString(), driverAuditReportSearchCriteria).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportRequested.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportRequested.class)));
    }

    @Test
    public void shouldCreateDriverSearchAuditReport() {
        final DriverSearchAuditReportCreated reportCreatedEvent = DriverSearchAuditReportCreated.driverSearchAuditReportCreated()
                .withId(REPORT_ID)
                .withMaterialId(MATERIAL_ID)
                .withReportFileId(REPORT_FILE_ID)
                .build();

        final DriverRecordSearchAuditReportCreated reportCreatedCommand = DriverRecordSearchAuditReportCreated.driverRecordSearchAuditReportCreated()
                .withId(REPORT_ID)
                .withReportFileId(REPORT_FILE_ID).build();

        aggregate.apply(reportCreatedEvent);
        final List<Object> eventStream = aggregate.auditReportCreated(reportCreatedCommand).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportCreated.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportCreated.class)));
    }

    @Test
    public void shouldStoreDriverSearchAuditReport() {
        final DriverSearchAuditReportStored reportStoredEvent = DriverSearchAuditReportStored.driverSearchAuditReportStored()
                .withId(REPORT_ID)
                .withMaterialId(MATERIAL_ID)
                .build();

        final DriverRecordSearchAuditReportStored reportStoredCommand = DriverRecordSearchAuditReportStored.driverRecordSearchAuditReportStored()
                .withId(REPORT_ID)
                .withMaterialId(MATERIAL_ID).build();

        aggregate.apply(reportStoredEvent);
        final List<Object> eventStream = aggregate.auditReportStored(reportStoredCommand).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportStored.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportStored.class)));
    }

    @Test
    public void shouldRaiseDriverSearchAuditReportDeletionFailed() {

        final DriverSearchAuditReportDeletionFailed deletionFailed = DriverSearchAuditReportDeletionFailed.driverSearchAuditReportDeletionFailed()
                .withId(REPORT_ID)
                .withMaterialId(MATERIAL_ID)
                .build();

        aggregate.apply(deletionFailed);
        aggregate.setReportCreatedByMap(REPORT_ID, randomUUID());
        final List<Object> eventStream = aggregate.deleteAuditReport(REPORT_ID, USER_ID, MATERIAL_ID).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportDeletionFailed.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportDeletionFailed.class)));
    }

    @Test
    public void shouldDeleteDriverSearchAuditReport() {
        final DriverSearchAuditReportDeleted reportDeleted = DriverSearchAuditReportDeleted.driverSearchAuditReportDeleted()
                .withId(REPORT_ID)
                .withMaterialId(MATERIAL_ID)
                .build();

        aggregate.apply(reportDeleted);
        aggregate.setReportCreatedByMap(REPORT_ID, USER_ID);
        final List<Object> eventStream = aggregate.deleteAuditReport(REPORT_ID, USER_ID, MATERIAL_ID).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportDeleted.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(DriverSearchAuditReportDeleted.class)));
    }
}
