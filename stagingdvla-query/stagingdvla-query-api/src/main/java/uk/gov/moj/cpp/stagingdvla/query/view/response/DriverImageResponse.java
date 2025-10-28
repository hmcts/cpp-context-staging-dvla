package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class DriverImageResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String count;

    private Error error;

    private Photograph photograph;

    private Signature signature;

    public DriverImageResponse(final Error error, final Photograph photograph, final Signature signature) {
        this.error = error;
        this.photograph = photograph;
        this.signature = signature;
    }

    public DriverImageResponse(){}

    public String getCount() {
        return count;
    }

    public Error getError() {
        return error;
    }

    public Photograph getPhotograph() {
        return photograph;
    }

    public Signature getSignature() {
        return signature;
    }

    public static final class Builder {
        private Photograph photograph;
        private Signature signature;
        private Error error;

        public Builder error(final Error error) {
            this.error = error;
            return this;
        }
        public Builder photograph(final Photograph photograph) {
            this.photograph = photograph;
            return this;
        }

        public Builder signature(final Signature signature) {
            this.signature = signature;
            return this;
        }

        public DriverImageResponse build() {
            return new DriverImageResponse(error, photograph, signature);
        }
    }
}
