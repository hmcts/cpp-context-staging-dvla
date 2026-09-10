package uk.gov.moj.cpp.persistence.entity;


import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Delivery tracking for a D20 document request, keyed by materialId.
 *
 * <p>Maps the table created by changeset {@code 006-create-dvla-document-delivery-table.xml}.
 * Nothing creates rows in this table yet - the only writer today is the email delivery outcome
 * projection, which updates {@code email_status} on an existing row.
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
@Entity
@Table(name = "dvla_document_delivery")
public class DvlaDocumentDeliveryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    @Column(name = "material_status")
    private String materialStatus;

    @Column(name = "email_status")
    private String emailStatus;

    @Column(name = "payload_blob_uri")
    private String payloadBlobUri;

    @Column(name = "document_blob_uri")
    private String documentBlobUri;

    public DvlaDocumentDeliveryEntity() {
    }

    public DvlaDocumentDeliveryEntity(final UUID materialId, final ZonedDateTime createdAt, final String materialStatus,
                                      final String emailStatus, final String payloadBlobUri, final String documentBlobUri) {
        this.materialId = materialId;
        this.createdAt = createdAt;
        this.materialStatus = materialStatus;
        this.emailStatus = emailStatus;
        this.payloadBlobUri = payloadBlobUri;
        this.documentBlobUri = documentBlobUri;
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

    public String getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(final String emailStatus) {
        this.emailStatus = emailStatus;
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
}
