package uk.gov.moj.cpp.stagingdvla.service;

public enum ConversionFormat {
    PDF("pdf");

    private final String value;

    ConversionFormat(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
