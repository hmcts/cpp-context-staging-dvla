package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class Disqualification implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer years;

    private Integer months;

    private Integer days;

    private Boolean forLife;

    private String extendedType;

    public Integer getYears() {
        return years;
    }

    public Integer getMonths() {
        return months;
    }

    public Integer getDays() {
        return days;
    }

    public Boolean getForLife() {
        return forLife;
    }

    public String getExtendedType() {
        return extendedType;
    }

    public static Builder disqualification() {
        return new Disqualification.Builder();
    }

    @Override
    public String toString() {
        return "Disqualification{" +
                "years=" + years +
                ", months=" + months +
                ", days=" + days +
                ", forLife=" + forLife +
                ", extendedType='" + extendedType + '\'' +
                '}';
    }

    public static final class Builder {
        private Integer years;
        private Integer months;
        private Integer days;
        private Boolean forLife;
        private String extendedType;

        private Builder() {
        }

        public static Builder aDisqualification() {
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

        public Builder withForLife(Boolean forLife) {
            this.forLife = forLife;
            return this;
        }

        public Builder withExtendedType(String extendedType) {
            this.extendedType = extendedType;
            return this;
        }

        public Disqualification build() {
            final Disqualification disqualification = new Disqualification();
            disqualification.days = this.days;
            disqualification.months = this.months;
            disqualification.forLife = this.forLife;
            disqualification.years = this.years;
            disqualification.extendedType = this.extendedType;
            return disqualification;
        }
    }
}
