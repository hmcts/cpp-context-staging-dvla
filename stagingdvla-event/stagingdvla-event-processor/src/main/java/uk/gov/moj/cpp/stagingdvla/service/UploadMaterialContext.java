package uk.gov.moj.cpp.stagingdvla.service;

import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("squid:S2384")
public class UploadMaterialContext {

    private  Sender sender;

    private  JsonEnvelope originatingEnvelope;

    private  UUID userId;

    private  UUID hearingId;

    private  UUID materialId;

    private  UUID fileId;

    private  UUID caseId;

    private  UUID applicationId;

    private List<EmailChannel> emailNotifications;

    public Sender getSender() {
        return sender;
    }

    public JsonEnvelope getOriginatingEnvelope() {
        return originatingEnvelope;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getFileId() {
        return fileId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UploadMaterialContext setSender(final Sender sender) {
        this.sender = sender;
        return this;
    }

    public UploadMaterialContext setOriginatingEnvelope(final JsonEnvelope originatingEnvelope) {
        this.originatingEnvelope = originatingEnvelope;
        return this;
    }

    public UploadMaterialContext setUserId(final UUID userId) {
        this.userId = userId;
        return this;
    }

    public UploadMaterialContext setHearingId(final UUID hearingId) {
        this.hearingId = hearingId;
        return this;
    }

    public UploadMaterialContext setMaterialId(final UUID materialId) {
        this.materialId = materialId;
        return this;
    }

    public UploadMaterialContext setFileId(final UUID fileId) {
        this.fileId = fileId;
        return this;
    }

    public UploadMaterialContext setCaseId(final UUID caseId) {
        this.caseId = caseId;
        return this;
    }

    public UploadMaterialContext setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    public List<EmailChannel> getEmailNotifications() {
        return emailNotifications;
    }

    public UploadMaterialContext setEmailNotifications(final List<EmailChannel> emailNotifications) {
        this.emailNotifications = emailNotifications;
        return this;
    }

    public UploadMaterialContext build() {
        final UploadMaterialContext uploadMaterialContext = new UploadMaterialContext();
        uploadMaterialContext.setSender(sender);
        uploadMaterialContext.setOriginatingEnvelope(originatingEnvelope);
        uploadMaterialContext.setUserId(userId);
        uploadMaterialContext.setHearingId(hearingId);
        uploadMaterialContext.setFileId(fileId);
        uploadMaterialContext.setMaterialId(materialId);
        uploadMaterialContext.setCaseId(caseId);
        uploadMaterialContext.setApplicationId(applicationId);
        uploadMaterialContext.setEmailNotifications(emailNotifications);
        return uploadMaterialContext;
    }
}
