package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface DriverAuditRepository extends EntityRepository<DriverAuditEntity, UUID> {

    List<DriverAuditEntity> findByUserId(UUID userId);


    @Query(value = "SELECT * FROM driver_audit h WHERE h.date_time >= :startDate AND h.date_time <= :endDate " +
            "AND (" +
            "(:drivingLicenseNumber IS NULL OR h.driving_license_number = CAST(:drivingLicenseNumber AS character varying)) " +
            "AND (:userEmail IS NULL OR LOWER(h.user_email) = CAST(:userEmail AS character varying)) " +
            ")", isNative = true)
    List<DriverAuditEntity> findAllActiveDriverAuditRecords(@QueryParam("startDate") final LocalDateTime startDate,
                                                            @QueryParam("endDate") final LocalDateTime endDate,
                                                            @QueryParam("drivingLicenseNumber") final String drivingLicenseNumber,
                                                            @QueryParam("userEmail") final String userEmail);

}

