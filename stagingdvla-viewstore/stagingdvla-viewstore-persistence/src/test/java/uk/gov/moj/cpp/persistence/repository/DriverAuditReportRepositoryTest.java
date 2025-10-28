package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DriverAuditReportRepositoryTest {

    private static final UUID USER_ID = randomUUID();
    @Inject
    private DriverAuditReportRepository driverAuditReportRepository;

    @Test
    public void shouldSaveAndRetrieveByUserId() {

        DriverAuditReportEntity driverAuditReportEntity1 = new DriverAuditReportEntity(randomUUID(), USER_ID, ZonedDateTime.now(), "reportSearchCriteria", "status", "file_RX123RE", randomUUID());
        DriverAuditReportEntity driverAuditReportEntity2 = new DriverAuditReportEntity(randomUUID(), USER_ID, ZonedDateTime.now(), "reportSearchCriteria", "status", "file_ZX5677T", randomUUID());

        driverAuditReportRepository.save(driverAuditReportEntity1);
        driverAuditReportRepository.save(driverAuditReportEntity2);

        List<DriverAuditReportEntity> userIdList = driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(USER_ID);
        assertThat(userIdList.size(), equalTo(2));
        verifyCaseText(userIdList.get(0), driverAuditReportEntity1);
        verifyCaseText(userIdList.get(1), driverAuditReportEntity2);

    }

    private void verifyCaseText(final DriverAuditReportEntity actual, final DriverAuditReportEntity expected) {
        assertThat(actual.getUserId(), equalTo(USER_ID));
    }
}