package uk.gov.moj.cpp.stagingdvla.event.listener;

import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.cpp.stagingdvla.event.DriverAuditRecord;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.persistence.entity.DriverAuditEntity;
import uk.gov.moj.cpp.persistence.repository.DriverAuditRepository;

import java.time.LocalDate;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DriverAuditEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private DriverAuditRepository driverAuditRepository;

    @Handles("stagingdvla.event.driver-audit-record")
    public void handleDriverAuditDetails(final JsonEnvelope jsonEnvelope) {
        final DriverAuditRecord driverAuditDetails = jsonObjectConverter
                .convert(jsonEnvelope.payloadAsJsonObject(), DriverAuditRecord.class);

        final DriverAuditEntity driverAuditEntity = new DriverAuditEntity();
        driverAuditEntity.setId(driverAuditDetails.getId());
        driverAuditEntity.setUserId(driverAuditDetails.getUserId());
        driverAuditEntity.setUserEmail(driverAuditDetails.getUserEmail());
        if (isEmpty(driverAuditDetails.getDateTime())) {
            driverAuditEntity.setDateTime(now());
        } else {
            driverAuditEntity.setDateTime(parse(driverAuditDetails.getDateTime()));
        }
        driverAuditEntity.setReasonType(driverAuditDetails.getSearchReason().getReasonType());
        driverAuditEntity.setReference(driverAuditDetails.getSearchReason().getReference());
        driverAuditEntity.setDrivingLicenseNumber(driverAuditDetails.getSearchCriteria().getDrivingLicenceNumber());
        driverAuditEntity.setFirstNames(driverAuditDetails.getSearchCriteria().getFirstName());
        driverAuditEntity.setLastName(driverAuditDetails.getSearchCriteria().getLastName());
        driverAuditEntity.setGender(driverAuditDetails.getSearchCriteria().getGender());
        driverAuditEntity.setPostcode(driverAuditDetails.getSearchCriteria().getPostcode());
        if (isEmpty(driverAuditDetails.getSearchCriteria().getDateOfBirth())) {
            driverAuditEntity.setDateOfBirth(null);
        } else {
            driverAuditEntity.setDateOfBirth(LocalDate.parse(driverAuditDetails.getSearchCriteria().getDateOfBirth()));
        }
        driverAuditRepository.save(driverAuditEntity);

    }
}
