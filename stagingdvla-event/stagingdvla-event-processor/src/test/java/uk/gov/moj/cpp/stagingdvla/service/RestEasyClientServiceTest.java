package uk.gov.moj.cpp.stagingdvla.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

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
    javax.ws.rs.core.Response expectedResponse;


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
}