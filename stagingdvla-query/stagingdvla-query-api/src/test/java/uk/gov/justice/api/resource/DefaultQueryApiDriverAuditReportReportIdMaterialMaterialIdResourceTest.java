package uk.gov.justice.api.resource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

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
}
