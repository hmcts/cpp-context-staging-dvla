package uk.gov.moj.cpp.stagingdvla.handler.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class DriverSearchAuditApi {

    protected static final String STAGINGDVLA_COMMAND_GENERATE_DRIVER_SEARCH_AUDIT_REPORT = "stagingdvla.command.generate-driver-record-search-audit-report";
    protected static final String STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT = "stagingdvla.command.handler.generate-driver-record-search-audit-report";
    protected static final String STAGINGDVLA_COMMAND_DELETE_DRIVER_SEARCH_AUDIT_REPORT = "stagingdvla.command.delete-driver-record-search-audit-report";
    protected static final String STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT = "stagingdvla.command.handler.delete-driver-record-search-audit-report";
    private final Sender sender;

    @Inject
    public DriverSearchAuditApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles(STAGINGDVLA_COMMAND_GENERATE_DRIVER_SEARCH_AUDIT_REPORT)
    public void generateDriverSearchAuditReport(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(STAGINGDVLA_COMMAND_HANDLER_GENERATE_DRIVER_SEARCH_AUDIT_REPORT)
                .withMetadataFrom(envelope));
    }

    @Handles(STAGINGDVLA_COMMAND_DELETE_DRIVER_SEARCH_AUDIT_REPORT)
    public void deleteDriverSearchAuditReport(final JsonEnvelope envelope) {
        sender.send(envelop(envelope.payloadAsJsonObject())
                .withName(STAGINGDVLA_COMMAND_HANDLER_DELETE_DRIVER_SEARCH_AUDIT_REPORT)
                .withMetadataFrom(envelope));
    }
}
