package uk.gov.justice.api.resource;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;

import java.util.ArrayList;
import java.util.List;

import jakarta.ejb.Stateless;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiDriverAuditReportReportIdMaterialMaterialIdResourceTest {

    @InjectMocks
    private DefaultQueryApiDriverAuditReportReportIdMaterialMaterialIdResource resource;

    @Mock
    private StagingdvlaQueryView stagingdvlaQueryView;

    @Test
    public void shouldReturnProcessedMaterialResponseWhenStatusIsOk() {
        final String reportId = "reportId";
        final String materialId = "materialId";
        final String userId = "userId";
        final String url = "http://some.url";

        final Response materialResponse = mock(Response.class);
        when(materialResponse.getStatus()).thenReturn(OK.getStatusCode());
        when(materialResponse.readEntity(String.class)).thenReturn(url);

        when(stagingdvlaQueryView.getDriverAuditReportByMaterialId(reportId, materialId, userId)).thenReturn(materialResponse);

        final Response result = resource.getDriverAuditReportByReportIdMaterialByMaterialId(reportId, materialId, userId);

        assertThat(result, is(notNullValue()));
        assertThat(result.getStatus(), is(OK.getStatusCode()));

        final JsonObject entity = (JsonObject) result.getEntity();
        assertThat(entity, is(notNullValue()));
        assertThat(entity.getString("url"), is(url));
    }

    @Test
    public void shouldReturnStatusFromViewWhenStatusIsNotOk() {
        final String reportId = "reportId";
        final String materialId = "materialId";
        final String userId = "userId";

        final Response materialResponse = mock(Response.class);
        when(materialResponse.getStatus()).thenReturn(BAD_REQUEST.getStatusCode());

        when(stagingdvlaQueryView.getDriverAuditReportByMaterialId(reportId, materialId, userId)).thenReturn(materialResponse);

        final Response result = resource.getDriverAuditReportByReportIdMaterialByMaterialId(reportId, materialId, userId);

        assertThat(result, is(notNullValue()));
        assertThat(result.getStatus(), is(BAD_REQUEST.getStatusCode()));
    }

    /*
     * CHD-08 (BC-21) - Java 17 behaviour: the REST generator produces one interface and one Default...Resource per
     * RAML resource in stagingdvla-query-api.raml, except /driver-audit-report/{reportId}/material/{materialId},
     * which is hand-written in src/main and calls StagingdvlaQueryView directly (no RestProcessor/interceptor chain).
     */
    @Test
    public void chd08_shouldHaveExactlyOneResourcePerRamlEndpointWithTheDownloadOneHandWritten() throws Exception {
        final List<String> missing = new ArrayList<>();
        for (final String resource : List.of("DriverAuditRecords", "DriverDriverNumberImage", "DriverDriverNumber",
                "DriverSearchAuditReports", "Drivers", "DrivingConvictionRetryMaxCount", "DriverAuditReportReportIdMaterialMaterialId")) {
            for (final String className : List.of("QueryApi" + resource + "Resource", "DefaultQueryApi" + resource + "Resource")) {
                try {
                    Class.forName("uk.gov.justice.api.resource." + className);
                } catch (final ClassNotFoundException e) {
                    missing.add(className);
                }
            }
        }
        assertThat(missing, is(empty()));

        final Class<?> download = DefaultQueryApiDriverAuditReportReportIdMaterialMaterialIdResource.class;
        assertThat(download.isAnnotationPresent(Stateless.class), is(true));
        assertThat(download.getDeclaredField("stagingdvlaQueryView").getType(), is((Object) StagingdvlaQueryView.class));
        assertFalse(java.util.Arrays.stream(download.getDeclaredFields()).anyMatch(f -> f.getType().getSimpleName().equals("RestProcessor")));
    }
}
