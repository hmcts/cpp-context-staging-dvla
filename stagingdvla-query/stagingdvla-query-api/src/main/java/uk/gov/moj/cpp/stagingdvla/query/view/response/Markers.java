package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Markers implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean declaredHardship;

    public static class Builder {
        private Boolean declaredHardship;

        public Builder declaredHardship(Boolean declaredHardship) {
            this.declaredHardship = declaredHardship;
            return this;
        }

        public Markers build() {
            return new Markers(declaredHardship);
        }
    }

    private Markers(Boolean declaredHardship) {
        this.declaredHardship = declaredHardship;
    }

    public Markers(){}

    public Boolean getDeclaredHardship() {
        return declaredHardship;
    }
}
