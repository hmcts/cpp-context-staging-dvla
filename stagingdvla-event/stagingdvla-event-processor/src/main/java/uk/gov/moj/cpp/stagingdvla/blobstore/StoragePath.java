package uk.gov.moj.cpp.stagingdvla.blobstore;

public class StoragePath {

    private final String prefix;

    private StoragePath(final String prefix) {
        this.prefix = prefix;
    }

    public static StoragePath internal() {
        return new StoragePath("internal");
    }

    public String blobName(final String fileId) {
        return prefix + "/" + fileId;
    }

    @Override
    public String toString() {
        return prefix;
    }
}
