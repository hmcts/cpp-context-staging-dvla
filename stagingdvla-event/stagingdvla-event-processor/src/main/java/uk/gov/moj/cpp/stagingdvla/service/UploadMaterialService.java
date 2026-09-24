package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;


import javax.inject.Inject;
import javax.json.JsonObject;

public class UploadMaterialService {

    public static final String STAGINGDVLA_COMMAND = "stagingdvla.command.record-nows-material-request";

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private Enveloper enveloper;

    public void uploadFile(final UploadMaterialContext uploadMaterialContext) {
        final MaterialDetails.Builder context = MaterialDetails.materialDetails()
                .withMaterialId(uploadMaterialContext.getMaterialId())
                .withHearingId(uploadMaterialContext.getHearingId())
                .withUserId(uploadMaterialContext.getUserId())
                .withCaseId(uploadMaterialContext.getCaseId())
                .withApplicationId(uploadMaterialContext.getApplicationId())
                .withEmailNotifications(uploadMaterialContext.getEmailNotifications());

        // materialDetails.json is an exclusive oneOf - either a file service id or both uris, never
        // a mix. Branch on what this document actually carries rather than on the dvlaFileStore
        // toggle: an in-flight render straddles a toggle flip, so the event is the only reliable
        // signal of which mode this particular document is in.
        if (isNull(uploadMaterialContext.getDestinationFileUri())) {
            context.withFileId(uploadMaterialContext.getFileId());
        } else {
            context.withPayloadFileUri(uploadMaterialContext.getPayloadFileUri())
                    .withDestinationFileUri(uploadMaterialContext.getDestinationFileUri());
        }

        final NowsMaterialRequestRecorded recordNowsMaterialRequest = NowsMaterialRequestRecorded.nowsMaterialRequestRecorded()
                .withContext(context.build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(recordNowsMaterialRequest);

        final Metadata metadata = metadataFrom(uploadMaterialContext.getOriginatingEnvelope().metadata())
                .withUserId(uploadMaterialContext.getUserId().toString())
                .build();

        uploadMaterialContext.getSender().send(Envelope.envelopeFrom(metadataFrom(metadata)
                        .withName(STAGINGDVLA_COMMAND)
                        .build(),
                payload
        ));
    }

}
