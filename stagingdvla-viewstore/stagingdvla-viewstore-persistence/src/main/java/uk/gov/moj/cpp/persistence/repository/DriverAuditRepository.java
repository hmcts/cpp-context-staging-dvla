package uk.gov.moj.cpp.persistence.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

    String BASE_QRY = "SELECT * FROM driver_audit h WHERE h.date_time >= :startDate AND h.date_time <= :endDate";
    String DLN_QRY = " AND h.driving_license_number = :drivingLicenseNumber";
    String EMAIL_QRY = " AND LOWER(h.user_email) = LOWER(:userEmail)";

    List<DriverAuditEntity> findByUserId(UUID userId);

    default List<DriverAuditEntity> findAllActiveDriverAuditRecords(@QueryParam("startDate") final LocalDateTime startDate,
                                                                    @QueryParam("endDate") final LocalDateTime endDate,
                                                                    @QueryParam("drivingLicenseNumber") final String drivingLicenseNumber,
                                                                    @QueryParam("userEmail") final String userEmail) {
        if (isBlank(drivingLicenseNumber) && isBlank(userEmail)) {
            return findAllAuditRecords(startDate, endDate);
        } else if (isBlank(drivingLicenseNumber)) {
            return findByEmail(startDate, endDate, userEmail);
        } else if (isBlank(userEmail)) {
            return findByDrivingLicenseNumber(startDate, endDate, drivingLicenseNumber);
        } else {
            return findByEmailAndDrivingLicenseNumber(startDate, endDate, drivingLicenseNumber, userEmail);
        }
    }


    @Query(value = BASE_QRY, isNative = true)
    List<DriverAuditEntity> findAllAuditRecords(@QueryParam("startDate") final LocalDateTime startDate,
                                                @QueryParam("endDate") final LocalDateTime endDate);

    @Query(value = BASE_QRY + DLN_QRY, isNative = true)
    List<DriverAuditEntity> findByDrivingLicenseNumber(@QueryParam("startDate") final LocalDateTime startDate,
                                                       @QueryParam("endDate") final LocalDateTime endDate,
                                                       @QueryParam("drivingLicenseNumber") final String drivingLicenseNumber);

    @Query(value = BASE_QRY + EMAIL_QRY, isNative = true)
    List<DriverAuditEntity> findByEmail(@QueryParam("startDate") final LocalDateTime startDate,
                                        @QueryParam("endDate") final LocalDateTime endDate,
                                        @QueryParam("userEmail") final String userEmail);

    @Query(value = BASE_QRY + DLN_QRY + EMAIL_QRY, isNative = true)
    List<DriverAuditEntity> findByEmailAndDrivingLicenseNumber(@QueryParam("startDate") final LocalDateTime startDate,
                                                               @QueryParam("endDate") final LocalDateTime endDate,
                                                               @QueryParam("drivingLicenseNumber") final String drivingLicenseNumber,
                                                               @QueryParam("userEmail") final String userEmail);


}
