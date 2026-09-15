package uk.gov.moj.cpp.stagingdvla.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.CONVERSION_FORMAT;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.ORIGINATING_SOURCE;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.PAYLOAD_FILE_SERVICE_ID;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.SOURCE_CORRELATION_ID;
import static uk.gov.moj.cpp.stagingdvla.helper.DriverSearchAuditHelper.TEMPLATE_IDENTIFIER;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Objects;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemDocGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemDocGeneratorService.class);
    private static final String GENERATE_DOCUMENT_COMMAND = "systemdocgenerator.generate-document";

    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;

    public void generateDocument(final DocumentGenerationRequest request, final JsonEnvelope envelope ) {
        final JsonObjectBuilder builder = createObjectBuilder();
        builder.add(ORIGINATING_SOURCE, request.getOriginatingSource())
                .add(TEMPLATE_IDENTIFIER, request.getTemplateIdentifier())
                .add(CONVERSION_FORMAT, request.getConversionFormat().getValue())
                .add(SOURCE_CORRELATION_ID, request.getSourceCorrelationId());
        if(Objects.isNull(request.getPayloadFileServiceId())) {
            builder.add("payloadFileUri", request.getPayloadFileUri())
                    .add("destinationFileUri", request.getDestinationFileUri());
        } else {
            builder.add(PAYLOAD_FILE_SERVICE_ID, request.getPayloadFileServiceId().toString());
        }

        final JsonObject docGeneratorPayload = builder.build();

        LOGGER.info(GENERATE_DOCUMENT_COMMAND + " - {}", docGeneratorPayload);
        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata()).withName(GENERATE_DOCUMENT_COMMAND),
                docGeneratorPayload
        ));
    }

}
