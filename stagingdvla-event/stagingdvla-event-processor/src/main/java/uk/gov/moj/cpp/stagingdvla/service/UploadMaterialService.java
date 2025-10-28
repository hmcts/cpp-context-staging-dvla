package uk.gov.moj.cpp.stagingdvla.service;

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
        final NowsMaterialRequestRecorded recordNowsMaterialRequest = NowsMaterialRequestRecorded.nowsMaterialRequestRecorded()
                .withContext(MaterialDetails.materialDetails()
                        .withMaterialId(uploadMaterialContext.getMaterialId())
                        .withFileId(uploadMaterialContext.getFileId())
                        .withHearingId(uploadMaterialContext.getHearingId())
                        .withUserId(uploadMaterialContext.getUserId())
                        .withCaseId(uploadMaterialContext.getCaseId())
                        .withApplicationId(uploadMaterialContext.getApplicationId())
                        .withEmailNotifications(uploadMaterialContext.getEmailNotifications())
                        .build())
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
