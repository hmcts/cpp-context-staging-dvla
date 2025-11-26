package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

        final UUID userId = randomUUID();

        DriverAuditEntity driverAuditEntity1 = new DriverAuditEntity(randomUUID(), userId, "user@gmail.com", ZonedDateTime.now(), "reasonType", "CaseUrn", "TX456789", "peter", "parker", "MALE", "CR01hb", LocalDate.now());
        DriverAuditEntity driverAuditEntity2 = new DriverAuditEntity(randomUUID(), userId, "user@gmail.com", ZonedDateTime.now(), "reasonType", "CaseUrn", "TX456789", "peter", "parker", "MALE", "CR01hb", LocalDate.now());

        driverAuditRepository.save(driverAuditEntity1);
        driverAuditRepository.save(driverAuditEntity2);

        List<DriverAuditEntity> userIdList = driverAuditRepository.findByUserId(userId);
        assertThat(userIdList.size(), equalTo(2));
        assertThat(userIdList.get(0).getUserId(), equalTo(userId));
        assertThat(userIdList.get(1).getUserId(), equalTo(userId));

    }

    @Test
    public void shouldFindAllActiveDriverAuditRecords_withNullFilters_returnsAllWithinDateRangeInclusive() {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime start = now.minusDays(10);
        final ZonedDateTime end = now.minusDays(5);

        final DriverAuditEntity beforeRange = new DriverAuditEntity(randomUUID(), USER_ID, "user1@example.com", start.minusMinutes(1), "reasonType", "CaseUrn", "DLN-1", "John", "Doe", "MALE", "AB12CD", LocalDate.now());
        final DriverAuditEntity atStart = new DriverAuditEntity(randomUUID(), USER_ID, "user2@example.com", start.plusHours(2), "reasonType", "CaseUrn", "DLN-2", "Jane", "Roe", "FEMALE", "EF34GH", LocalDate.now());
        final DriverAuditEntity inRange = new DriverAuditEntity(randomUUID(), USER_ID, "user3@example.com", now, "reasonType", "CaseUrn", "DLN-3", "Max", "Payne", "MALE", "IJ56KL", LocalDate.now());
        final DriverAuditEntity atEnd = new DriverAuditEntity(randomUUID(), USER_ID, "user4@example.com", end, "reasonType", "CaseUrn", "DLN-4", "Lara", "Croft", "FEMALE", "MN78OP", LocalDate.now());
        final DriverAuditEntity afterRange = new DriverAuditEntity(randomUUID(), USER_ID, "user5@example.com", end.plusHours(2), "reasonType", "CaseUrn", "DLN-5", "Alan", "Wake", "MALE", "QR90ST", LocalDate.now());

        driverAuditRepository.save(beforeRange);
        driverAuditRepository.save(atStart);
        driverAuditRepository.save(inRange);
        driverAuditRepository.save(atEnd);
        driverAuditRepository.save(afterRange);

        final List<DriverAuditEntity> results = driverAuditRepository.findAllActiveDriverAuditRecords(
                start.toLocalDateTime(), end.toLocalDateTime(), null, null);

        assertThat(results.size(), equalTo(2));
        // Expect atStart, inRange, atEnd
        assertThat(results.stream().anyMatch(e -> e.getId().equals(atStart.getId())), equalTo(true));
        assertThat(results.stream().anyMatch(e -> e.getId().equals(inRange.getId())), equalTo(false));
        assertThat(results.stream().anyMatch(e -> e.getId().equals(atEnd.getId())), equalTo(true));
    }

    @Test
    public void shouldFilterByDrivingLicenseNumber_whenProvided() {
        final ZonedDateTime now = ZonedDateTime.now();
        final LocalDateTime start = now.minusDays(1).toLocalDateTime();
        final LocalDateTime end = now.plusDays(1).toLocalDateTime();

        final DriverAuditEntity matchDln = new DriverAuditEntity(randomUUID(), USER_ID, "user@example.com", now, "reasonType", "CaseUrn", "ABC123", "Amy", "Pond", "FEMALE", "ZZ11ZZ", LocalDate.now());
        final DriverAuditEntity nonMatchDln = new DriverAuditEntity(randomUUID(), USER_ID, "user@example.com", now, "reasonType", "CaseUrn", "XYZ999", "Rory", "Williams", "MALE", "YY22YY", LocalDate.now());

        driverAuditRepository.save(matchDln);
        driverAuditRepository.save(nonMatchDln);

        final List<DriverAuditEntity> results = driverAuditRepository.findAllActiveDriverAuditRecords(
                start, end, "ABC123", null);

        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0).getId(), equalTo(matchDln.getId()));
    }

    @Test
    public void shouldFilterByUserEmail_caseInsensitive_whenProvided() {
        final ZonedDateTime now = ZonedDateTime.now();
        final LocalDateTime start = now.minusDays(1).toLocalDateTime();
        final LocalDateTime end = now.plusDays(1).toLocalDateTime();

        final DriverAuditEntity emailLower = new DriverAuditEntity(randomUUID(), USER_ID, "tester@example.com", now, "reasonType", "CaseUrn", "DLN-100", "Billie", "Jean", "FEMALE", "AB10CD", LocalDate.now());
        final DriverAuditEntity emailUpper = new DriverAuditEntity(randomUUID(), USER_ID, "TESTER@EXAMPLE.COM", now, "reasonType", "CaseUrn", "DLN-101", "Michael", "Jackson", "MALE", "EF10GH", LocalDate.now());
        final DriverAuditEntity emailCamel = new DriverAuditEntity(randomUUID(), USER_ID, "Tester@Example.COM", now, "reasonType", "CaseUrn", "DLN-101", "Michael", "Jackson", "MALE", "EF10GH", LocalDate.now());
        final DriverAuditEntity otherEmail = new DriverAuditEntity(randomUUID(), USER_ID, "other@example.com", now, "reasonType", "CaseUrn", "DLN-102", "Paul", "Smith", "MALE", "IJ10KL", LocalDate.now());

        driverAuditRepository.save(emailLower);
        driverAuditRepository.save(emailUpper);
        driverAuditRepository.save(emailCamel);
        driverAuditRepository.save(otherEmail);

        final List<DriverAuditEntity> resultsLower = driverAuditRepository.findAllActiveDriverAuditRecords(
                start, end, null, "tester@example.com");
        assertThat(resultsLower.size(), equalTo(3));

        final List<DriverAuditEntity> resultsUpper = driverAuditRepository.findAllActiveDriverAuditRecords(
                start, end, null, "TESTER@EXAMPLE.COM");
        assertThat(resultsUpper.size(), equalTo(3));
    }

    @Test
    public void shouldApplyBothFilters_whenBothProvided() {
        final ZonedDateTime now = ZonedDateTime.now();
        final LocalDateTime start = now.minusDays(1).toLocalDateTime();
        final LocalDateTime end = now.plusDays(1).toLocalDateTime();

        final DriverAuditEntity matchBoth = new DriverAuditEntity(randomUUID(), USER_ID, "combo@example.com", now, "reasonType", "CaseUrn", "DLN-777", "Ada", "Lovelace", "FEMALE", "PC11AA", LocalDate.now());
        final DriverAuditEntity matchEmailOnly = new DriverAuditEntity(randomUUID(), USER_ID, "combo@example.com", now, "reasonType", "CaseUrn", "DLN-888", "Grace", "Hopper", "FEMALE", "PC11AB", LocalDate.now());
        final DriverAuditEntity matchDlnOnly = new DriverAuditEntity(randomUUID(), USER_ID, "someone@example.com", now, "reasonType", "CaseUrn", "DLN-777", "Alan", "Turing", "MALE", "PC11AC", LocalDate.now());

        driverAuditRepository.save(matchBoth);
        driverAuditRepository.save(matchEmailOnly);
        driverAuditRepository.save(matchDlnOnly);

        final List<DriverAuditEntity> results = driverAuditRepository.findAllActiveDriverAuditRecords(
                start, end, "DLN-777", "combo@example.com");

        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0).getId(), equalTo(matchBoth.getId()));
    }

    private void verifyCaseText(final DriverAuditEntity actual, final DriverAuditEntity expected) {
        assertThat(actual.getUserId(), equalTo(USER_ID));
    }
}