package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.MATERIAL_ID;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.AUDIT_REPORT_ORIGINATOR_VALUE;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.ORIGINATOR_VALUE;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.PROCESS_ID;
import static uk.gov.moj.cpp.stagingdvla.service.MaterialService.SOURCE;

import uk.gov.justice.core.courts.StagingdvlaSendEmailNotification;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportStored;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class MaterialAddedProcessor {

    public static final String STAGING_DVLA_COMMAND_SEND_EMAIL_NOTIFICATION = "stagingdvla.command.send-email-notification";
    public static final String STAGING_DVLA_COMMAND_AUDIT_REPORT_STORED = "stagingdvla.command.handler.driver-record-search-audit-report-stored";

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialAddedProcessor.class.getName());
    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("material.material-added")
    public void processEvent(final JsonEnvelope event) {
        LOGGER.info("Received MaterialAddedEvent {}", event.toObfuscatedDebugString());
        if (event.metadata().asJsonObject().containsKey(SOURCE) && ORIGINATOR_VALUE.equalsIgnoreCase(event.metadata().asJsonObject().getString(SOURCE))) {
            processDvlaMaterialNotificationRequest(event);
        }

        if (event.metadata().asJsonObject().containsKey(SOURCE) && AUDIT_REPORT_ORIGINATOR_VALUE.equalsIgnoreCase(event.metadata().asJsonObject().getString(SOURCE))) {
            handleDriverAuditReportUploadedEvent(event);
        }
    }

    private void handleDriverAuditReportUploadedEvent(final JsonEnvelope event) {
        final String reportId = event.metadata().asJsonObject().getString(PROCESS_ID);
        final UUID materialId = UUID.fromString(event.payloadAsJsonObject().getString(MATERIAL_ID));
        LOGGER.info("Sending stagingdvla.command.handler.driver-record-search-audit-report-stored for reportId: {}",
                reportId);
        this.sender.send(Envelope.envelopeFrom(metadataFrom(event.metadata()).withName(STAGING_DVLA_COMMAND_AUDIT_REPORT_STORED).build(),
                this.objectToJsonObjectConverter.convert(buildHandleDriverAuditReportDocumentStored(fromString(reportId), materialId))));

    }

    private DriverRecordSearchAuditReportStored buildHandleDriverAuditReportDocumentStored(final UUID reportId, final UUID materialId) {
        return new DriverRecordSearchAuditReportStored.Builder().withId(reportId).withMaterialId(materialId).build();
    }

    private void processDvlaMaterialNotificationRequest(JsonEnvelope event) {
        final UUID materialId = UUID.fromString(event.payloadAsJsonObject().getString(MATERIAL_ID));
        final StagingdvlaSendEmailNotification materialAdded = StagingdvlaSendEmailNotification.stagingdvlaSendEmailNotification()
                .withMaterialId(materialId).build();
        final JsonObject payload = objectToJsonObjectConverter.convert(materialAdded);
        sender.send(envelop(payload).withName(STAGING_DVLA_COMMAND_SEND_EMAIL_NOTIFICATION).withMetadataFrom(event));

    }

}
