package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;

public class PreviousDrivingLicence implements Serializable {

    private static final long serialVersionUID = 1L;

    private String previousDrivingLicenceNumber;
    private String previousLastName;
    private String previousFirstNames;
    private LocalDate previousDateOfBirth;

    public static class Builder {
        private String previousDrivingLicenceNumber;
        private String previousLastName;
        private String previousFirstNames;
        private LocalDate previousDateOfBirth;

        public Builder previousDrivingLicenceNumber(String previousDrivingLicenceNumber) {
            this.previousDrivingLicenceNumber = previousDrivingLicenceNumber;
            return this;
        }

        public Builder previousLastName(String previousLastName) {
            this.previousLastName = previousLastName;
            return this;
        }

        public Builder previousFirstNames(String previousFirstNames) {
            this.previousFirstNames = previousFirstNames;
            return this;
        }

        public Builder previousDateOfBirth(LocalDate previousDateOfBirth) {
            this.previousDateOfBirth = previousDateOfBirth;
            return this;
        }

        public PreviousDrivingLicence build() {
            return new PreviousDrivingLicence(previousDrivingLicenceNumber, previousLastName, previousFirstNames, previousDateOfBirth);
        }
    }

    private PreviousDrivingLicence(final String previousDrivingLicenceNumber, final String previousLastName, final String previousFirstNames, final LocalDate previousDateOfBirth) {
        this.previousDrivingLicenceNumber = previousDrivingLicenceNumber;
        this.previousLastName = previousLastName;
        this.previousFirstNames = previousFirstNames;
        this.previousDateOfBirth = previousDateOfBirth;
    }

    public PreviousDrivingLicence(){}

    public String getPreviousDrivingLicenceNumber() {
        return previousDrivingLicenceNumber;
    }

    public String getPreviousLastName() {
        return previousLastName;
    }

    public String getPreviousFirstNames() {
        return previousFirstNames;
    }

    public LocalDate getPreviousDateOfBirth() {
        return previousDateOfBirth;
    }
}
