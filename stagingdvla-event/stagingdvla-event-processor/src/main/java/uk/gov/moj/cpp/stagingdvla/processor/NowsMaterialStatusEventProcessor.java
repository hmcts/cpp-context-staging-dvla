package uk.gov.moj.cpp.stagingdvla.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

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
        materialService.uploadMaterial(nowsMaterialRequestRecorded.getContext().getFileId(), nowsMaterialRequestRecorded.getContext().getMaterialId(), event);
    }

}