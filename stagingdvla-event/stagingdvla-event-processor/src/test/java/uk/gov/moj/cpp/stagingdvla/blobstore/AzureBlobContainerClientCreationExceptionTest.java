package uk.gov.moj.cpp.stagingdvla.blobstore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class AzureBlobContainerClientCreationExceptionTest {

    @Test
    public void shouldCarryMessageAndCause() {
        final Throwable cause = new RuntimeException("root cause");

        final AzureBlobContainerClientCreationException exception =
                new AzureBlobContainerClientCreationException("failed to create container", cause);

        assertThat(exception.getMessage(), is("failed to create container"));
        assertThat(exception.getCause(), sameInstance(cause));
    }

    @Test
    public void shouldBeARuntimeException() {
        final AzureBlobContainerClientCreationException exception =
                new AzureBlobContainerClientCreationException("boom", null);

        assertThat(exception, is(org.hamcrest.CoreMatchers.instanceOf(RuntimeException.class)));
    }
}
