package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
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

    private DriverAuditReportEntity aReport(final UUID materialId) {
        return new DriverAuditReportEntity(randomUUID(), USER_ID, new UtcClock().now(), "reportSearchCriteria", "status", "file_" + materialId, materialId);
    }
}
