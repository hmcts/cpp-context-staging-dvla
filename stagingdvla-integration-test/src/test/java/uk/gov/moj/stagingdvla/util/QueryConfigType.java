package uk.gov.moj.stagingdvla.util;

public enum QueryConfigType {

    PERSON_BY_ID("/stagingdvla/driver/", "application/vnd.stagingdvla.query.drivernumber+json");

    private final String urlSuffix;
    private final String mediaType;

    QueryConfigType(String urlSuffix, String mediaType) {
        this.urlSuffix = urlSuffix;
        this.mediaType = mediaType;
    }


    public String getUrlSuffix() {
        return urlSuffix;
    }

    public String getMediaType() {
        return mediaType;
    }

}
