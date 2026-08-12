package uk.gov.moj.cpp.stagingdvla.service;

import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;

import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
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

    @Inject
    private DvlaApimConfig dvlaApimConfig;

    private ResteasyClient client;

    @PostConstruct
    private void init() {
        client = new ResteasyClientBuilderImpl().disableTrustManager()
                .connectionPoolSize(Integer.valueOf(dvlaApimConfig.getDrivingConvictionRetryMaxRecordCount()) > 0
                        ? Integer.valueOf(dvlaApimConfig.getDrivingConvictionRetryMaxRecordCount()) + 1
                        : 100)
                .build();
    }

    public Response post(final String url, final String payload, final String key) {
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