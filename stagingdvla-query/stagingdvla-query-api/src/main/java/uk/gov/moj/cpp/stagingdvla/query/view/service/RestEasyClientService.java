package uk.gov.moj.cpp.stagingdvla.query.view.service;

import java.util.Map;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

public class RestEasyClientService {

    ResteasyClient client = new ResteasyClientBuilderImpl().disableTrustManager().build();

    public Response post(final String url,  final String payload, final String key){
        final Invocation.Builder request = this.client.target(url).request();
        request.headers(new MultivaluedHashMap(getHeaders(key)));
        return request.post(Entity.json(payload));
    }

    private Map<String, String> getHeaders(final String subscriptionKey) {
        return ImmutableMap.of(
                HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                "Ocp-Apim-Subscription-Key", subscriptionKey,
                "Ocp-Apim-Trace", "true");
    }
}
