package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Intoxicant implements Serializable {

    private static final long serialVersionUID = 1L;

    private String intoxicantType;
    private String testingMethod;
    private Integer level;
    private String unitType;

    public static class Builder {
        private String intoxicantType;
        private String testingMethod;
        private Integer level;
        private String unitType;

        public Builder intoxicantType(String intoxicantType) {
            this.intoxicantType = intoxicantType;
            return this;
        }

        public Builder testingMethod(String testingMethod) {
            this.testingMethod = testingMethod;
            return this;
        }

        public Builder level(Integer level) {
            this.level = level;
            return this;
        }

        public Builder unitType(String unitType) {
            this.unitType = unitType;
            return this;
        }

        public Intoxicant build() {
            return new Intoxicant(intoxicantType, testingMethod, level, unitType);
        }
    }

    private Intoxicant(String intoxicantType, String testingMethod, Integer level, String unitType) {
        this.intoxicantType = intoxicantType;
        this.testingMethod = testingMethod;
        this.level = level;
        this.unitType = unitType;
    }

    public Intoxicant(){}

    public String getIntoxicantType() {
        return intoxicantType;
    }

    public String getTestingMethod() {
        return testingMethod;
    }

    public Integer getLevel() {
        return level;
    }

    public String getUnitType() {
        return unitType;
    }
}

