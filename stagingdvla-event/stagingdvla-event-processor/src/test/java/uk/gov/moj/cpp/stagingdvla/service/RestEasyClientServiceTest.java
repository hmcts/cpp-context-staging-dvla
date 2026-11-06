package uk.gov.moj.cpp.stagingdvla.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RestEasyClientServiceTest {

    @InjectMocks
    private RestEasyClientService restEasyClientService;

    @Mock
    private ResteasyClient client;

    @Mock
    ResteasyWebTarget target;

    @Mock
    Invocation.Builder request;

    @Mock
    jakarta.ws.rs.core.Response expectedResponse;

    @Test
    public void testPost() {
        final String url = "test-url";
        final String payload = "test-payload";
        final String key = "test-apim-key";

        when(client.target(any(String.class))).thenReturn(target);
        when(target.request()).thenReturn(request);
        when(request.post(any(Entity.class))).thenReturn(expectedResponse);
        when(expectedResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        Response actualResponse = restEasyClientService.post(url, payload, key);

        verify(request, times(1)).headers(any(MultivaluedHashMap.class));
        assertEquals(expectedResponse.getStatus(), actualResponse.getStatus());
        assertEquals(HttpServletResponse.SC_OK, actualResponse.getStatus());
    }

    // CHD-05: the DVLA client posts over HTTPS with trust and hostname checks off, sending the APIM headers.
    @Test
    public void chd05_shouldPostJsonWithApimHeadersOverHttpsToAServerWithAnUntrustedCertificateForAnotherHost() throws Exception {
        final AtomicReference<Headers> receivedHeaders = new AtomicReference<>();
        final AtomicReference<String> receivedBody = new AtomicReference<>();
        final HttpsServer server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContextWithSelfSignedCertificateFor("untrusted.example.invalid")));
        server.createContext("/notify-driving-conviction", exchange -> {
            receivedHeaders.set(exchange.getRequestHeaders());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), UTF_8));
            final byte[] response = "{\"status\":\"accepted\"}".getBytes(UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(HttpServletResponse.SC_OK, response.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(response);
            }
        });
        server.start();
        try {
            final RestEasyClientService service = realRestEasyClientServiceWithPoolSizeFrom("5");
            final String url = "https://localhost:" + server.getAddress().getPort() + "/notify-driving-conviction";

            try (Response response = service.post(url, "{\"drivingLicenceNumber\":\"TEST123\"}", "apim-subscription-key")) {
                assertEquals(HttpServletResponse.SC_OK, response.getStatus());
                assertEquals("{\"status\":\"accepted\"}", response.readEntity(String.class));
            }

            assertEquals("{\"drivingLicenceNumber\":\"TEST123\"}", receivedBody.get());
            assertEquals("application/json", receivedHeaders.get().getFirst("Accept"));
            assertEquals("apim-subscription-key", receivedHeaders.get().getFirst("Ocp-Apim-Subscription-Key"));
            assertEquals("true", receivedHeaders.get().getFirst("Ocp-Apim-Trace"));
            assertTrue(receivedHeaders.get().getFirst("Content-Type").startsWith("application/json"));
        } finally {
            server.stop(0);
        }
    }

    private static RestEasyClientService realRestEasyClientServiceWithPoolSizeFrom(final String maxRecordCount) throws Exception {
        final DvlaApimConfig dvlaApimConfig = mock(DvlaApimConfig.class);
        when(dvlaApimConfig.getDrivingConvictionRetryMaxRecordCount()).thenReturn(maxRecordCount);
        final RestEasyClientService service = new RestEasyClientService();
        final Field config = RestEasyClientService.class.getDeclaredField("dvlaApimConfig");
        config.setAccessible(true);
        config.set(service, dvlaApimConfig);
        final Method init = RestEasyClientService.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(service);
        return service;
    }

    private static SSLContext sslContextWithSelfSignedCertificateFor(final String commonName) throws Exception {
        final char[] password = "changeit".toCharArray();
        final Path keystore = Files.createTempDirectory("chd05").resolve("server.p12");
        final String keytool = Paths.get(System.getProperty("java.home"), "bin", "keytool").toString();
        final Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server", "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=" + commonName, "-validity", "1", "-storetype", "PKCS12", "-keystore", keystore.toString(),
                "-storepass", "changeit", "-keypass", "changeit")
                .redirectErrorStream(true)
                .start();
        process.getInputStream().readAllBytes();
        assertEquals(0, process.waitFor(), "keytool failed to create the test keystore");

        final KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(keystore.toFile())) {
            keyStore.load(in, password);
        }
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }
}
