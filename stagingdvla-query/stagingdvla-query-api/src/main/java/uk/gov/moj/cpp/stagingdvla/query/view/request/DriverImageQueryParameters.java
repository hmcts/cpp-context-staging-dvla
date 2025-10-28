package uk.gov.moj.cpp.stagingdvla.query.view.request;

public class DriverImageQueryParameters {
    private final String driverNumber;

    private final String requiredImage;

    public DriverImageQueryParameters(final String driverNumber, final String requiredImage) {
        this.driverNumber = driverNumber;
        this.requiredImage = requiredImage;
    }

    public String getDriverNumber() {
        return driverNumber;
    }

    public String getRequiredImage() {
        return requiredImage;
    }

    public static Builder driverImageParameters() {
        return new DriverImageQueryParameters.Builder();
    }

    @Override
    public String toString() {
        return "DriverImageParameters{" +
                "driverNumber='" + driverNumber + "'," +
                "requiredImage='" + requiredImage + "'" +
                "}";
    }

    public static class Builder {
        private String driverNumber;

        private String requiredImage;

        public Builder driverNumber(final String driverNumber) {
            this.driverNumber = driverNumber;
            return this;
        }

        public Builder requiredImage(final String requiredImage) {
            this.requiredImage = requiredImage;
            return this;
        }

        public Builder withValuesFrom(final DriverImageQueryParameters driverImageParameters) {
            this.driverNumber = driverImageParameters.getDriverNumber();
            this.requiredImage = driverImageParameters.getRequiredImage();
            return this;
        }

        public DriverImageQueryParameters build() {
            return new DriverImageQueryParameters(driverNumber, requiredImage);
        }
    }
}
