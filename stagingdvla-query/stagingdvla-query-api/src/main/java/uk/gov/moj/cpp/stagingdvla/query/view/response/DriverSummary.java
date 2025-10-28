package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class DriverSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    private String drivingLicenceNumber;
    private String firstNames;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String postcode;
    private Boolean driverRedirect;

    public DriverSummary(final String drivingLicenceNumber, final String firstNames, final String lastName, final LocalDate dateOfBirth, final String gender, final String postcode, final Boolean driverRedirect) {
        this.drivingLicenceNumber = drivingLicenceNumber;
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.postcode = postcode;
        this.driverRedirect = driverRedirect;
    }

    public DriverSummary(){}

    public String getDrivingLicenceNumber() {
        return drivingLicenceNumber;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getPostcode() {
        return postcode;
    }

    public Boolean getDriverRedirect() {
        return driverRedirect;
    }

    public static final class Builder {
        private String drivingLicenceNumber;
        private String firstNames;
        private String lastName;
        private LocalDate dateOfBirth;
        private String gender;
        private String postcode;
        private Boolean driverRedirect;

        public Builder drivingLicenceNumber(final String drivingLicenceNumber) {
            this.drivingLicenceNumber = drivingLicenceNumber;
            return this;
        }

        public Builder firstNames(final String firstNames) {
            this.firstNames = firstNames;
            return this;
        }

        public Builder lastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder dateOfBirth(final LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }


        public Builder gender(final String gender) {
            this.gender = gender;
            return this;
        }

        public Builder postcode(final String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder driverRedirect(final Boolean driverRedirect) {
            this.driverRedirect = driverRedirect;
            return this;
        }

        public DriverSummary build() {
            return new DriverSummary(drivingLicenceNumber, firstNames, lastName,
                    dateOfBirth, gender, postcode, driverRedirect);
        }
    }
}
