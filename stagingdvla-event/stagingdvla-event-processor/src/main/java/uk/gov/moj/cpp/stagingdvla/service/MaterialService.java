package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonMetadata.ID;
import static uk.gov.justice.services.messaging.JsonMetadata.NAME;
import static uk.gov.justice.services.messaging.JsonMetadata.USER_ID;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.exception.UserNotFoundException;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaterialService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialService.class.getCanonicalName());
    public static final String DELETE_MATERIAL = "material.command.delete-material";
    public static final String CONTEXT = "context";
    public static final String SOURCE = "originator";
    public static final String PROCESS_ID = "processId";
    public static final String ORIGINATOR_VALUE = "d20";
    public static final String AUDIT_REPORT_ORIGINATOR_VALUE = "auditReport";
    public static final String UPLOAD_MATERIAL = "material.command.upload-file";
    public static final String MATERIAL_ID = "materialId";
    public static final String FILE_SERVICE_ID = "fileServiceId";
    public static final String MISSING_USER_ID = "UserId missing from event.";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public static Metadata createMetadataWithProcessIdAndUserId(final String id, final String name, final String userId) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ID, id)
                .add(NAME, name)
                .add(SOURCE, ORIGINATOR_VALUE)
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(USER_ID, userId))
                .build()).build();
    }

    public static Metadata createMetadataWithProcessIdAndUserIdAndSource(final String processId, final String name, final String userId, final String source) {
        return metadataFrom(Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(NAME, name)
                .add(SOURCE, source)
                .add(PROCESS_ID, processId)
                .add(CONTEXT, Json.createObjectBuilder()
                        .add(USER_ID, userId))
                .build()).build();
    }

    public static JsonEnvelope assembleEnvelopeWithPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String userId) {
        final Metadata metadata = createMetadataWithProcessIdAndUserId(UUID.randomUUID().toString(), contentType, userId);
        final JsonObject payloadWithMetada = addMetadataToPayload(payload, metadata);
        return envelopeFrom(metadata, payloadWithMetada);
    }

    public static JsonEnvelope assembleEnvelopeWithCustomPayloadAndMetaDetails(final JsonObject payload, final String contentType, final String userId, final String source, final UUID processId) {
        final Metadata metadata = createMetadataWithProcessIdAndUserIdAndSource(processId.toString(), contentType, userId, source);
        final JsonObject payloadWithMetada = addMetadataToPayload(payload, metadata);
        return envelopeFrom(metadata, payloadWithMetada);
    }

    private static JsonObject addMetadataToPayload(final JsonObject load, final Metadata metadata) {
        final JsonObjectBuilder job = Json.createObjectBuilder();
        load.entrySet().forEach(entry -> job.add(entry.getKey(), entry.getValue()));
        job.add(JsonEnvelope.METADATA, metadata.asJsonObject());
        return job.build();
    }

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final JsonEnvelope envelope) {
        final UUID userId = fromString(envelope.metadata().userId().orElseThrow(() -> new UserNotFoundException(MISSING_USER_ID)));
        uploadMaterial(fileServiceId, materialId, userId);
    }

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final UUID userId) {
        if (isNull(userId)) {
            throw new UserNotFoundException(MISSING_USER_ID);
        }
        LOGGER.info("material being uploaded '{}' file service id '{}'", materialId, fileServiceId);
        final JsonObject uploadMaterialPayload = Json.createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(FILE_SERVICE_ID, fileServiceId.toString())
                .build();

        LOGGER.info("requesting material service to upload file id {} for material {}", fileServiceId, materialId);

        sender.send(assembleEnvelopeWithPayloadAndMetaDetails(uploadMaterialPayload, UPLOAD_MATERIAL, userId.toString()));
    }

    public void uploadMaterial(final UUID fileServiceId, final UUID materialId, final UUID userId, final String source, final UUID processId) {
        if (isNull(userId)) {
            throw new UserNotFoundException(MISSING_USER_ID);
        }
        LOGGER.info("material being uploaded '{}' file service id '{}'", materialId, fileServiceId);
        final JsonObject uploadMaterialPayload = Json.createObjectBuilder()
                .add(MATERIAL_ID, materialId.toString())
                .add(FILE_SERVICE_ID, fileServiceId.toString())
                .build();

        LOGGER.info("requesting material service to upload file id {} for material {}", fileServiceId, materialId);

        sender.send(assembleEnvelopeWithCustomPayloadAndMetaDetails(uploadMaterialPayload, UPLOAD_MATERIAL, userId.toString(), source, processId));
    }

    public void sendCommandToDeleteMaterial(final JsonEnvelope envelope, final UUID materialId) {
        sender.send(
                Enveloper.envelop(createObjectBuilder()
                                .add(MATERIAL_ID, materialId.toString()).build())
                        .withName(DELETE_MATERIAL)
                        .withMetadataFrom(envelope));
    }


}
