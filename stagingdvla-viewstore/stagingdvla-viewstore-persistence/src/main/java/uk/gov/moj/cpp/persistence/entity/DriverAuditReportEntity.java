package uk.gov.moj.cpp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "driver_audit_report")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class DriverAuditReportEntity implements Serializable {
    private static final long serialVersionUID = 2442781778236204988L;
    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "date_time", nullable = false)
    private ZonedDateTime dateTime;

    @Column(name = "report_search_criteria", nullable = false)
    private String reportSearchCriteria;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "report_file_id")
    private String reportFileId;

    @Column(name = "material_id")
    private UUID materialId;

    public DriverAuditReportEntity() {
    }

    public DriverAuditReportEntity(final UUID id, final UUID userId, final ZonedDateTime dateTime, final String reportSearchCriteria, final String status, final String reportFileId, final UUID materialId) {
        this.id = id;
        this.userId = userId;
        this.dateTime = dateTime;
        this.reportSearchCriteria = reportSearchCriteria;
        this.status = status;
        this.reportFileId = reportFileId;
        this.materialId = materialId;
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

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(final ZonedDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getReportSearchCriteria() {
        return reportSearchCriteria;
    }

    public void setReportSearchCriteria(final String reportSearchCriteria) {
        this.reportSearchCriteria = reportSearchCriteria;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getReportFileId() {
        return reportFileId;
    }

    public void setReportFileId(final String reportFileId) {
        this.reportFileId = reportFileId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }


    public static final class DriverAuditReportEntityBuilder {
        private UUID id;
        private UUID userId;
        private ZonedDateTime dateTime;
        private String reportSearchCriteria;
        private String status;
        private String reportFileId;
        private UUID materialId;

        public static DriverAuditReportEntityBuilder builder() {
            return new DriverAuditReportEntityBuilder();
        }

        public DriverAuditReportEntityBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public DriverAuditReportEntityBuilder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public DriverAuditReportEntityBuilder withDateTime(ZonedDateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public DriverAuditReportEntityBuilder withReportSearchCriteria(String reportSearchCriteria) {
            this.reportSearchCriteria = reportSearchCriteria;
            return this;
        }

        public DriverAuditReportEntityBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public DriverAuditReportEntityBuilder withReportFileId(String reportFileId) {
            this.reportFileId = reportFileId;
            return this;
        }

        public DriverAuditReportEntityBuilder withMaterialId(UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public DriverAuditReportEntity build() {
            final DriverAuditReportEntity driverAuditReportEntity = new DriverAuditReportEntity();
            driverAuditReportEntity.setId(id);
            driverAuditReportEntity.setUserId(userId);
            driverAuditReportEntity.setDateTime(dateTime);
            driverAuditReportEntity.setReportSearchCriteria(reportSearchCriteria);
            driverAuditReportEntity.setStatus(status);
            driverAuditReportEntity.setReportFileId(reportFileId);
            driverAuditReportEntity.setMaterialId(materialId);
            return driverAuditReportEntity;
        }
    }
}
