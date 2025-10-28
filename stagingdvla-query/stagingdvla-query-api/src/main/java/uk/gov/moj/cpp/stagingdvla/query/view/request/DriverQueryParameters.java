package uk.gov.moj.cpp.stagingdvla.query.view.request;

public class DriverQueryParameters {
    private String driverNumber;
    private String caseId;
    private String reasonType;
    private String reference;

    public DriverQueryParameters() {
    }

    @SuppressWarnings("squid:S00107")
    public DriverQueryParameters(final String driverNumber, final String caseId, final String reasonType,final String reference) {
        this.driverNumber = driverNumber;
        this.caseId = caseId;
        this.reasonType = reasonType;
        this.reference = reference;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getReasonType() {
        return reasonType;
    }

    public String getReference() {
        return reference;
    }

    public static class Builder {
        private String driverNumber;
        private String caseId;

        private String reasonType;
        private String reference;

        public Builder driverNumber(String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public Builder caseId(String caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder reasonType(String reasonType) {
            this.reasonType = reasonType;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public DriverQueryParameters build() {
            return new DriverQueryParameters(driverNumber, caseId, reasonType, reference);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DriverQueryParameters{");
        sb.append("driverNumber='").append(driverNumber).append('\'');
        sb.append(", caseId='").append(caseId).append('\'');
        sb.append(", reasonType='").append(reasonType).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        sb.append('}');
        return sb.toString();
    }
}