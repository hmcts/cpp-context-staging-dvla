package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.common.util.UtcClock;

import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DriverAuditReportRepositoryTest {

    private static final UUID USER_ID = randomUUID();
    // J17 port: rows commit here (no per-test rollback), so the CHD tests use their own user id and don't change
    // the counts shouldSaveAndRetrieveByUserId sees for USER_ID.
    private static final UUID CHD_USER_ID = randomUUID();
    @Inject
    private DriverAuditReportRepository driverAuditReportRepository;

    // CHD parity port: main has no HibernateTestEntityManagerProvider; this is the same request-scoped
    // EntityManager (framework EntityManagerProducer) that the DeltaSpike repository uses in this test.
    @Inject
    private EntityManager entityManager;

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

    // CHD-01 (accepted): J17 threw NoResultException; J25 returns null, which the query view turns into a 403.
    @Test
    public void chd01_shouldReturnNullWhenReportBelongsToAnotherUserOrMaterial_acceptedJava25Contract() {
        final UUID materialId = randomUUID();
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(aReport(materialId));

        assertThat(driverAuditReportRepository.findByIdAndUserIdAndMaterialId(saved.getId(), randomUUID(), materialId), is(nullValue()));
        assertThat(driverAuditReportRepository.findByIdAndUserIdAndMaterialId(saved.getId(), CHD_USER_ID, randomUUID()), is(nullValue()));
    }

    // CHD-13: saving an instance whose id already has a row updates that row.
    @Test
    public void chd13_shouldUpdateTheExistingRowWhenSavingAnInstanceWithAnExistingId() {
        final UUID userId = randomUUID();
        final DriverAuditReportEntity saved = driverAuditReportRepository.save(
                new DriverAuditReportEntity(randomUUID(), userId, new UtcClock().now(), "reportSearchCriteria", "status", "file", randomUUID()));
        flushAndClear();

        final DriverAuditReportEntity sameId = new DriverAuditReportEntity(saved.getId(), userId, saved.getDateTime(),
                "reportSearchCriteria", "STORED", saved.getReportFileId(), saved.getMaterialId());
        driverAuditReportRepository.save(sameId);
        flushAndClear();

        assertThat(driverAuditReportRepository.findBy(saved.getId()).getStatus(), is("STORED"));
        assertThat(driverAuditReportRepository.findByUserIdOrderByDateTimeDesc(userId).size(), is(1));
    }

    // J17 port: each DeltaSpike repository call commits (and so flushes) its own transaction; flush() outside a
    // transaction would throw TransactionRequiredException, so only clear() the persistence context here.
    private void flushAndClear() {
        entityManager.clear();
    }

    private DriverAuditReportEntity aReport(final UUID materialId) {
        return new DriverAuditReportEntity(randomUUID(), CHD_USER_ID, new UtcClock().now(), "reportSearchCriteria", "status", "file_" + materialId, materialId);
    }

    private void verifyCaseText(final DriverAuditReportEntity actual, final DriverAuditReportEntity expected) {
        assertThat(actual.getUserId(), equalTo(USER_ID));
    }
}