package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.cpp.stagingdvla.DriverSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.DriverSearchReason;
import uk.gov.justice.cpp.stagingdvla.event.DriverAuditRecord;

import java.time.ZonedDateTime;
import java.util.List;

import org.hamcrest.CoreMatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverAuditAggregateTest {

    @InjectMocks
    private DriverAuditAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new DriverAuditAggregate();
    }

    @Test
    public void shouldCreateDriverAuditRecord() {
        final DriverAuditRecord driverAuditRecord = DriverAuditRecord.driverAuditRecord()
                .withId(randomUUID())
                .withDateTime(ZonedDateTime.now().toString())
                .withUserEmail("peter@gmail.com")
                .withSearchReason(DriverSearchReason.driverSearchReason().build())
                .withSearchCriteria(DriverSearchCriteria.driverSearchCriteria().build())
                .build();

        aggregate.apply(driverAuditRecord);
        final List<Object> eventStream = aggregate.createAudit(driverAuditRecord).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DriverAuditRecord.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(DriverAuditRecord.class)));
    }
}
