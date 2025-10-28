package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;

public class TokenValidity implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate tokenValidFromDate;
    private LocalDate tokenValidToDate;
    private String tokenIssueNumber;

    public static class Builder {
        private LocalDate tokenValidFromDate;
        private LocalDate tokenValidToDate;
        private String tokenIssueNumber;

        public Builder tokenValidFromDate(LocalDate tokenValidFromDate) {
            this.tokenValidFromDate = tokenValidFromDate;
            return this;
        }

        public Builder tokenValidToDate(LocalDate tokenValidToDate) {
            this.tokenValidToDate = tokenValidToDate;
            return this;
        }

        public Builder tokenIssueNumber(String tokenIssueNumber) {
            this.tokenIssueNumber = tokenIssueNumber;
            return this;
        }

        public TokenValidity build() {
            return new TokenValidity(tokenValidFromDate, tokenValidToDate, tokenIssueNumber);
        }
    }

    private TokenValidity(LocalDate tokenValidFromDate, LocalDate tokenValidToDate, String tokenIssueNumber) {
        this.tokenValidFromDate = tokenValidFromDate;
        this.tokenValidToDate = tokenValidToDate;
        this.tokenIssueNumber = tokenIssueNumber;
    }

    public TokenValidity() { }

    public LocalDate getTokenValidFromDate() {
        return tokenValidFromDate;
    }

    public LocalDate getTokenValidToDate() {
        return tokenValidToDate;
    }

    public String getTokenIssueNumber() {
        return tokenIssueNumber;
    }
}
