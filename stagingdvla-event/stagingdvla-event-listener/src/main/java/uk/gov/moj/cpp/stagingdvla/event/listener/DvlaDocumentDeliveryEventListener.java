package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DvlaDocumentDeliveryEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Handles("stagingdvla.event.dvla-document-delivery-recorded")
    public void handleDvlaDocumentDeliveryRecorded(final JsonEnvelope jsonEnvelope) {
        final DvlaDocumentDeliveryRecorded recorded = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), DvlaDocumentDeliveryRecorded.class);

        final DvlaDocumentDeliveryEntity entity = dvlaDocumentDeliveryRepository.findBy(recorded.getMaterialId());

        if (isNull(entity)) {
            insertNewRecord(recorded);
        } else {
            updateExistingRecord(recorded, entity);
        }
    }

    private void insertNewRecord(final DvlaDocumentDeliveryRecorded recorded) {
        final DvlaDocumentDeliveryEntity entity = new DvlaDocumentDeliveryEntity();
        entity.setMaterialId(recorded.getMaterialId());
        entity.setCreatedAt(now());
        applyNonNullFields(recorded, entity);
        dvlaDocumentDeliveryRepository.save(entity);
    }

    private void updateExistingRecord(final DvlaDocumentDeliveryRecorded recorded, final DvlaDocumentDeliveryEntity entity) {
        applyNonNullFields(recorded, entity);
        dvlaDocumentDeliveryRepository.save(entity);
    }

    private void applyNonNullFields(final DvlaDocumentDeliveryRecorded recorded, final DvlaDocumentDeliveryEntity entity) {
        if (nonNull(recorded.getMaterialStatus())) {
            entity.setMaterialStatus(recorded.getMaterialStatus());
        }
        if (nonNull(recorded.getPayloadBlobUri())) {
            entity.setPayloadBlobUri(recorded.getPayloadBlobUri());
        }
        if (nonNull(recorded.getDocumentBlobUri())) {
            entity.setDocumentBlobUri(recorded.getDocumentBlobUri());
        }
        if (nonNull(recorded.getCaseId())) {
            entity.setCaseId(recorded.getCaseId());
        }
        if (nonNull(recorded.getSjpCorrelationId())) {
            entity.setSjpCorrelationId(recorded.getSjpCorrelationId());
        }
        if (nonNull(recorded.getSjpStatus())) {
            entity.setSjpStatus(recorded.getSjpStatus());
        }
    }
}
