package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Restriction implements Serializable {

    private static final long serialVersionUID = 1L;

    private String restrictionCode;
    private String restrictionLiteral;

    public static class Builder {
        private String restrictionCode;
        private String restrictionLiteral;

        public Builder restrictionCode(String restrictionCode) {
            this.restrictionCode = restrictionCode;
            return this;
        }

        public Builder restrictionLiteral(String restrictionLiteral) {
            this.restrictionLiteral = restrictionLiteral;
            return this;
        }

        public Restriction build() {
            return new Restriction(restrictionCode, restrictionLiteral);
        }
    }

    private Restriction(String restrictionCode, String restrictionLiteral) {
        this.restrictionCode = restrictionCode;
        this.restrictionLiteral = restrictionLiteral;
    }

    public Restriction(){}

    public String getRestrictionCode() {
        return restrictionCode;
    }

    public String getRestrictionLiteral() {
        return restrictionLiteral;
    }
}
