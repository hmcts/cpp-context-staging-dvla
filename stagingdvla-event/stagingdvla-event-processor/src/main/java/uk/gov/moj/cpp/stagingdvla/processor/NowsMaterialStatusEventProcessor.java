package uk.gov.moj.cpp.stagingdvla.processor;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.service.MaterialService;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class NowsMaterialStatusEventProcessor {

    public static final String GENERATED_STATUS_VALUE = "generated";

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private MaterialService materialService;

    @Handles("stagingdvla.event.nows-material-request-recorded")
    public void processRequestRecorded(final JsonEnvelope event) {
        final NowsMaterialRequestRecorded nowsMaterialRequestRecorded = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), NowsMaterialRequestRecorded.class);
        final MaterialDetails context = nowsMaterialRequestRecorded.getContext();

        // destinationFileUri is the rendered document; payloadFileUri is the render input and never
        // reaches material. Exactly one of the two addressing modes is populated - materialDetails.json
        // enforces that with a oneOf.
        if (isNull(context.getDestinationFileUri())) {
            materialService.uploadMaterial(context.getFileId(), context.getMaterialId(), event);
        } else {
            materialService.uploadMaterialFromUri(context.getDestinationFileUri(), context.getMaterialId(), event);
        }
    }

}