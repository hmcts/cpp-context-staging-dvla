package uk.gov.moj.cpp.stagingdvla.blobstore;

import static java.time.Duration.ofSeconds;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AzureFileStoreBlobConfigurationTest {

    private static final String SOME_NON_SENTINEL_VALUE = "some-configured-value";

    private final AzureFileStoreBlobConfiguration configuration = new AzureFileStoreBlobConfiguration();

    @BeforeEach
    public void setUp() {
        setField(configuration, "containerName", "referencedata-files");
        setField(configuration, "endpoint", "https://mystorage.blob.core.windows.net");
        setField(configuration, "connectionTimeoutSeconds", "10");
        setField(configuration, "responseTimeoutSeconds", "30");
        setField(configuration, "transferTimeoutSeconds", "300");
    }

    @Test
    public void shouldReturnRawConnectionStringValue() {
        setField(configuration, "connectionString", SOME_NON_SENTINEL_VALUE);

        assertThat(configuration.getConnectionString(), is(SOME_NON_SENTINEL_VALUE));
    }

    @Test
    public void shouldReturnConfiguredEndpoint() {
        assertThat(configuration.getEndpoint(), is("https://mystorage.blob.core.windows.net"));
    }

    @Test
    public void shouldReturnConfiguredContainerName() {
        assertThat(configuration.getContainerName(), is("referencedata-files"));
    }

    @Test
    public void shouldTreatANonSentinelConnectionStringAsPresent() {
        setField(configuration, "connectionString", SOME_NON_SENTINEL_VALUE);

        assertThat(configuration.hasConnectionString(), is(true));
    }

    @Test
    public void shouldTreatAbsentConnectionStringAsNotPresent() {
        setField(configuration, "connectionString", null);

        assertThat(configuration.hasConnectionString(), is(false));
    }

    @Test
    public void shouldTreatBlankConnectionStringAsNotPresent() {
        setField(configuration, "connectionString", "   ");

        assertThat(configuration.hasConnectionString(), is(false));
    }

    @Test
    public void shouldTreatSentinelConnectionStringAsNotPresent() {
        setField(configuration, "connectionString", "DefaultAzureCredential");

        assertThat(configuration.hasConnectionString(), is(false));
    }

    @Test
    public void shouldCacheHasConnectionStringResultAcrossCalls() {
        setField(configuration, "connectionString", "DefaultAzureCredential");
        assertThat(configuration.hasConnectionString(), is(false));

        // mutate the backing field directly - the cached lazy value must not be recomputed
        setField(configuration, "connectionString", SOME_NON_SENTINEL_VALUE);

        assertThat(configuration.hasConnectionString(), is(false));
    }

    @Test
    public void shouldParseConnectionTimeoutInSeconds() {
        assertThat(configuration.getConnectionTimeout(), is(ofSeconds(10)));
    }

    @Test
    public void shouldParseResponseTimeoutInSeconds() {
        assertThat(configuration.getResponseTimeout(), is(ofSeconds(30)));
    }

    @Test
    public void shouldParseTransferTimeoutInSeconds() {
        assertThat(configuration.getTransferTimeout(), is(ofSeconds(300)));
    }
}
