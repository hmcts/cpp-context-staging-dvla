package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class Entitlement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String categoryCode;
    private String categoryLegalLiteral;
    private String categoryShortLiteral;
    private String categoryType;
    private LocalDate fromDate;
    private LocalDate expiryDate;
    private String categoryStatus;
    private List<Restriction> restrictions;
    private Boolean restrictedToAutomaticTransmission;
    private Boolean fromNonGB;

    public static class Builder {
        private String categoryCode;
        private String categoryLegalLiteral;
        private String categoryShortLiteral;
        private String categoryType;
        private LocalDate fromDate;
        private LocalDate expiryDate;
        private String categoryStatus;
        private List<Restriction> restrictions;
        private Boolean restrictedToAutomaticTransmission;
        private Boolean fromNonGB;

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

        public Builder categoryType(String categoryType) {
            this.categoryType = categoryType;
            return this;
        }

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder categoryStatus(String categoryStatus) {
            this.categoryStatus = categoryStatus;
            return this;
        }

        public Builder restrictions(List<Restriction> restrictions) {
            this.restrictions = restrictions;
            return this;
        }

        public Builder restrictedToAutomaticTransmission(Boolean restrictedToAutomaticTransmission) {
            this.restrictedToAutomaticTransmission = restrictedToAutomaticTransmission;
            return this;
        }

        public Builder fromNonGB(Boolean fromNonGB) {
            this.fromNonGB = fromNonGB;
            return this;
        }

        public Entitlement build() {
            return new Entitlement(categoryCode, categoryLegalLiteral, categoryShortLiteral, categoryType, fromDate, expiryDate, categoryStatus, restrictions, restrictedToAutomaticTransmission, fromNonGB);
        }
    }

    private Entitlement(String categoryCode, String categoryLegalLiteral, String categoryShortLiteral, String categoryType, LocalDate fromDate, LocalDate expiryDate, String categoryStatus, List<Restriction> restrictions, Boolean restrictedToAutomaticTransmission, Boolean fromNonGB) {
        this.categoryCode = categoryCode;
        this.categoryLegalLiteral = categoryLegalLiteral;
        this.categoryShortLiteral = categoryShortLiteral;
        this.categoryType = categoryType;
        this.fromDate = fromDate;
        this.expiryDate = expiryDate;
        this.categoryStatus = categoryStatus;
        this.restrictions = restrictions;
        this.restrictedToAutomaticTransmission = restrictedToAutomaticTransmission;
        this.fromNonGB = fromNonGB;
    }

    public Entitlement() {}

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getCategoryLegalLiteral() {
        return categoryLegalLiteral;
    }

    public String getCategoryShortLiteral() {
        return categoryShortLiteral;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getCategoryStatus() {
        return categoryStatus;
    }

    public List<Restriction> getRestrictions() {
        return restrictions;
    }

    public Boolean getRestrictedToAutomaticTransmission() {
        return restrictedToAutomaticTransmission;
    }

    public Boolean getFromNonGB() {
        return fromNonGB;
    }

}
