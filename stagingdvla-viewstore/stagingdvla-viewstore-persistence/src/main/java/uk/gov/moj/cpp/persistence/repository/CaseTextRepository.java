package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.CaseTextEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface CaseTextRepository extends EntityRepository<CaseTextEntity, UUID> {

    List<CaseTextEntity> findByCaseIdOrderByCreatedDateTimeDesc(UUID caseId);

}
