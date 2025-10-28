package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class OtherSentence implements Serializable {
    private static final long serialVersionUID = 1L;

    private String otherSentenceType;

    private Integer years;

    private Integer months;

    private Integer weeks;

    private Integer days;

    private Integer hours;

    public String getOtherSentenceType() {
        return otherSentenceType;
    }

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }


    public Integer getWeeks() {
        return weeks;
    }

    public Integer getDays() {
        return days;
    }

    public Integer getHours() {
        return hours;
    }

    public static Builder otherSentence() {
        return new OtherSentence.Builder();
    }

    @Override
    public String toString() {
        return "OtherSentence{" +
                "otherSentenceType='" + otherSentenceType + '\'' +
                ", years=" + years +
                ", months=" + months +
                ", weeks=" + weeks +
                ", days=" + days +
                ", hours=" + hours +
                '}';
    }


    public static final class Builder {
        private String otherSentenceType;
        private Integer years;
        private Integer months;
        private Integer weeks;
        private Integer days;
        private Integer hours;

        private Builder() {
        }

        public static Builder anOtherSentence() {
            return new Builder();
        }

        public Builder withOtherSentenceType(String otherSentenceType) {
            this.otherSentenceType = otherSentenceType;
            return this;
        }

        public Builder withYears(Integer years) {
            this.years = years;
            return this;
        }

        public Builder withMonths(Integer months) {
            this.months = months;
            return this;
        }

        public Builder withWeeks(Integer weeks) {
            this.weeks = weeks;
            return this;
        }

        public Builder withDays(Integer days) {
            this.days = days;
            return this;
        }

        public Builder withHours(Integer hours) {
            this.hours = hours;
            return this;
        }

        public OtherSentence build() {
            final OtherSentence otherSentence = new OtherSentence();
            otherSentence.hours = this.hours;
            otherSentence.otherSentenceType = this.otherSentenceType;
            otherSentence.months = this.months;
            otherSentence.weeks = this.weeks;
            otherSentence.days = this.days;
            otherSentence.years = this.years;
            return otherSentence;
        }
    }
}
