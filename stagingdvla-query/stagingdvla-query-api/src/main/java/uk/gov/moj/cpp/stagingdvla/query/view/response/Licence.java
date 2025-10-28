package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Licence implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String status;
    private String statusQualifier;
    private String countryToWhichExchanged;

    public static class Builder {
        private String type;
        private String status;
        private String statusQualifier;
        private String countryToWhichExchanged;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder statusQualifier(String statusQualifier) {
            this.statusQualifier = statusQualifier;
            return this;
        }

        public Builder countryToWhichExchanged(String countryToWhichExchanged) {
            this.countryToWhichExchanged = countryToWhichExchanged;
            return this;
        }

        public Licence build() {
            return new Licence(type, status, statusQualifier, countryToWhichExchanged);
        }
    }

    private Licence(String type, String status, String statusQualifier, String countryToWhichExchanged) {
        this.type = type;
        this.status = status;
        this.statusQualifier = statusQualifier;
        this.countryToWhichExchanged = countryToWhichExchanged;
    }

    public Licence() {}

    public String getType() {
        return type;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusQualifier() {
        return statusQualifier;
    }

    public String getCountryToWhichExchanged() {
        return countryToWhichExchanged;
    }
}