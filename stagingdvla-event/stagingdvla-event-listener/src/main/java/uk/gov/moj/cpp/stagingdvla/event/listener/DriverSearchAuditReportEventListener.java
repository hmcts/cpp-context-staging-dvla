package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditReportRepository;
import uk.gov.moj.cpp.stagingdvla.domain.constants.DriverAuditReportStatus;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DriverSearchAuditReportEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DriverAuditReportRepository driverAuditReportRepository;

    @Handles("stagingdvla.event.driver-search-audit-report-requested")
    public void handleDriverSearchAuditReportRequested(final JsonEnvelope jsonEnvelope) {
        final DriverSearchAuditReportRequested reportRequested = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), DriverSearchAuditReportRequested.class);

        final DriverAuditReportEntity driverAuditReportEntity = new DriverAuditReportEntity();
        driverAuditReportEntity.setId(reportRequested.getId());
        driverAuditReportEntity.setUserId(reportRequested.getUserId());
        driverAuditReportEntity.setDateTime((isEmpty(reportRequested.getDateTime())) ? now() : parse(reportRequested.getDateTime()));
        driverAuditReportEntity.setReportSearchCriteria(objectToJsonObjectConverter.convert(reportRequested.getReportSearchCriteria()).toString());
        driverAuditReportEntity.setStatus(DriverAuditReportStatus.PENDING.getStatus());
        driverAuditReportRepository.save(driverAuditReportEntity);

    }

    @Handles("stagingdvla.event.driver-search-audit-report-stored")
    public void handleDriverSearchAuditReportStored(final JsonEnvelope jsonEnvelope) {
        final DriverSearchAuditReportStored reportStored = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), DriverSearchAuditReportStored.class);
        final DriverAuditReportEntity driverAuditReportEntity = driverAuditReportRepository.findBy(reportStored.getId());
        if (nonNull(driverAuditReportEntity)) {
            driverAuditReportEntity.setStatus(DriverAuditReportStatus.COMPLETED.getStatus());
            driverAuditReportEntity.setMaterialId(reportStored.getMaterialId());
        }
        driverAuditReportRepository.save(driverAuditReportEntity);
    }

    @Handles("stagingdvla.event.driver-search-audit-report-deleted")
    public void handleDriverSearchAuditReportDeleted(final JsonEnvelope jsonEnvelope) {
        final DriverSearchAuditReportDeleted reportDeleted = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), DriverSearchAuditReportDeleted.class);
        final DriverAuditReportEntity driverAuditReportEntity = driverAuditReportRepository.findBy(reportDeleted.getId());
        driverAuditReportRepository.remove(driverAuditReportEntity);
    }

}
