package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class Driver implements Serializable {

    private static final long serialVersionUID = 1L;

    private String drivingLicenceNumber;
    private String firstNames;
    private String lastName;
    private String title;
    private String nameFormat;
    private String fullModeOfAddress;
    private String gender;
    private LocalDate dateOfBirth;
    private String placeOfBirth;
    private Address address;
    private LocalDate disqualifiedUntil;
    private Boolean disqualifiedForLife;
    private String eyesight;
    private Boolean imagesExist;
    private Boolean disqualifiedPendingSentence;
    private Boolean retainedC1_D1Entitlement;
    private List<PreviousDrivingLicence> previousDrivingLicence;

    public static class Builder {
        private String drivingLicenceNumber;
        private String firstNames;
        private String lastName;
        private String title;
        private String nameFormat;
        private String fullModeOfAddress;
        private String gender;
        private LocalDate dateOfBirth;
        private String placeOfBirth;
        private Address address;
        private LocalDate disqualifiedUntil;
        private Boolean disqualifiedForLife;
        private String eyesight;
        private Boolean imagesExist;
        private Boolean disqualifiedPendingSentence;
        private Boolean retainedC1_d1Entitlement;
        private List<PreviousDrivingLicence> previousDrivingLicence;

        public Builder drivingLicenceNumber(String drivingLicenceNumber) {
            this.drivingLicenceNumber = drivingLicenceNumber;
            return this;
        }

        public Builder firstNames(String firstNames) {
            this.firstNames = firstNames;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder nameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
            return this;
        }

        public Builder fullModeOfAddress(String fullModeOfAddress) {
            this.fullModeOfAddress = fullModeOfAddress;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder placeOfBirth(String placeOfBirth) {
            this.placeOfBirth = placeOfBirth;
            return this;
        }

        public Builder address(Address address) {
            this.address = address;
            return this;
        }

        public Builder disqualifiedUntil(LocalDate disqualifiedUntil) {
            this.disqualifiedUntil = disqualifiedUntil;
            return this;
        }

        public Builder disqualifiedForLife(Boolean disqualifiedForLife) {
            this.disqualifiedForLife = disqualifiedForLife;
            return this;
        }

        public Builder eyesight(String eyesight) {
            this.eyesight = eyesight;
            return this;
        }

        public Builder imagesExist(Boolean imagesExist) {
            this.imagesExist = imagesExist;
            return this;
        }

        public Builder disqualifiedPendingSentence(Boolean disqualifiedPendingSentence) {
            this.disqualifiedPendingSentence = disqualifiedPendingSentence;
            return this;
        }

        public Builder retainedC1_d1Entitlement(Boolean retainedC1_d1Entitlement) {
            this.retainedC1_d1Entitlement = retainedC1_d1Entitlement;
            return this;
        }

        public Builder previousDrivingLicence(List<PreviousDrivingLicence> previousDrivingLicence) {
            this.previousDrivingLicence = previousDrivingLicence;
            return this;
        }

        public Driver build() {
            return new Driver(drivingLicenceNumber, firstNames, lastName, title, nameFormat, fullModeOfAddress, gender, dateOfBirth, placeOfBirth, address, disqualifiedUntil, disqualifiedForLife, eyesight, imagesExist, disqualifiedPendingSentence, retainedC1_d1Entitlement, previousDrivingLicence);
        }
    }

    @SuppressWarnings("squid:S00107")
    public Driver(String drivingLicenceNumber, String firstNames, String lastName, String title, String nameFormat, String fullModeOfAddress, String gender, LocalDate dateOfBirth, String placeOfBirth, Address address, LocalDate disqualifiedUntil, Boolean disqualifiedForLife, String eyesight, Boolean imagesExist, Boolean disqualifiedPendingSentence, Boolean retainedC1_D1Entitlement, List<PreviousDrivingLicence> previousDrivingLicence) {
        this.drivingLicenceNumber = drivingLicenceNumber;
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.title = title;
        this.nameFormat = nameFormat;
        this.fullModeOfAddress = fullModeOfAddress;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.placeOfBirth = placeOfBirth;
        this.address = address;
        this.disqualifiedUntil = disqualifiedUntil;
        this.disqualifiedForLife = disqualifiedForLife;
        this.eyesight = eyesight;
        this.imagesExist = imagesExist;
        this.disqualifiedPendingSentence = disqualifiedPendingSentence;
        this.retainedC1_D1Entitlement = retainedC1_D1Entitlement;
        this.previousDrivingLicence = previousDrivingLicence;
    }

    public Driver() {}

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

    public String getFullModeOfAddress() {
        return fullModeOfAddress;
    }

    public void setDrivingLicenceNumber(String drivingLicenceNumber) {
        this.drivingLicenceNumber = drivingLicenceNumber;
    }

    public void setFirstNames(String firstNames) {
        this.firstNames = firstNames;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public void setFullModeOfAddress(String fullModeOfAddress) {
        this.fullModeOfAddress = fullModeOfAddress;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public void setDisqualifiedUntil(LocalDate disqualifiedUntil) {
        this.disqualifiedUntil = disqualifiedUntil;
    }

    public void setDisqualifiedForLife(Boolean disqualifiedForLife) {
        this.disqualifiedForLife = disqualifiedForLife;
    }

    public void setEyesight(String eyesight) {
        this.eyesight = eyesight;
    }

    public void setImagesExist(Boolean imagesExist) {
        this.imagesExist = imagesExist;
    }

    public void setRetainedC1_D1Entitlement(Boolean retainedC1_D1Entitlement) {
        this.retainedC1_D1Entitlement = retainedC1_D1Entitlement;
    }

    public void setPreviousDrivingLicence(List<PreviousDrivingLicence> previousDrivingLicence) {
        this.previousDrivingLicence = previousDrivingLicence;
    }

    public void setDisqualifiedPendingSentence(final Boolean disqualifiedPendingSentence) {
        this.disqualifiedPendingSentence = disqualifiedPendingSentence;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public Address getAddress() {
        return address;
    }

    public LocalDate getDisqualifiedUntil() {
        return disqualifiedUntil;
    }

    public Boolean getDisqualifiedForLife() {
        return disqualifiedForLife;
    }

    public String getEyesight() {
        return eyesight;
    }

    public Boolean getImagesExist() {
        return imagesExist;
    }

    public Boolean getDisqualifiedPendingSentence() {
        return disqualifiedPendingSentence;
    }

    public Boolean getRetainedC1_D1Entitlement() {
        return retainedC1_D1Entitlement;
    }

    public List<PreviousDrivingLicence> getPreviousDrivingLicence() {
        return previousDrivingLicence;
    }
}

