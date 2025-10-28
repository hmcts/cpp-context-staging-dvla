package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


public class DriverResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Driver driver;
    private Licence licence;
    private List<Entitlement> entitlement;
    private List<TestPass> testPass;
    private List<Endorsement> endorsements;
    private TokenValidity tokenValidity;
    private Boolean driverRedirect;
    private Error error;

    public static class Builder {
        private Driver driver;
        private Licence licence;
        private List<Entitlement> entitlement;
        private List<TestPass> testPass;
        private List<Endorsement> endorsements;
        private TokenValidity tokenValidity;
        private Boolean driverRedirect;
        private Error error;

        public Builder driver(Driver driver) {
            this.driver = driver;
            return this;
        }

        public Builder licence(Licence licence) {
            this.licence = licence;
            return this;
        }

        public Builder entitlement(List<Entitlement> entitlement) {
            this.entitlement = entitlement;
            return this;
        }

        public Builder testPass(List<TestPass> testPass) {
            this.testPass = testPass;
            return this;
        }

        public Builder endorsements(List<Endorsement> endorsements) {
            this.endorsements = endorsements;
            return this;
        }

        public Builder tokenValidity(TokenValidity tokenValidity) {
            this.tokenValidity = tokenValidity;
            return this;
        }

        public Builder driverRedirect(Boolean driverRedirect) {
            this.driverRedirect = driverRedirect;
            return this;
        }

        public Builder error(Error error) {
            this.error = error;
            return this;
        }

        public DriverResponse build() {
            return new DriverResponse(driver, licence, entitlement, testPass, endorsements, tokenValidity, driverRedirect, error);
        }
    }

    private DriverResponse(final Driver driver, final Licence licence, final List<Entitlement> entitlement, final List<TestPass> testPass, final List<Endorsement> endorsements, final TokenValidity tokenValidity, final Boolean driverRedirect, final Error error) {
        this.driver = driver;
        this.licence = licence;
        this.entitlement = entitlement;
        this.testPass = testPass;
        this.endorsements = endorsements;
        this.tokenValidity = tokenValidity;
        this.driverRedirect = driverRedirect;
        this.error = error;
    }

    public DriverResponse() {}

    public Driver getDriver() {
        return driver;
    }

    public Licence getLicence() {
        return licence;
    }

    public List<Entitlement> getEntitlement() {
        return entitlement;
    }

    public List<TestPass> getTestPass() {
        return testPass;
    }

    public List<Endorsement> getEndorsements() {
        return endorsements;
    }

    public TokenValidity getTokenValidity() {
        return tokenValidity;
    }

    @JsonProperty("token")
    public void setToken(Token token) {
        this.tokenValidity = token != null ? new TokenValidity.Builder()
                .tokenValidFromDate(token.getValidFromDate())
                .tokenValidToDate(token.getValidToDate())
                .tokenIssueNumber(token.getIssueNumber())
                .build() : null;

    }

    public Boolean getDriverRedirect() {
        return driverRedirect;
    }

    public Error getError() {
        return error;
    }
}
