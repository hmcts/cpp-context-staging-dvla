package uk.gov.moj.cpp.persistence.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class DriverAuditRepository {

    private static final String BASE_QRY = "SELECT * FROM driver_audit h WHERE h.date_time >= :startDate AND h.date_time <= :endDate";
    private static final String DLN_QRY = " AND h.driving_license_number = :drivingLicenseNumber";
    private static final String EMAIL_QRY = " AND LOWER(h.user_email) = LOWER(:userEmail)";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";

    @PersistenceContext(unitName = "stagingdvla-persistence-unit")
    EntityManager entityManager;

    public DriverAuditEntity save(final DriverAuditEntity driverAuditEntity) {
        return entityManager.merge(driverAuditEntity);
    }

    public List<DriverAuditEntity> findByUserId(final UUID userId) {
        return entityManager.createQuery(
                        "SELECT d FROM DriverAuditEntity d WHERE d.userId = :userId", DriverAuditEntity.class)
                .setParameter("userId", userId)
                .getResultList();
    }

    public List<DriverAuditEntity> findAllActiveDriverAuditRecords(final LocalDateTime startDate,
                                                                   final LocalDateTime endDate,
                                                                   final String drivingLicenseNumber,
                                                                   final String userEmail) {
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

    @SuppressWarnings("unchecked")
    public List<DriverAuditEntity> findAllAuditRecords(final LocalDateTime startDate, final LocalDateTime endDate) {
        return entityManager.createNativeQuery(BASE_QRY, DriverAuditEntity.class)
                .setParameter(START_DATE, startDate)
                .setParameter(END_DATE, endDate)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<DriverAuditEntity> findByDrivingLicenseNumber(final LocalDateTime startDate, final LocalDateTime endDate, final String drivingLicenseNumber) {
        return entityManager.createNativeQuery(BASE_QRY + DLN_QRY, DriverAuditEntity.class)
                .setParameter(START_DATE, startDate)
                .setParameter(END_DATE, endDate)
                .setParameter("drivingLicenseNumber", drivingLicenseNumber)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<DriverAuditEntity> findByEmail(final LocalDateTime startDate, final LocalDateTime endDate, final String userEmail) {
        return entityManager.createNativeQuery(BASE_QRY + EMAIL_QRY, DriverAuditEntity.class)
                .setParameter(START_DATE, startDate)
                .setParameter(END_DATE, endDate)
                .setParameter("userEmail", userEmail)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<DriverAuditEntity> findByEmailAndDrivingLicenseNumber(final LocalDateTime startDate, final LocalDateTime endDate, final String drivingLicenseNumber, final String userEmail) {
        return entityManager.createNativeQuery(BASE_QRY + DLN_QRY + EMAIL_QRY, DriverAuditEntity.class)
                .setParameter(START_DATE, startDate)
                .setParameter(END_DATE, endDate)
                .setParameter("drivingLicenseNumber", drivingLicenseNumber)
                .setParameter("userEmail", userEmail)
                .getResultList();
    }
}
