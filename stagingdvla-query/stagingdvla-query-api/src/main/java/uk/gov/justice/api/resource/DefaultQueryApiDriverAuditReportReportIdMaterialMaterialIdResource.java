package uk.gov.justice.api.resource;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

@Stateless
@Adapter("QUERY_API")
public class DefaultQueryApiDriverAuditReportReportIdMaterialMaterialIdResource implements QueryApiDriverAuditReportReportIdMaterialMaterialIdResource {

    @Inject
    private static final Logger LOGGER = getLogger(DefaultQueryApiDriverAuditReportReportIdMaterialMaterialIdResource.class);
    @Inject
    private StagingdvlaQueryView stagingdvlaQueryView;

    @Override
    public Response getDriverAuditReportByReportIdMaterialByMaterialId(final String reportId, final String materialId, final String userId) {
        final Response materialResponse = stagingdvlaQueryView.getDriverAuditReportByMaterialId(reportId, materialId, userId);
        final Response.Status materialResponseStatus = fromStatusCode(materialResponse.getStatus());
        LOGGER.debug("Material context response status {} for materialId {}", materialResponseStatus, materialId);
        if (OK.equals(materialResponseStatus)) {
            return processedMaterialResponse(materialResponse);
        } else {
            return status(materialResponseStatus).build();
        }
    }

    private Response processedMaterialResponse(final Response materialResponse) {
        final String url = materialResponse.readEntity(String.class);
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("url", url)
                .build();

        return Response
                .status(OK)
                .entity(jsonObject)
                .header(CONTENT_TYPE, "application/json")
                .build();
    }

}
