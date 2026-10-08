package uk.gov.moj.cpp.stagingdvla.blobstore;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.jupiter.api.Test;

public class AzureFileStoreBlobConfigurationTest {

    @Test
    public void shouldBeAzuriteEnabledWhenFlagIsTrue() {
        final AzureFileStoreBlobConfiguration configuration = configurationWith("true", "10", "30", "300");

        assertThat(configuration.isAzuriteEnabled(), is(true));
    }

    @Test
    public void shouldNotBeAzuriteEnabledWhenFlagIsFalse() {
        final AzureFileStoreBlobConfiguration configuration = configurationWith("false", "10", "30", "300");

        assertThat(configuration.isAzuriteEnabled(), is(false));
    }

    @Test
    public void shouldNotBeAzuriteEnabledWhenFlagIsMissing() {
        final AzureFileStoreBlobConfiguration configuration = configurationWith(null, "10", "30", "300");

        assertThat(configuration.isAzuriteEnabled(), is(false));
    }

    @Test
    public void shouldReturnConnectionString() {
        final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();
        setField(configuration, "connectionString", "some-connection-string");

        assertThat(configuration.getConnectionString(), is("some-connection-string"));
    }

    @Test
    public void shouldReturnConfiguredEndpoint() {
        final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();
        setField(configuration, "endpoint", "https://mystorage.blob.core.windows.net");

        assertThat(configuration.getEndpoint(), is("https://mystorage.blob.core.windows.net"));
    }

    @Test
    public void shouldReturnConfiguredContainerName() {
        final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();
        setField(configuration, "containerName", "stagingdvla-files");

        assertThat(configuration.getContainerName(), is("stagingdvla-files"));
    }

    @Test
    public void shouldReturnConfiguredTimeouts() {
        final AzureFileStoreBlobConfiguration configuration = configurationWith("false", "7", "11", "99");

        assertThat(configuration.getConnectionTimeout(), is(ofSeconds(7)));
        assertThat(configuration.getResponseTimeout(), is(ofSeconds(11)));
        assertThat(configuration.getTransferTimeout(), is(ofSeconds(99)));
    }

    private AzureFileStoreBlobConfiguration configurationWith(final String azuriteEnabled,
                                                               final String connectionTimeoutSeconds,
                                                               final String responseTimeoutSeconds,
                                                               final String transferTimeoutSeconds) {
        final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();
        setField(configuration, "azuriteEnabled", azuriteEnabled);
        setField(configuration, "connectionTimeoutSeconds", connectionTimeoutSeconds);
        setField(configuration, "responseTimeoutSeconds", responseTimeoutSeconds);
        setField(configuration, "transferTimeoutSeconds", transferTimeoutSeconds);
        return configuration;
    }
}
