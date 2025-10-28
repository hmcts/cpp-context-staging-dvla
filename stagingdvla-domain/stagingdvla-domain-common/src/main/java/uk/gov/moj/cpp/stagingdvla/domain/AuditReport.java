package uk.gov.moj.cpp.stagingdvla.domain;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class AuditReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;

    private UUID userId;

    private ZonedDateTime dateTime;

    private String reportSearchCriteria;

    private String status;

    private String reportFileId;

    private UUID materialId;

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


    public static class AuditReportBuilder {
        private UUID id;
        private UUID userId;
        private ZonedDateTime dateTime;
        private String reportSearchCriteria;
        private String status;
        private String reportFileId;
        private UUID materialId;

        public static AuditReportBuilder auditReport() {
            return new AuditReportBuilder();
        }

        public AuditReportBuilder withId(UUID id) {
            this.id = id;
            return this;
        }

        public AuditReportBuilder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public AuditReportBuilder withDateTime(ZonedDateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public AuditReportBuilder withReportSearchCriteria(String reportSearchCriteria) {
            this.reportSearchCriteria = reportSearchCriteria;
            return this;
        }

        public AuditReportBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public AuditReportBuilder withReportFileId(String reportFileId) {
            this.reportFileId = reportFileId;
            return this;
        }

        public AuditReportBuilder withMaterialId(UUID materialId) {
            this.materialId = materialId;
            return this;
        }

        public AuditReport build() {
            final AuditReport auditReport = new AuditReport();
            auditReport.setId(id);
            auditReport.setUserId(userId);
            auditReport.setDateTime(dateTime);
            auditReport.setReportSearchCriteria(reportSearchCriteria);
            auditReport.setStatus(status);
            auditReport.setReportFileId(reportFileId);
            auditReport.setMaterialId(materialId);
            return auditReport;
        }
    }
}
