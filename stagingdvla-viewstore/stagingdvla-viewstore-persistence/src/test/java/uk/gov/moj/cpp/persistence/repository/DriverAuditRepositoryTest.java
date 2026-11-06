package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DriverAuditRepositoryTest {

    private static final UUID USER_ID = randomUUID();
    @Inject
    private DriverAuditRepository driverAuditRepository;

    // CHD parity port: main has no HibernateTestEntityManagerProvider; this is the same request-scoped
    // EntityManager (framework EntityManagerProducer) that the DeltaSpike repository uses in this test.
    @Inject
    private EntityManager entityManager;

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

    // CHD-02: read back in UTC on both J17 (zone UTC) and J25 (zone Z), even with the JVM zone set to Europe/London.
    @Test
    public void chd02_shouldReadAuditDateTimeBackInUtcWhateverTheJvmZone() {
        final TimeZone originalDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        try {
            final ZonedDateTime halfPastMidnightUtcInBst = ZonedDateTime.of(2026, 7, 1, 0, 30, 0, 0, ZoneOffset.UTC);
            final DriverAuditEntity audit = new DriverAuditEntity(randomUUID(), USER_ID, "user@example.com", halfPastMidnightUtcInBst,
                    "reasonType", "CaseUrn", "DLN-CHD02", "Ada", "Lovelace", "FEMALE", "AB12CD", LocalDate.of(1980, 1, 1));
            driverAuditRepository.save(audit);
            // J17 port: each DeltaSpike repository call commits (and so flushes) its own transaction; flush()
            // outside a transaction would throw TransactionRequiredException, so only clear() here.
            entityManager.clear();

            final List<DriverAuditEntity> results = driverAuditRepository.findAllActiveDriverAuditRecords(
                    LocalDateTime.of(2026, 6, 30, 0, 0), LocalDateTime.of(2026, 7, 2, 0, 0), "DLN-CHD02", null);

            assertThat(results.size(), equalTo(1));
            final ZonedDateTime readBack = results.get(0).getDateTime();
            assertThat(readBack.toInstant(), equalTo(halfPastMidnightUtcInBst.toInstant()));
            assertThat(readBack.getZone().normalized(), equalTo(ZoneOffset.UTC));
            assertThat(readBack.toLocalDate(), equalTo(LocalDate.of(2026, 7, 1)));
            assertThat(readBack.toLocalTime(), equalTo(LocalTime.of(0, 30)));
        } finally {
            TimeZone.setDefault(originalDefault);
        }
    }

    // CHD-09: the date range is inclusive at both ends (H2 only; PostgreSQL still to check).
    @Test
    public void chd09_shouldIncludeRowsExactlyOnTheStartAndEndBoundsAndExcludeOneMillisecondOutside() {
        final ZonedDateTime start = ZonedDateTime.of(2026, 3, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        final ZonedDateTime end = ZonedDateTime.of(2026, 3, 2, 17, 0, 0, 0, ZoneOffset.UTC);

        final DriverAuditEntity justBefore = anAudit("DLN-CHD09", start.minusNanos(1_000_000));
        final DriverAuditEntity onStart = anAudit("DLN-CHD09", start);
        final DriverAuditEntity onEnd = anAudit("DLN-CHD09", end);
        final DriverAuditEntity justAfter = anAudit("DLN-CHD09", end.plusNanos(1_000_000));
        driverAuditRepository.save(justBefore);
        driverAuditRepository.save(onStart);
        driverAuditRepository.save(onEnd);
        driverAuditRepository.save(justAfter);

        final List<DriverAuditEntity> results = driverAuditRepository.findAllActiveDriverAuditRecords(
                start.toLocalDateTime(), end.toLocalDateTime(), "DLN-CHD09", null);

        assertThat(results.stream().map(DriverAuditEntity::getId).sorted().toList(),
                equalTo(List.of(onStart.getId(), onEnd.getId()).stream().sorted().toList()));
    }

    private DriverAuditEntity anAudit(final String drivingLicenceNumber, final ZonedDateTime dateTime) {
        return new DriverAuditEntity(randomUUID(), USER_ID, "user@example.com", dateTime, "reasonType", "CaseUrn",
                drivingLicenceNumber, "Ada", "Lovelace", "FEMALE", "AB12CD", LocalDate.of(1980, 1, 1));
    }

    private void verifyCaseText(final DriverAuditEntity actual, final DriverAuditEntity expected) {
        assertThat(actual.getUserId(), equalTo(USER_ID));
    }
}