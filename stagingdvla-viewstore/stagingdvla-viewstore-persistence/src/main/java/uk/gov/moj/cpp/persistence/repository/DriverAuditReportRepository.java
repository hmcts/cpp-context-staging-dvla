package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DriverAuditReportRepository {

    @PersistenceContext(unitName = "stagingdvla-persistence-unit")
    EntityManager entityManager;

    public DriverAuditReportEntity save(final DriverAuditReportEntity driverAuditReportEntity) {
        return entityManager.merge(driverAuditReportEntity);
    }

    public DriverAuditReportEntity findBy(final UUID id) {
        return entityManager.find(DriverAuditReportEntity.class, id);
    }

    public void remove(final DriverAuditReportEntity driverAuditReportEntity) {
        entityManager.remove(entityManager.contains(driverAuditReportEntity)
                ? driverAuditReportEntity
                : entityManager.merge(driverAuditReportEntity));
    }

    public List<DriverAuditReportEntity> findByUserIdOrderByDateTimeDesc(final UUID userId) {
        return entityManager.createQuery(
                        "SELECT dar FROM DriverAuditReportEntity dar WHERE dar.userId = :userId ORDER BY dar.dateTime DESC", DriverAuditReportEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public DriverAuditReportEntity findByIdAndUserIdAndMaterialId(final UUID id, final UUID userId, final UUID materialId) {
        return entityManager.createQuery(
                        "SELECT dar FROM DriverAuditReportEntity dar WHERE dar.id = :id AND dar.userId = :userId AND dar.materialId = :materialId", DriverAuditReportEntity.class)
                .setParameter("id", id)
                .setParameter("userId", userId)
                .setParameter("materialId", materialId)
                .getResultStream()
                .findFirst()
                .orElse(null);
    }
}
