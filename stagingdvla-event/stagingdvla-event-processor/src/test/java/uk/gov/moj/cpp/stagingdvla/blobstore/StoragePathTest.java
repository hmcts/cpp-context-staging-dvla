package uk.gov.moj.cpp.stagingdvla.blobstore;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class StoragePathTest {

    @Test
    public void shouldBuildBlobNameFromInternalPrefixAndFileId() {
        final UUID fileId = randomUUID();

        final StoragePath storagePath = StoragePath.internal();

        assertThat(storagePath.blobName(fileId.toString()), is("internal/" + fileId));
    }

    @Test
    public void shouldReturnDifferentBlobNamesForDifferentFileIds() {
        final StoragePath storagePath = StoragePath.internal();

        final String firstBlobName = storagePath.blobName(randomUUID().toString());
        final String secondBlobName = storagePath.blobName(randomUUID().toString());

        assertThat(firstBlobName.equals(secondBlobName), is(false));
    }

    @Test
    public void shouldExposePrefixViaToString() {
        assertThat(StoragePath.internal().toString(), is("internal"));
    }
}
