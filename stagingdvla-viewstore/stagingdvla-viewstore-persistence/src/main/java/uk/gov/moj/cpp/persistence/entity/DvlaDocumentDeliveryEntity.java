package uk.gov.moj.cpp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "dvla_document_delivery")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class DvlaDocumentDeliveryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "material_status")
    private String materialStatus;

    @Column(name = "payload_blob_uri")
    private String payloadBlobUri;

    @Column(name = "document_blob_uri")
    private String documentBlobUri;

    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "sjp_correlation_id")
    private UUID sjpCorrelationId;

    @Column(name = "sjp_status")
    private String sjpStatus;

    public DvlaDocumentDeliveryEntity() {
    }

    public DvlaDocumentDeliveryEntity(final UUID materialId, final ZonedDateTime createdAt, final String materialStatus, final String payloadBlobUri, final String documentBlobUri) {
        this(materialId, createdAt, materialStatus, payloadBlobUri, documentBlobUri, null, null, null);
    }

    public DvlaDocumentDeliveryEntity(final UUID materialId, final ZonedDateTime createdAt, final String materialStatus, final String payloadBlobUri, final String documentBlobUri, final UUID caseId, final UUID sjpCorrelationId, final String sjpStatus) {
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

    public void setMaterialId(final UUID materialId) {
        this.materialId = materialId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(final ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMaterialStatus() {
        return materialStatus;
    }

    public void setMaterialStatus(final String materialStatus) {
        this.materialStatus = materialStatus;
    }

    public String getPayloadBlobUri() {
        return payloadBlobUri;
    }

    public void setPayloadBlobUri(final String payloadBlobUri) {
        this.payloadBlobUri = payloadBlobUri;
    }

    public String getDocumentBlobUri() {
        return documentBlobUri;
    }

    public void setDocumentBlobUri(final String documentBlobUri) {
        this.documentBlobUri = documentBlobUri;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getSjpCorrelationId() {
        return sjpCorrelationId;
    }

    public void setSjpCorrelationId(final UUID sjpCorrelationId) {
        this.sjpCorrelationId = sjpCorrelationId;
    }

    public String getSjpStatus() {
        return sjpStatus;
    }

    public void setSjpStatus(final String sjpStatus) {
        this.sjpStatus = sjpStatus;
    }
}
