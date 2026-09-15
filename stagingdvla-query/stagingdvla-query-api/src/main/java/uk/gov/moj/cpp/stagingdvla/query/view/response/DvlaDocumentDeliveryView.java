package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

public class DvlaDocumentDeliveryView implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID materialId;
    private ZonedDateTime createdAt;
    private String materialStatus;
    private String payloadBlobUri;
    private String documentBlobUri;
    private UUID caseId;
    private UUID sjpCorrelationId;
    private String sjpStatus;

    public DvlaDocumentDeliveryView() {
    }

    @SuppressWarnings("squid:S00107")
    public DvlaDocumentDeliveryView(final UUID materialId, final ZonedDateTime createdAt, final String materialStatus,
                                     final String payloadBlobUri, final String documentBlobUri,
                                     final UUID caseId, final UUID sjpCorrelationId, final String sjpStatus) {
        this.materialId = materialId;
        this.createdAt = createdAt;
        this.materialStatus = materialStatus;
        this.payloadBlobUri = payloadBlobUri;
        this.documentBlobUri = documentBlobUri;
        this.caseId = caseId;
        this.sjpCorrelationId = sjpCorrelationId;
        this.sjpStatus = sjpStatus;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public String getMaterialStatus() {
        return materialStatus;
    }

    public String getPayloadBlobUri() {
        return payloadBlobUri;
    }

    public String getDocumentBlobUri() {
        return documentBlobUri;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getSjpCorrelationId() {
        return sjpCorrelationId;
    }

    public String getSjpStatus() {
        return sjpStatus;
    }
}
