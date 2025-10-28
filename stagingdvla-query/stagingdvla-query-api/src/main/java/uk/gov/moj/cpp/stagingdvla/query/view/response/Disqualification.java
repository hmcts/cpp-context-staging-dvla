package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Disqualification implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private Boolean forLife;
    private Integer years;
    private Integer months;
    private Integer days;

    public static class Builder {
        private String type;
        private Boolean forLife;
        private Integer years;
        private Integer months;
        private Integer days;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder forLife(Boolean forLife) {
            this.forLife = forLife;
            return this;
        }

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

        public Disqualification build() {
            return new Disqualification(type, forLife, years, months, days);
        }
    }

    private Disqualification(String type, Boolean forLife, Integer years, Integer months, Integer days) {
        this.type = type;
        this.forLife = forLife;
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public Disqualification(){}

    public String getType() {
        return type;
    }

    public Boolean getForLife() {
        return forLife;
    }

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }

    public Integer getDays() {
        return days;
    }
}
