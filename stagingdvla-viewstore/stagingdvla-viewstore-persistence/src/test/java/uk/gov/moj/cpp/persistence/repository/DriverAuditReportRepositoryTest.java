package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DriverAuditReportRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingdvla-test-persistence-unit";
    private static final UUID USER_ID = randomUUID();

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DriverAuditReportRepository driverAuditReportRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        driverAuditReportRepository = new DriverAuditReportRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(driverAuditReportRepository);
    }

    @Test
    public void shouldSaveAndRetrieveByUserId() {

        final DriverAuditReportEntity driverAuditReportEntity1 = driverAuditReportRepository.save(aReport(randomUUID()));
        final DriverAuditReportEntity driverAuditReportEntity2 = driverAuditReportRepository.save(aReport(randomUUID()));

        final List<DriverAuditReportEntity> userIdList = driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(USER_ID);
        assertThat(userIdList.size(), equalTo(2));
        assertThat(userIdList.get(0).getUserId(), equalTo(USER_ID));
        assertThat(userIdList.get(1).getUserId(), equalTo(USER_ID));
        assertThat(userIdList, org.hamcrest.Matchers.containsInAnyOrder(driverAuditReportEntity1, driverAuditReportEntity2));
    }

    @Test
    public void shouldFindById() {
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(randomUUID()));

        assertThat(driverAuditReportRepository.findBy(saved.getId()), is(saved));
    }

    @Test
    public void shouldFindByIdAndUserIdAndMaterialId() {
        final UUID materialId = randomUUID();
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(materialId));

        assertThat(driverAuditReportRepository.findByIdAndUserIdAndMaterialId(saved.getId(), USER_ID, materialId), is(saved));
    }

    @Test
    public void shouldReturnNullWhenNotFoundByIdAndUserIdAndMaterialId() {
        driverAuditReportRepository.save(aReport(randomUUID()));

        assertThat(driverAuditReportRepository.findByIdAndUserIdAndMaterialId(randomUUID(), randomUUID(), randomUUID()), is(nullValue()));
    }

    @Test
    public void shouldRemove() {
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(randomUUID()));

        driverAuditReportRepository.remove(saved);

        assertThat(driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(USER_ID).size(), is(0));
    }

    @Test
    public void shouldRemoveADetachedReportByMergingItFirst() {
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(randomUUID()));
        // a fresh instance with the same id is not managed, so remove() takes the merge branch
        final DriverAuditReportEntity detached = new DriverAuditReportEntity(saved.getId(), USER_ID, new UtcClock().now(), "reportSearchCriteria", "status", "file", randomUUID());

        driverAuditReportRepository.remove(detached);

        assertThat(driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(USER_ID).size(), is(0));
    }

    /*
     * CHD-01 (BC-01) - ACCEPTED divergence, so this pins the Java 25 contract rather than Java 17.
     * Java 17 (DeltaSpike findBy..., SingleResultType.JPA) threw NoResultException when nothing matched.
     * The team accepted the Java 25 behaviour: null, which StagingdvlaQueryView turns into a 403.
     */
    @Test
    public void chd01_shouldReturnNullWhenReportBelongsToAnotherUserOrMaterial_acceptedJava25Contract() {
        final UUID materialId = randomUUID();
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(materialId));

        assertThat(driverAuditReportRepository.findByIdAndUserIdAndMaterialId(saved.getId(), randomUUID(), materialId), is(nullValue()));
        assertThat(driverAuditReportRepository.findByIdAndUserIdAndMaterialId(saved.getId(), USER_ID, randomUUID()), is(nullValue()));
    }

    /*
     * CHD-02 - parity: Java 17 and Java 25 both read searchDateTime back in UTC, whatever the JVM default zone is.
     * Java 17: the framework's EntityManagerProducer (persistence-deltaspike 17.104.0) calls TimeZone.setDefault(UTC)
     * when the EntityManager is created, and Hibernate 5 then hydrates in ZoneId.systemDefault() -> zone "UTC".
     * Java 25: Hibernate 6 with hibernate.jdbc.time_zone=UTC (TIMESTAMP_UTC) -> zone "Z".
     * Only the zone label differs (UTC vs Z); the instant and the local date/time are identical, and nothing prints
     * the label (StagingdvlaQueryView uses toLocalDate()/toLocalTime()). The test sets Europe/London first to prove
     * the read does not follow the JVM zone.
     */
    @Test
    public void chd02_shouldReadSearchDateTimeBackInUtcWhateverTheJvmZone() {
        final TimeZone originalDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"));
        try {
            final ZonedDateTime halfPastMidnightUtcInBst = ZonedDateTime.of(2026, 7, 1, 0, 30, 0, 0, ZoneOffset.UTC);
            final DriverAuditReportEntity report = new DriverAuditReportEntity(randomUUID(), USER_ID, halfPastMidnightUtcInBst,
                    "reportSearchCriteria", "status", "file", randomUUID());
            driverAuditReportRepository.save(report);
            flushAndClear();

            final ZonedDateTime readBack = driverAuditReportRepository.findBy(report.getId()).getDateTime();

            assertThat(readBack.toInstant(), is(halfPastMidnightUtcInBst.toInstant()));
            assertThat(readBack.getZone().normalized(), is(ZoneOffset.UTC));
            assertThat(readBack.toLocalDateTime(), is(LocalDateTime.of(2026, 7, 1, 0, 30)));
        } finally {
            TimeZone.setDefault(originalDefault);
        }
    }

    /*
     * CHD-13 - Java 17 behaviour: DeltaSpike save() called persist() for an entity whose id had no row yet
     * (isNew = !em.contains(entity) && COUNT(id) == 0), which makes the instance passed in managed.
     * The Java 25 repository always merges, which returns a managed copy and leaves the argument detached.
     */
    /*
     * CHD-13 ACCEPTED - disabled; passes on JDK 17, fails on JDK 25 as expected.
     * Java 17 DeltaSpike save() persisted a new entity, so the instance passed in became managed. Java 25 save() always
     * merges and returns a managed copy, leaving the argument unmanaged. The row written is identical. It would only matter
     * if a caller changed the passed instance after save() and expected that to be written; neither production caller
     * (DriverSearchAuditReportEventListener lines 46 and 59) does: the requested handler saves as its last statement, and
     * the stored handler saves an entity it loaded with findBy(), which merge() returns as the same instance.
     */
    @Disabled("CHD-13 accepted: J25 save() merges, so the passed instance stays unmanaged; no caller relies on it; see comment")
    @Test
    public void chd13_shouldMakeThePassedInstanceManagedWhenSavingANewReport() {
        final DriverAuditReportEntity newReport = aReport(randomUUID());

        driverAuditReportRepository.save(newReport);

        assertThat(entityManager().contains(newReport), is(true));
    }

    /*
     * CHD-13 - Java 17 behaviour: saving an instance whose id already has a row merged it, so the row is updated.
     */
    @Test
    public void chd13_shouldUpdateTheExistingRowWhenSavingAnInstanceWithAnExistingId() {
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(randomUUID()));
        flushAndClear();

        final DriverAuditReportEntity sameId = new DriverAuditReportEntity(saved.getId(), USER_ID, saved.getDateTime(),
                "reportSearchCriteria", "STORED", saved.getReportFileId(), saved.getMaterialId());
        driverAuditReportRepository.save(sameId);
        flushAndClear();

        assertThat(driverAuditReportRepository.findBy(saved.getId()).getStatus(), is("STORED"));
        assertThat(driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(USER_ID).size(), is(1));
    }

    /*
     * CHD-14 - Java 17 behaviour: DeltaSpike remove() called em.remove(entity) directly, and JPA rejects a
     * detached instance with IllegalArgumentException. The Java 25 repository merges it first and deletes the row.
     */
    /*
     * CHD-14 ACCEPTED - disabled; the JDK 17 run showed DeltaSpike wrapped the IllegalArgumentException in
     * QueryInvocationException, and Java 25 merges the detached instance and deletes the row instead of failing.
     * The only production caller (DriverSearchAuditReportEventListener.handleDriverSearchAuditReportDeleted) removes the
     * managed entity it has just loaded with findBy() in the same transaction, so the detached path is never reached.
     * The Java 25 behaviour is pinned by shouldRemoveADetachedReportByMergingItFirst.
     */
    @Disabled("CHD-14 accepted: J25 deletes a detached entity instead of throwing; no caller passes one; see comment")
    @Test
    public void chd14_shouldThrowIllegalArgumentExceptionWhenRemovingADetachedReport() {
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(randomUUID()));
        flushAndClear();
        final DriverAuditReportEntity detached = new DriverAuditReportEntity(saved.getId(), USER_ID, saved.getDateTime(),
                "reportSearchCriteria", "status", saved.getReportFileId(), saved.getMaterialId());

        assertThrows(IllegalArgumentException.class, () -> driverAuditReportRepository.remove(detached));
    }

    /*
     * CHD-14 - Java 17 behaviour: removing null (a driver-search-audit-report-deleted event for a report that
     * is already gone) failed with IllegalArgumentException.
     * The JDK 17 run showed DeltaSpike wrapped it in QueryInvocationException; Java 25 throws the bare
     * IllegalArgumentException. Both fail the event; only the exception class differs (CHD-14, accepted).
     */
    @Test
    public void chd14_shouldThrowIllegalArgumentExceptionWhenRemovingNull() {
        assertThrows(IllegalArgumentException.class, () -> driverAuditReportRepository.remove(null));
    }

    private EntityManager entityManager() {
        return hibernateTestEntityManagerProvider.getEntityManager();
    }

    private void flushAndClear() {
        entityManager().flush();
        entityManager().clear();
    }

    private DriverAuditReportEntity aReport(final UUID materialId) {
        return new DriverAuditReportEntity(randomUUID(), USER_ID, new UtcClock().now(), "reportSearchCriteria", "status", "file_" + materialId, materialId);
    }
}
