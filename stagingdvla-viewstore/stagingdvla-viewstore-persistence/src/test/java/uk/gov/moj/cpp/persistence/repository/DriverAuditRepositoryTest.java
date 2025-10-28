package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DriverAuditRepositoryTest {

    private static final UUID USER_ID = randomUUID();
    @Inject
    private DriverAuditRepository driverAuditRepository;

    @Test
    public void shouldSaveAndRetrieveByUserId() {

        DriverAuditEntity driverAuditEntity1 = new DriverAuditEntity(randomUUID(), USER_ID, "user@gmail.com", ZonedDateTime.now(), "reasonType", "CaseUrn", "TX456789", "peter", "parker", "MALE", "CR01hb", LocalDate.now());
        DriverAuditEntity driverAuditEntity2 = new DriverAuditEntity(randomUUID(), USER_ID, "user@gmail.com", ZonedDateTime.now(), "reasonType", "CaseUrn", "TX456789", "peter", "parker", "MALE", "CR01hb", LocalDate.now());

        driverAuditRepository.save(driverAuditEntity1);
        driverAuditRepository.save(driverAuditEntity2);

        List<DriverAuditEntity> userIdList = driverAuditRepository.findByUserId(USER_ID);
        assertThat(userIdList.size(), equalTo(2));
        verifyCaseText(userIdList.get(0), driverAuditEntity1);
        verifyCaseText(userIdList.get(1), driverAuditEntity2);

    }

    private void verifyCaseText(final DriverAuditEntity actual, final DriverAuditEntity expected) {
        assertThat(actual.getUserId(), equalTo(USER_ID));
    }
}