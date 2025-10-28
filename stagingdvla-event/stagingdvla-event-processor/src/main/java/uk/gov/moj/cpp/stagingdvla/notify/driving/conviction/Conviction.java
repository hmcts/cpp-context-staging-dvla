package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class Conviction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String identifier;

    private String convictingCourtCode;

    private String convictionDate;

    private String drivingLicenceNumber;

    private String firstNames;

    private String lastName;

    private String title;

    private String nameFormat;

    private String gender;

    private String dateOfBirth;

    private Address address;

    private Boolean noFixedAbode;

    private String licenceProducedInCourt;

    private String licenceIssueNumber;

    private Boolean licenceSurrendered;

    private Boolean previouslyNotified;

    private Boolean remove;

    public String getIdentifier() {
        return identifier;
    }

    public String getConvictingCourtCode() {
        return convictingCourtCode;
    }

    public String getConvictionDate() {
        return convictionDate;
    }

    public String getDrivingLicenceNumber() {
        return drivingLicenceNumber;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastName() {
        return lastName;
    }

    public String getTitle() {
        return title;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public String getGender() {
        return gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public Address getAddress() {
        return address;
    }

    public Boolean getNoFixedAbode() {
        return noFixedAbode;
    }

    public String getLicenceProducedInCourt() {
        return licenceProducedInCourt;
    }

    public String getLicenceIssueNumber() {
        return licenceIssueNumber;
    }

    public Boolean getLicenceSurrendered() {
        return licenceSurrendered;
    }

    public Boolean getPreviouslyNotified() {
        return previouslyNotified;
    }

    public Boolean getRemove() {
        return remove;
    }

    public static Builder conviction() {
        return new Conviction.Builder();
    }

    @Override
    public String toString() {
        return "Conviction{" +
                "identifier='" + identifier + '\'' +
                ", convictingCourtCode='" + convictingCourtCode + '\'' +
                ", convictionDate='" + convictionDate + '\'' +
                ", drivingLicenceNumber='" + drivingLicenceNumber + '\'' +
                ", firstNames='" + firstNames + '\'' +
                ", lastName='" + lastName + '\'' +
                ", title='" + title + '\'' +
                ", nameFormat='" + nameFormat + '\'' +
                ", gender='" + gender + '\'' +
                ", dateOfBirth='" + dateOfBirth + '\'' +
                ", address=" + address +
                ", noFixedAbode=" + noFixedAbode +
                ", licenceProducedInCourt='" + licenceProducedInCourt + '\'' +
                ", licenceIssueNumber='" + licenceIssueNumber + '\'' +
                ", licenceSurrendered=" + licenceSurrendered +
                ", previouslyNotified=" + previouslyNotified +
                ", remove=" + remove +
                '}';
    }

    public static final class Builder {
        private String identifier;
        private String convictingCourtCode;
        private String convictionDate;
        private String drivingLicenceNumber;
        private String firstNames;
        private String lastName;
        private String title;
        private String nameFormat;
        private String gender;
        private String dateOfBirth;
        private Address address;
        private Boolean noFixedAbode;
        private String licenceProducedInCourt;
        private String licenceIssueNumber;
        private Boolean licenceSurrendered;
        private Boolean previouslyNotified;
        private Boolean remove;

        private Builder() {
        }

        public static Builder aConviction() {
            return new Builder();
        }

        public Builder withIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder withConvictingCourtCode(String convictingCourtCode) {
            this.convictingCourtCode = convictingCourtCode;
            return this;
        }

        public Builder withConvictionDate(String convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public Builder withDrivingLicenceNumber(String drivingLicenceNumber) {
            this.drivingLicenceNumber = drivingLicenceNumber;
            return this;
        }

        public Builder withFirstNames(String firstNames) {
            this.firstNames = firstNames;
            return this;
        }

        public Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withNameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
            return this;
        }

        public Builder withGender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder withDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withAddress(Address address) {
            this.address = address;
            return this;
        }

        public Builder withNoFixedAbode(Boolean noFixedAbode) {
            this.noFixedAbode = noFixedAbode;
            return this;
        }

        public Builder withLicenceProducedInCourt(String licenceProducedInCourt) {
            this.licenceProducedInCourt = licenceProducedInCourt;
            return this;
        }

        public Builder withLicenceIssueNumber(String licenceIssueNumber) {
            this.licenceIssueNumber = licenceIssueNumber;
            return this;
        }

        public Builder withLicenceSurrendered(Boolean licenceSurrendered) {
            this.licenceSurrendered = licenceSurrendered;
            return this;
        }

        public Builder withPreviouslyNotified(Boolean previouslyNotified) {
            this.previouslyNotified = previouslyNotified;
            return this;
        }

        public Builder withRemove(Boolean remove) {
            this.remove = remove;
            return this;
        }

        public Conviction build() {
            final Conviction conviction = new Conviction();
            conviction.gender = this.gender;
            conviction.remove = this.remove;
            conviction.firstNames = this.firstNames;
            conviction.lastName = this.lastName;
            conviction.address = this.address;
            conviction.convictionDate = this.convictionDate;
            conviction.licenceSurrendered = this.licenceSurrendered;
            conviction.identifier = this.identifier;
            conviction.licenceProducedInCourt = this.licenceProducedInCourt;
            conviction.noFixedAbode = this.noFixedAbode;
            conviction.title = this.title;
            conviction.nameFormat = this.nameFormat;
            conviction.drivingLicenceNumber = this.drivingLicenceNumber;
            conviction.convictingCourtCode = this.convictingCourtCode;
            conviction.previouslyNotified = this.previouslyNotified;
            conviction.licenceIssueNumber = this.licenceIssueNumber;
            conviction.dateOfBirth = this.dateOfBirth;
            return conviction;
        }
    }
}
