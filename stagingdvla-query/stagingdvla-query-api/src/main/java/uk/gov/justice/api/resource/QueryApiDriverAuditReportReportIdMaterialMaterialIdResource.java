package uk.gov.justice.api.resource;

import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

@Path("driver-audit-report/{reportId}/material/{materialId}")
public interface QueryApiDriverAuditReportReportIdMaterialMaterialIdResource {

    @GET
    @Produces({"application/vnd.stagingdvla.query.driver-search-audit-report-content+json"})
    Response getDriverAuditReportByReportIdMaterialByMaterialId(@PathParam("reportId") String reportId,
                                                                @PathParam("materialId") String materialId,
                                                                @HeaderParam(USER_ID) String userId);

}
