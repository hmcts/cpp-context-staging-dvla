package uk.gov.moj.cpp.stagingdvla.blobstore;

import static java.lang.Long.parseLong;
import static java.time.Duration.ofSeconds;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.util.LazyValue;

import java.time.Duration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * JNDI-backed configuration bean for Azure Blob Storage.
 *
 * <p>Reads per-application JNDI values from WildFly via the framework's
 * {@code @Value} annotation. {@code azure.filestore.endpoint} and
 * {@code azure.filestore.container-name} must always be present in
 * {@code standalone.xml}. {@code azure.filestore.connection-string} is
 * <strong>optional</strong>: when absent it defaults to the sentinel value
 * {@code "DefaultAzureCredential"}, which causes
 * {@link AzureFileStoreBlobContainerClientProducer} to authenticate via
 * {@code DefaultAzureCredential} (Workload Identity on AKS).
 *
 * <p>Authentication is selected at startup based on the connection string value:
 * <ul>
 *   <li><strong>Managed Identity (AKS with Workload Identity)</strong> — omit
 *       {@code azure.filestore.connection-string} entirely; {@code DefaultAzureCredential}
 *       activates automatically.</li>
 *   <li><strong>Environments not yet on Managed Identity</strong> — set
 *       {@code azure.filestore.connection-string} to the real Azure Storage account
 *       connection string; the code uses it directly without any code change.</li>
 *   <li><strong>Local development / integration tests (Azurite)</strong> — set
 *       {@code azure.filestore.connection-string} to the Azurite emulator connection
 *       string.</li>
 * </ul>
 *
 * <p>See {@code patterns/authentication.md} in {@code pe_arch_design_docs} for the full
 * transition guide and per-environment JNDI reference.
 */
@SuppressWarnings("java:S6813")
@ApplicationScoped
public class AzureFileStoreBlobConfiguration {

    @Inject
    @Value(key = "azure.filestore.connection-string", defaultValue = "DefaultAzureCredential")
    private String connectionString;

    @Inject
    @Value(key = "azure.filestore.endpoint", defaultValue = "")
    private String endpoint;

    @Inject
    @Value(key = "azure.filestore.container-name")
    private String containerName;

    @Inject
    @Value(key = "azure.filestore.connection-timeout-seconds", defaultValue = "10")
    private String connectionTimeoutSeconds;

    @Inject
    @Value(key = "azure.filestore.response-timeout-seconds", defaultValue = "30")
    private String responseTimeoutSeconds;

    @Inject
    @Value(key = "azure.filestore.transfer-timeout-seconds", defaultValue = "300")
    private String transferTimeoutSeconds;

    private final LazyValue hasConnectionStringLazyValue = new LazyValue();
    private final LazyValue connectionTimeoutLazyValue = new LazyValue();
    private final LazyValue responseTimeoutLazyValue = new LazyValue();
    private final LazyValue transferTimeoutLazyValue = new LazyValue();

    /**
     * Returns the raw {@code azure.filestore.connection-string} JNDI value.
     *
     * <p>When the JNDI entry is absent, this returns the sentinel value
     * {@code "DefaultAzureCredential"} — not a real connection string.
     *
     * <p>Use {@link #hasConnectionString()} to test whether a real connection
     * string has been configured, rather than inspecting this value directly.
     *
     * @return the connection string, or {@code "DefaultAzureCredential"} when no
     *         JNDI entry is present
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Returns {@code true} when a real Azurite connection string has been configured.
     *
     * <p>Returns {@code false} when the connection string is absent from JNDI
     * (defaulting to the {@code "DefaultAzureCredential"} sentinel), blank, or null.
     * In all {@code false} cases {@link AzureFileStoreBlobContainerClientProducer} authenticates
     * via {@code DefaultAzureCredential} (Workload Identity on AKS).
     *
     * @return {@code true} when a real connection string is present — either the
     *         Azurite emulator string (local dev / IT) or a real Azure Storage
     *         account connection string (environments not yet on Managed Identity)
     */
    public boolean hasConnectionString() {
        return hasConnectionStringLazyValue.createIfAbsent(() -> connectionString != null && !connectionString.isBlank() && !"DefaultAzureCredential".equals(connectionString));
    }

    /**
     * Returns the Azure Blob Storage service endpoint URL.
     *
     * <p>Used when {@link #hasConnectionString()} returns {@code false}, i.e. in
     * production where Workload Identity (Entra ID Federated Identity Credential)
     * is used for authentication.
     * Example: {@code https://mystorage.blob.core.windows.net}.
     *
     * @return the storage account endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Returns the name of the blob container owned by this service.
     *
     * <p>Each CPP service owns exactly one container. For reference-data this is
     * {@code referencedata-files} (local) or {@code referencedata-files-{env}} (AKS).
     * The container is created at startup via {@code createIfNotExists()} if it does
     * not already exist.
     *
     * @return the container name
     */
    public String getContainerName() {
        return containerName;
    }

    /**
     * Timeout for establishing a TCP connection to the Azure Storage endpoint.
     *
     * <p>Configurable via JNDI key {@code azure.filestore.connection-timeout-seconds}.
     * Default 10 s.
     *
     * @return the TCP connection timeout
     */
    public Duration getConnectionTimeout() {
        return  connectionTimeoutLazyValue.createIfAbsent(() -> ofSeconds(parseLong(connectionTimeoutSeconds)));
    }

    /**
     * Timeout for receiving response headers after a request is sent.
     *
     * <p>Covers control-plane calls ({@code exists()}, {@code getProperties()}).
     * Configurable via JNDI key {@code azure.filestore.response-timeout-seconds}.
     * Default 30 s.
     *
     * @return the response header timeout
     */
    public Duration getResponseTimeout() {
        return responseTimeoutLazyValue.createIfAbsent(() -> ofSeconds(parseLong(responseTimeoutSeconds)));
    }

    /**
     * Overall wall-clock deadline for data transfer operations — upload body,
     * download body, and server-side copy.
     *
     * <p>Configurable via JNDI key {@code azure.filestore.transfer-timeout-seconds}.
     * Default 300 s. Raise for services handling blobs significantly larger than a few MB.
     *
     * @return the transfer timeout
     */
    public Duration getTransferTimeout() {
        return transferTimeoutLazyValue.createIfAbsent(() -> ofSeconds(parseLong(transferTimeoutSeconds)));
    }
}
