package uk.gov.moj.cpp.stagingdvla.query.view.request;

public class DriverAuditQueryParameters {


    private String driverNumber;
    private String email;
    private String startDate;
    private String endDate;

    public DriverAuditQueryParameters() {
    }

    @SuppressWarnings("squid:S00107")
    public DriverAuditQueryParameters(final String driverNumber, final String email, final String startDate, final String endDate) {
        this.driverNumber = driverNumber;
        this.email = email;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public static class Builder {
        private String driverNumber;
        private String email;

        private String startDate;
        private String endDate;

        public Builder driverNumber(String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public Builder caseId(String email) {
            this.email = email;
            return this;
        }

        public Builder reasonType(String startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder reference(String endDate) {
            this.endDate = endDate;
            return this;
        }

        public DriverAuditQueryParameters build() {
            return new DriverAuditQueryParameters(driverNumber, email, startDate, endDate);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DriverQueryParameters{");
        sb.append("driverNumber='").append(driverNumber).append('\'');
        sb.append(", email='").append(email).append('\'');
        sb.append(", startDate='").append(startDate).append('\'');
        sb.append(", endDate='").append(endDate).append('\'');
        sb.append('}');
        return sb.toString();
    }
}