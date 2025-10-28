package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;
import java.math.BigDecimal;

public class Offence implements Serializable {
    private static final long serialVersionUID = 1L;

    private String offenceCode;

    private String dateOfOffence;

    private Integer fine;

    private Integer penaltyPoints;

    private String intoxicantType;

    private String testingMethod;

    private BigDecimal testingResultLevel;

    private String testingResultUnits;

    private Disqualification disqualification;

    private OtherSentence otherSentence;

    private PrisonSentenceSuspended prisonSentenceSuspended;

    private String sentenceState;

    private String sentencingCourtCode;

    private String sentenceDate;

    private String disqualificationRemovalDate;

    private String disqualificationReimposedDate;

    private String disqualificationSuspendedPendingAppealDate;

    private String appealCourtCode;

    private String appealDate;

    private Boolean hardshipClaimed;

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getDateOfOffence() {
        return dateOfOffence;
    }

    public Integer getFine() {
        return fine;
    }

    public Integer getPenaltyPoints() {
        return penaltyPoints;
    }

    public String getIntoxicantType() {
        return intoxicantType;
    }

    public String getTestingMethod() {
        return testingMethod;
    }

    public BigDecimal getTestingResultLevel() {
        return testingResultLevel;
    }

    public String getTestingResultUnits() {
        return testingResultUnits;
    }

    public Disqualification getDisqualification() {
        return disqualification;
    }

    public OtherSentence getOtherSentence() {
        return otherSentence;
    }

    public PrisonSentenceSuspended getPrisonSentenceSuspended() {
        return prisonSentenceSuspended;
    }

    public String getSentenceState() {
        return sentenceState;
    }

    public String getSentencingCourtCode() {
        return sentencingCourtCode;
    }

    public String getSentenceDate() {
        return sentenceDate;
    }

    public String getDisqualificationRemovalDate() {
        return disqualificationRemovalDate;
    }

    public String getDisqualificationReimposedDate() {
        return disqualificationReimposedDate;
    }

    public String getDisqualificationSuspendedPendingAppealDate() {
        return disqualificationSuspendedPendingAppealDate;
    }

    public String getAppealCourtCode() {
        return appealCourtCode;
    }

    public String getAppealDate() {
        return appealDate;
    }

    public Boolean getHardshipClaimed() {
        return hardshipClaimed;
    }

    public static Builder offence() {
        return new Offence.Builder();
    }

    @Override
    public String toString() {
        return "Offence{" +
                "offenceCode='" + offenceCode + '\'' +
                ", dateOfOffence='" + dateOfOffence + '\'' +
                ", fine=" + fine +
                ", penaltyPoints=" + penaltyPoints +
                ", intoxicantType='" + intoxicantType + '\'' +
                ", testingMethod='" + testingMethod + '\'' +
                ", testingResultLevel=" + testingResultLevel +
                ", testingResultUnits='" + testingResultUnits + '\'' +
                ", disqualification=" + disqualification +
                ", otherSentence=" + otherSentence +
                ", prisonSentenceSuspended=" + prisonSentenceSuspended +
                ", sentenceState='" + sentenceState + '\'' +
                ", sentencingCourtCode='" + sentencingCourtCode + '\'' +
                ", sentenceDate='" + sentenceDate + '\'' +
                ", disqualificationRemovalDate='" + disqualificationRemovalDate + '\'' +
                ", disqualificationReimposedDate='" + disqualificationReimposedDate + '\'' +
                ", disqualificationSuspendedPendingAppealDate='" + disqualificationSuspendedPendingAppealDate + '\'' +
                ", appealCourtCode='" + appealCourtCode + '\'' +
                ", appealDate='" + appealDate + '\'' +
                ", hardshipClaimed=" + hardshipClaimed +
                '}';
    }

    public static final class Builder {
        private String offenceCode;
        private String dateOfOffence;
        private Integer fine;
        private Integer penaltyPoints;
        private String intoxicantType;
        private String testingMethod;
        private BigDecimal testingResultLevel;
        private String testingResultUnits;
        private Disqualification disqualification;
        private OtherSentence otherSentence;
        private PrisonSentenceSuspended prisonSentenceSuspended;
        private String sentenceState;
        private String sentencingCourtCode;
        private String sentenceDate;
        private String disqualificationRemovalDate;
        private String disqualificationReimposedDate;
        private String disqualificationSuspendedPendingAppealDate;
        private String appealCourtCode;
        private String appealDate;
        private Boolean hardshipClaimed;

        private Builder() {
        }

        public static Builder anOffence() {
            return new Builder();
        }

        public Builder withOffenceCode(String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder withDateOfOffence(String dateOfOffence) {
            this.dateOfOffence = dateOfOffence;
            return this;
        }

        public Builder withFine(Integer fine) {
            this.fine = fine;
            return this;
        }

        public Builder withPenaltyPoints(Integer penaltyPoints) {
            this.penaltyPoints = penaltyPoints;
            return this;
        }

        public Builder withIntoxicantType(String intoxicantType) {
            this.intoxicantType = intoxicantType;
            return this;
        }

        public Builder withTestingMethod(String testingMethod) {
            this.testingMethod = testingMethod;
            return this;
        }

        public Builder withTestingResultLevel(BigDecimal testingResultLevel) {
            this.testingResultLevel = testingResultLevel;
            return this;
        }

        public Builder withTestingResultUnits(String testingResultUnits) {
            this.testingResultUnits = testingResultUnits;
            return this;
        }

        public Builder withDisqualification(Disqualification disqualification) {
            this.disqualification = disqualification;
            return this;
        }

        public Builder withOtherSentence(OtherSentence otherSentence) {
            this.otherSentence = otherSentence;
            return this;
        }

        public Builder withPrisonSentenceSuspended(PrisonSentenceSuspended prisonSentenceSuspended) {
            this.prisonSentenceSuspended = prisonSentenceSuspended;
            return this;
        }

        public Builder withSentenceState(String sentenceState) {
            this.sentenceState = sentenceState;
            return this;
        }

        public Builder withSentencingCourtCode(String sentencingCourtCode) {
            this.sentencingCourtCode = sentencingCourtCode;
            return this;
        }

        public Builder withSentenceDate(String sentenceDate) {
            this.sentenceDate = sentenceDate;
            return this;
        }

        public Builder withDisqualificationRemovalDate(String disqualificationRemovalDate) {
            this.disqualificationRemovalDate = disqualificationRemovalDate;
            return this;
        }

        public Builder withDisqualificationReimposedDate(String disqualificationReimposedDate) {
            this.disqualificationReimposedDate = disqualificationReimposedDate;
            return this;
        }

        public Builder withDisqualificationSuspendedPendingAppealDate(String disqualificationSuspendedPendingAppealDate) {
            this.disqualificationSuspendedPendingAppealDate = disqualificationSuspendedPendingAppealDate;
            return this;
        }

        public Builder withAppealCourtCode(String appealCourtCode) {
            this.appealCourtCode = appealCourtCode;
            return this;
        }

        public Builder withAppealDate(String appealDate) {
            this.appealDate = appealDate;
            return this;
        }

        public Builder withHardshipClaimed(Boolean hardshipClaimed) {
            this.hardshipClaimed = hardshipClaimed;
            return this;
        }

        public Offence build() {
            final Offence offence = new Offence();
            offence.appealDate = this.appealDate;
            offence.testingResultLevel = this.testingResultLevel;
            offence.disqualificationRemovalDate = this.disqualificationRemovalDate;
            offence.sentenceState = this.sentenceState;
            offence.disqualification = this.disqualification;
            offence.appealCourtCode = this.appealCourtCode;
            offence.hardshipClaimed = this.hardshipClaimed;
            offence.testingMethod = this.testingMethod;
            offence.testingResultUnits = this.testingResultUnits;
            offence.penaltyPoints = this.penaltyPoints;
            offence.otherSentence = this.otherSentence;
            offence.prisonSentenceSuspended = this.prisonSentenceSuspended;
            offence.dateOfOffence = this.dateOfOffence;
            offence.intoxicantType = this.intoxicantType;
            offence.fine = this.fine;
            offence.offenceCode = this.offenceCode;
            offence.disqualificationSuspendedPendingAppealDate = this.disqualificationSuspendedPendingAppealDate;
            offence.sentenceDate = this.sentenceDate;
            offence.disqualificationReimposedDate = this.disqualificationReimposedDate;
            offence.sentencingCourtCode = this.sentencingCourtCode;
            return offence;
        }
    }
}
