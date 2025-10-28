package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class TestPass implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String categoryCode;
    private String categoryLegalLiteral;
    private String categoryShortLiteral;
    private LocalDate testDate;
    private String status;
    private Boolean withAutomaticTransmission;
    private List<String> vehicleAdaptations;
    private Boolean withTrailer;
    private Boolean extendedTest;
    private Boolean licenceSurrendered;
    private String testingAuthority;

    public static class Builder {
        private String type;
        private String categoryCode;
        private String categoryLegalLiteral;
        private String categoryShortLiteral;
        private LocalDate testDate;
        private String status;
        private Boolean withAutomaticTransmission;
        private List<String> vehicleAdaptations;
        private Boolean withTrailer;
        private Boolean extendedTest;
        private Boolean licenceSurrendered;
        private String testingAuthority;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }

        public Builder categoryLegalLiteral(String categoryLegalLiteral) {
            this.categoryLegalLiteral = categoryLegalLiteral;
            return this;
        }

        public Builder categoryShortLiteral(String categoryShortLiteral) {
            this.categoryShortLiteral = categoryShortLiteral;
            return this;
        }

        public Builder testDate(LocalDate testDate) {
            this.testDate = testDate;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder withAutomaticTransmission(Boolean withAutomaticTransmission) {
            this.withAutomaticTransmission = withAutomaticTransmission;
            return this;
        }

        public Builder vehicleAdaptations(List<String> vehicleAdaptations) {
            this.vehicleAdaptations = vehicleAdaptations;
            return this;
        }

        public Builder withTrailer(Boolean withTrailer) {
            this.withTrailer = withTrailer;
            return this;
        }

        public Builder extendedTest(Boolean extendedTest) {
            this.extendedTest = extendedTest;
            return this;
        }

        public Builder licenceSurrendered(Boolean licenceSurrendered) {
            this.licenceSurrendered = licenceSurrendered;
            return this;
        }

        public Builder testingAuthority(String testingAuthority) {
            this.testingAuthority = testingAuthority;
            return this;
        }

        public TestPass build() {
            return new TestPass(type, categoryCode, categoryLegalLiteral, categoryShortLiteral, testDate, status, withAutomaticTransmission, vehicleAdaptations, withTrailer, extendedTest, licenceSurrendered, testingAuthority);
        }
    }

    @SuppressWarnings("squid:S00107")
    public TestPass(String type, String categoryCode, String categoryLegalLiteral, String categoryShortLiteral, LocalDate testDate, String status, Boolean withAutomaticTransmission, List<String> vehicleAdaptations, Boolean withTrailer, Boolean extendedTest, Boolean licenceSurrendered, String testingAuthority) {
        this.type = type;
        this.categoryCode = categoryCode;
        this.categoryLegalLiteral = categoryLegalLiteral;
        this.categoryShortLiteral = categoryShortLiteral;
        this.testDate = testDate;
        this.status = status;
        this.withAutomaticTransmission = withAutomaticTransmission;
        this.vehicleAdaptations = vehicleAdaptations;
        this.withTrailer = withTrailer;
        this.extendedTest = extendedTest;
        this.licenceSurrendered = licenceSurrendered;
        this.testingAuthority = testingAuthority;
    }

    public TestPass() { }

    public String getType() {
        return type;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryLegalLiteral() {
        return categoryLegalLiteral;
    }

    public String getCategoryShortLiteral() {
        return categoryShortLiteral;
    }

    public LocalDate getTestDate() {
        return testDate;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getWithAutomaticTransmission() {
        return withAutomaticTransmission;
    }

    public List<String> getVehicleAdaptations() {
        return vehicleAdaptations;
    }

    public Boolean getWithTrailer() {
        return withTrailer;
    }

    public Boolean getExtendedTest() {
        return extendedTest;
    }

    public Boolean getLicenceSurrendered() {
        return licenceSurrendered;
    }

    public String getTestingAuthority() {
        return testingAuthority;
    }
}

