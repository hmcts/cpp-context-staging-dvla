package uk.gov.moj.cpp.stagingdvla.domain.constants;

public enum DriverAuditReportStatus {
    COMPLETED("Completed"),
    PENDING("Pending");
    private String status;

    DriverAuditReportStatus(final String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
