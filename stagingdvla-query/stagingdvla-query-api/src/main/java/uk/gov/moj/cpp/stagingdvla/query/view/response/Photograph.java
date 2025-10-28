package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Photograph implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String image;

    private final String imageFormat;

    public Photograph(final String image, final String imageFormat) {
        this.image = image;
        this.imageFormat = imageFormat;
    }

    public String getImage() {
        return image;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public static Builder photograph() {
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

        public Builder withValuesFrom(final Photograph photograph) {
            this.image = photograph.getImage();
            this.imageFormat = photograph.getImageFormat();
            return this;
        }

        public Photograph build() {
            return new Photograph(image, imageFormat);
        }
    }
}
