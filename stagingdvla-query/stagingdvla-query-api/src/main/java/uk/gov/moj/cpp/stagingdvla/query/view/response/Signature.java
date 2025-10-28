package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Signature implements Serializable {

    private static final long serialVersionUID = 1L;
    private final String image;

    private final String imageFormat;

    public Signature(final String image, final String imageFormat) {
        this.image = image;
        this.imageFormat = imageFormat;
    }

    public String getImage() {
        return image;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public static Builder signature() {
        return new Builder();
    }

    public static class Builder {
        private String image;

        private String imageFormat;

        public Builder withImage(final String image) {
            this.image = image;
            return this;
        }

        public Builder withImageFormat(final String imageFormat) {
            this.imageFormat = imageFormat;
            return this;
        }

        public Builder withValuesFrom(final Signature signature) {
            this.image = signature.getImage();
            this.imageFormat = signature.getImageFormat();
            return this;
        }

        public Signature build() {
            return new Signature(image, imageFormat);
        }
    }
}
