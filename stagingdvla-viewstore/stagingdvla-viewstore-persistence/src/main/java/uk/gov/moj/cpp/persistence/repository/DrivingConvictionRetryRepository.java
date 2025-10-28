package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DrivingConvictionRetryRepository extends EntityRepository<DrivingConvictionRetryEntity, UUID> {
}