package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class DriverImage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Photograph photograph;

    private final Signature signature;

    public DriverImage(final Photograph photograph, final Signature signature) {
        this.photograph = photograph;
        this.signature = signature;
    }

    public Photograph getPhotograph() {
        return photograph;
    }

    public Signature getSignature() {
        return signature;
    }

    public static Builder driverimage() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "Driverimage{" +
                "photograph='" + photograph + "'," +
                "signature='" + signature + "'" +
                "}";
    }

    public static class Builder {
        private Photograph photograph;

        private Signature signature;

        public Builder withPhotograph(final Photograph photograph) {
            this.photograph = photograph;
            return this;
        }

        public Builder withSignature(final Signature signature) {
            this.signature = signature;
            return this;
        }

        public Builder withValuesFrom(final DriverImage driverimage) {
            this.photograph = driverimage.getPhotograph();
            this.signature = driverimage.getSignature();
            return this;
        }

        public DriverImage build() {
            return new DriverImage(photograph, signature);
        }
    }
}
