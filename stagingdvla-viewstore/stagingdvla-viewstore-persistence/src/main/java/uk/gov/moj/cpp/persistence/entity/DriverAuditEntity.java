package uk.gov.moj.cpp.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "driver_audit")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class DriverAuditEntity implements Serializable {

    private static final long serialVersionUID = 2442781778236204987L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "user_id",nullable = false)
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "date_time", nullable = false)
    private ZonedDateTime dateTime;

    @Column(name = "reason_type",nullable = false)
    private String reasonType;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "driving_license_number")
    private String drivingLicenseNumber;

    @Column(name = "first_names")
    private String firstNames;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "postcode")
    private String postcode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    public DriverAuditEntity() {
    }

    public DriverAuditEntity(final UUID id, final UUID userId, final String userEmail, final ZonedDateTime dateTime, final String reasonType, final String reference, final String drivingLicenseNumber, final String firstNames, final String lastName, final String gender, final String postcode, final LocalDate dateOfBirth) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.dateTime = dateTime;
        this.reasonType = reasonType;
        this.reference = reference;
        this.drivingLicenseNumber = drivingLicenseNumber;
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.gender = gender;
        this.postcode = postcode;
        this.dateOfBirth = dateOfBirth;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(final String userEmail) {
        this.userEmail = userEmail;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(final ZonedDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getReasonType() {
        return reasonType;
    }

    public void setReasonType(final String reasonType) {
        this.reasonType = reasonType;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    public String getDrivingLicenseNumber() {
        return drivingLicenseNumber;
    }

    public void setDrivingLicenseNumber(final String drivingLicenseNumber) {
        this.drivingLicenseNumber = drivingLicenseNumber;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public void setFirstNames(final String firstNames) {
        this.firstNames = firstNames;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(final String postcode) {
        this.postcode = postcode;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }
}
