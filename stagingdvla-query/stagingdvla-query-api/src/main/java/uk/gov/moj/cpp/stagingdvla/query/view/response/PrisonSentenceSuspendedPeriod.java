package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class PrisonSentenceSuspendedPeriod implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer years;
    private Integer months;
    private Integer days;
    private Integer hours;

    public static class Builder {
        private Integer years;
        private Integer months;
        private Integer days;
        private Integer hours;

        public Builder years(Integer years) {
            this.years = years;
            return this;
        }

        public Builder months(Integer months) {
            this.months = months;
            return this;
        }

        public Builder days(Integer days) {
            this.days = days;
            return this;
        }

        public Builder hours(Integer hours) {
            this.hours = hours;
            return this;
        }

        public PrisonSentenceSuspendedPeriod build() {
            return new PrisonSentenceSuspendedPeriod(years, months, days, hours);
        }
    }

    private PrisonSentenceSuspendedPeriod(Integer years, Integer months, Integer days, Integer hours) {
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
    }

    public PrisonSentenceSuspendedPeriod(){}

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }

    public Integer getDays() {
        return days;
    }

    public Integer getHours() {
        return hours;
    }

}
