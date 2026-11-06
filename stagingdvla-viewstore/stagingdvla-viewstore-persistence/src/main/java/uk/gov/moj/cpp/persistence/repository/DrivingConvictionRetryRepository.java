package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DrivingConvictionRetryRepository {

    @PersistenceContext(unitName = "stagingdvla-persistence-unit")
    EntityManager entityManager;

    public DrivingConvictionRetryEntity save(final DrivingConvictionRetryEntity drivingConvictionRetryEntity) {
        return entityManager.merge(drivingConvictionRetryEntity);
    }

    public DrivingConvictionRetryEntity findBy(final UUID id) {
        return entityManager.find(DrivingConvictionRetryEntity.class, id);
    }

    public List<DrivingConvictionRetryEntity> findAll() {
        return entityManager.createQuery(
                "SELECT d FROM DrivingConvictionRetryEntity d", DrivingConvictionRetryEntity.class).getResultList();
    }

    public List<DrivingConvictionRetryEntity> findAll(final int start, final int max) {
        return entityManager.createQuery(
                        "SELECT d FROM DrivingConvictionRetryEntity d", DrivingConvictionRetryEntity.class)
                .setFirstResult(start)
                .setMaxResults(max)
                .getResultList();
    }

    public void remove(final DrivingConvictionRetryEntity drivingConvictionRetryEntity) {
        entityManager.remove(entityManager.contains(drivingConvictionRetryEntity)
                ? drivingConvictionRetryEntity
                : entityManager.merge(drivingConvictionRetryEntity));
    }
}
