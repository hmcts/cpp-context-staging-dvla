package uk.gov.moj.cpp.stagingdvla.blobstore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.HttpResponse;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class AzureFileStoreBlobContainerClientProducerTest {

    private static final String CONTAINER_NAME = "test-container";
    private static final String NON_SENTINEL_CONNECTION_STRING_VALUE = "some-configured-value";

    private final Logger logger = mock(Logger.class);
    private final BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
    private final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();

    @BeforeEach
    public void setUp() {
        setField(configuration, "containerName", CONTAINER_NAME);
        setField(configuration, "connectionTimeoutSeconds", "10");
        setField(configuration, "responseTimeoutSeconds", "30");
    }

    @Test
    public void shouldExposeTheBlobContainerClientBuiltDuringInitialise() {
        final TestableProducer producer = newTestableProducer();
        when(blobContainerClient.createIfNotExists()).thenReturn(true);

        producer.initialise();

        assertThat(producer.blobContainerClient(), sameInstance(blobContainerClient));
    }

    @Test
    public void shouldSwallow409ConflictAndLogWarningWhenContainerAlreadyExists() {
        final TestableProducer producer = newTestableProducer();
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(409);
        when(blobContainerClient.createIfNotExists()).thenThrow(new HttpResponseException("conflict", httpResponse));

        producer.initialise();

        verify(logger).warn(anyString(), eq(CONTAINER_NAME));
        assertThat(producer.blobContainerClient(), sameInstance(blobContainerClient));
    }

    @Test
    public void shouldWrapNonConflictHttpErrorsInCreationException() {
        final TestableProducer producer = newTestableProducer();
        final HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(500);
        final HttpResponseException cause = new HttpResponseException("server error", httpResponse);
        when(blobContainerClient.createIfNotExists()).thenThrow(cause);

        final AzureBlobContainerClientCreationException exception =
                assertThrows(AzureBlobContainerClientCreationException.class, producer::initialise);

        assertThat(exception.getCause(), sameInstance((Throwable) cause));
        assertThat(exception.getMessage().contains(CONTAINER_NAME), is(true));
        verify(logger, never()).warn(anyString(), eq(CONTAINER_NAME));
    }

    @Test
    public void shouldWrapHttpErrorsWithoutAResponseInCreationException() {
        final TestableProducer producer = newTestableProducer();
        final HttpResponseException cause = new HttpResponseException("no response available", null);
        when(blobContainerClient.createIfNotExists()).thenThrow(cause);

        final AzureBlobContainerClientCreationException exception =
                assertThrows(AzureBlobContainerClientCreationException.class, producer::initialise);

        assertThat(exception.getCause(), sameInstance((Throwable) cause));
    }

    @Test
    public void shouldBuildBlobContainerClientUsingDefaultAzureCredentialWhenNoConnectionStringConfigured() {
        setField(configuration, "endpoint", "https://mystorage.blob.core.windows.net");

        final AzureFileStoreBlobContainerClientProducer producer = new AzureFileStoreBlobContainerClientProducer();

        final BlobContainerClient client = producer.buildBlobContainerClient(configuration);

        assertThat(client.getBlobContainerName(), is(CONTAINER_NAME));
    }

    @Test
    public void shouldAttemptConnectionStringAuthenticationWhenAConnectionStringIsConfigured() {
        setField(configuration, "connectionString", NON_SENTINEL_CONNECTION_STRING_VALUE);

        final AzureFileStoreBlobContainerClientProducer producer = new AzureFileStoreBlobContainerClientProducer();

        // the value is not a real connection string, so the SDK rejects it while parsing -
        // this proves the connection-string branch (rather than the DefaultAzureCredential/
        // endpoint branch, which does not attempt to parse it) was taken.
        assertThrows(IllegalArgumentException.class, () -> producer.buildBlobContainerClient(configuration));
    }

    private TestableProducer newTestableProducer() {
        final TestableProducer producer = new TestableProducer(blobContainerClient);
        setField(producer, "logger", logger);
        setField(producer, "azureBlobConfiguration", configuration);
        return producer;
    }

    private static final class TestableProducer extends AzureFileStoreBlobContainerClientProducer {

        private final BlobContainerClient client;

        private TestableProducer(final BlobContainerClient client) {
            this.client = client;
        }

        @Override
        protected BlobContainerClient buildBlobContainerClient(final AzureFileStoreBlobConfiguration configuration) {
            return client;
        }
    }
}
