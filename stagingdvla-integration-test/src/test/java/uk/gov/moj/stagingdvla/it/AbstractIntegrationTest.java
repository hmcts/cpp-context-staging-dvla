package uk.gov.moj.stagingdvla.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.stagingdvla.stubs.ApimStub.stubApimDvla;
import static uk.gov.moj.stagingdvla.stubs.MaterialStub.stubMaterialUploadFile;
import static uk.gov.moj.stagingdvla.stubs.NotifyStub.stubNotifications;
import static uk.gov.moj.stagingdvla.util.StubUtil.stubUser;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.mockMaterialUpload;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.http.RequestParams;

import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.google.common.base.Joiner;

public abstract class AbstractIntegrationTest {

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");
    private static final String SCHEME = "http";
    private static final String PORT = "8080";
    private static final String BASE_URI = SCHEME + "://" + HOST + ":" + PORT;
    private static final String WRITE_BASE_URL = "/stagingdvla-service/command/api/rest/stagingdvla";
    protected MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    public static UUID USER_ID = UUID.randomUUID();

    static {
        stubUser(USER_ID.toString());
        stubNotifications();
        stubMaterialUploadFile();
        stubApimDvla();
        defaultStubs();
    }

    public AbstractIntegrationTest() {
        headers.putSingle(HeaderConstants.USER_ID, USER_ID);
    }

    public static  void waitForStubToBeReady(final String resource, final String mediaType) {
        waitForStubToBeReady(resource, mediaType, Response.Status.OK);
    }

    public static  void waitForStubToBeReady(final String resource, final String mediaType, final Response.Status expectedStatus) {
        final RequestParams requestParams = requestParams(BASE_URI + resource, mediaType).build();

        poll(requestParams)
                .timeout(20L, SECONDS)
                .until(
                        status().is(expectedStatus)
                );
    }

    public static String getWriteUrl(final String resource) {
        return Joiner.on("").join(BASE_URI, WRITE_BASE_URL, resource);
    }

    private static void defaultStubs() {
        mockMaterialUpload();
    }

}
