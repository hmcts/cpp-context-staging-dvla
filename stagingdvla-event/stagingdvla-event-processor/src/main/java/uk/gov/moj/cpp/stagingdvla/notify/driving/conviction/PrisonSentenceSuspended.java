package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class PrisonSentenceSuspended implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer years;

    private Integer months;

    private Integer days;

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }

    public Integer getDays() {
        return days;
    }

    public static Builder prisonSentenceSuspended() {
        return new PrisonSentenceSuspended.Builder();
    }

    @Override
    public String toString() {
        return "PrisonSentenceSuspended{" +
                "years=" + years +
                ", months=" + months +
                ", days=" + days +
                '}';
    }

    public static final class Builder {
        private Integer years;
        private Integer months;
        private Integer days;

        private Builder() {
        }

        public static Builder aPrisonSentenceSuspended() {
            return new Builder();
        }

        public Builder withYears(Integer years) {
            this.years = years;
            return this;
        }

        public Builder withMonths(Integer months) {
            this.months = months;
            return this;
        }

        public Builder withDays(Integer days) {
            this.days = days;
            return this;
        }

        public PrisonSentenceSuspended build() {
            final PrisonSentenceSuspended prisonSentenceSuspended = new PrisonSentenceSuspended();
            prisonSentenceSuspended.years = this.years;
            prisonSentenceSuspended.months = this.months;
            prisonSentenceSuspended.days = this.days;
            return prisonSentenceSuspended;
        }
    }
}
