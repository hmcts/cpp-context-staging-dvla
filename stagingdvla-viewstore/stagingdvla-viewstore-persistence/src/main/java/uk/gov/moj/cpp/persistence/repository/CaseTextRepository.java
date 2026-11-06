package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.CaseTextEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseTextRepository {

    @PersistenceContext(unitName = "stagingdvla-persistence-unit")
    EntityManager entityManager;

    public CaseTextEntity save(final CaseTextEntity caseTextEntity) {
        return entityManager.merge(caseTextEntity);
    }

    public List<CaseTextEntity> findByCaseIdOrderByCreatedDateTimeDesc(final UUID caseId) {
        return entityManager.createQuery(
                        "SELECT c FROM CaseTextEntity c WHERE c.caseId = :caseId ORDER BY c.createdDateTime DESC", CaseTextEntity.class)
                .setParameter("caseId", caseId)
                .getResultList();
    }
}
