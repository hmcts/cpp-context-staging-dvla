package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DriverAuditReportRepository extends EntityRepository<DriverAuditReportEntity, UUID> {


    @Query(value = "from DriverAuditReportEntity dar where dar.userId = :userId order by dar.dateTime desc")
    List<DriverAuditReportEntity> findByUserIdOrderByDateTimeDesc(@QueryParam("userId") final UUID userId);

    DriverAuditReportEntity findByIdAndUserIdAndMaterialId(final UUID id, final UUID userId, final UUID materialId);


}
