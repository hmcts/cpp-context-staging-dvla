package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.QueryResult;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DvlaDocumentDeliveryRepository extends EntityRepository<DvlaDocumentDeliveryEntity, UUID> {

    /**
     * Returned as a {@link QueryResult} (rather than {@code List}) so callers can page the
     * potentially large result set with {@code firstResult}/{@code maxResults} instead of
     * loading every matching row.
     */
    QueryResult<DvlaDocumentDeliveryEntity> findByMaterialStatus(String materialStatus);

    QueryResult<DvlaDocumentDeliveryEntity> findByCaseId(UUID caseId);

    QueryResult<DvlaDocumentDeliveryEntity> findByCaseIdAndMaterialStatus(UUID caseId, String materialStatus);

    List<DvlaDocumentDeliveryEntity> findByDocumentBlobUri(String documentBlobUri);
}
