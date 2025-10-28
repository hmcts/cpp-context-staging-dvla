package uk.gov.justice.api.resource;

import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("driver-audit-report/{reportId}/material/{materialId}")
public interface QueryApiDriverAuditReportReportIdMaterialMaterialIdResource {

    @GET
    @Produces({"application/vnd.stagingdvla.query.driver-search-audit-report-content+json"})
    Response getDriverAuditReportByReportIdMaterialByMaterialId(@PathParam("reportId") String reportId,
                                                                @PathParam("materialId") String materialId,
                                                                @HeaderParam(USER_ID) String userId);

}
