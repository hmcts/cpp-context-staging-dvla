package uk.gov.moj.cpp.stagingdvla.query.view.request;

public class SearchCriteria {
    private final String drivingLicenceNumber;
    private final String gender;
    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final String postcode;

    public SearchCriteria(final String drivingLicenceNumber, final String gender, final String firstName, final String lastName, final String dateOfBirth, final String postcode) {
        this.drivingLicenceNumber = drivingLicenceNumber;
        this.gender = gender;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.postcode = postcode;
    }

    private SearchCriteria(final Builder builder) {
        drivingLicenceNumber = builder.drivingLicenceNumber;
        gender = builder.gender;
        firstName = builder.firstName;
        lastName = builder.lastName;
        dateOfBirth = builder.dateOfBirth;
        postcode = builder.postcode;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getDrivingLicenceNumber() {
        return drivingLicenceNumber;
    }

    public String getGender() {
        return gender;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPostcode() {
        return postcode;
    }


    public static final class Builder {
        private String drivingLicenceNumber;
        private String gender;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String postcode;

        private Builder() {
        }

        public Builder drivingLicenceNumber(final String val) {
            drivingLicenceNumber = val;
            return this;
        }

        public Builder gender(final String val) {
            gender = val;
            return this;
        }

        public Builder firstName(final String val) {
            firstName = val;
            return this;
        }

        public Builder lastName(final String val) {
            lastName = val;
            return this;
        }

        public Builder dateOfBirth(final String val) {
            dateOfBirth = val;
            return this;
        }

        public Builder postcode(final String val) {
            postcode = val;
            return this;
        }

        public SearchCriteria build() {
            return new SearchCriteria(this);
        }
    }
}
