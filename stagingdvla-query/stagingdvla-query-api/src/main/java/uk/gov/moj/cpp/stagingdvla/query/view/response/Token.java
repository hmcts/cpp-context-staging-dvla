package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;

public class Token implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate validFromDate;
    private LocalDate validToDate;
    private String issueNumber;

    public static class Builder {
        private LocalDate validFromDate;
        private LocalDate validToDate;
        private String issueNumber;

        public Token.Builder validFromDate(LocalDate validFromDate) {
            this.validFromDate = validFromDate;
            return this;
        }

        public Token.Builder validToDate(LocalDate validToDate) {
            this.validToDate = validToDate;
            return this;
        }

        public Token.Builder tokenIssueNumber(String issueNumber) {
            this.issueNumber = issueNumber;
            return this;
        }

        public Token build() {
            return new Token(validFromDate, validToDate, issueNumber);
        }
    }

    private Token(LocalDate validFromDate, LocalDate validToDate, String issueNumber) {
        this.validFromDate = validFromDate;
        this.validToDate = validToDate;
        this.issueNumber = issueNumber;
    }

    public Token() { }

    public LocalDate getValidFromDate() {
        return validFromDate;
    }

    public LocalDate getValidToDate() {
        return validToDate;
    }

    public String getIssueNumber() {
        return issueNumber;
    }
}
