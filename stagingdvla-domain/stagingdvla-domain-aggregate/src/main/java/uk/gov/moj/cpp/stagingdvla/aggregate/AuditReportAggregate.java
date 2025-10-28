package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.cpp.stagingdvla.DriverAuditReportSearchCriteria;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverRecordSearchAuditReportStored;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportCreated;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeleted;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportDeletionFailed;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportRequested;
import uk.gov.justice.cpp.stagingdvla.event.DriverSearchAuditReportStored;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@SuppressWarnings("squid:S1450")
public class AuditReportAggregate implements Aggregate {
    private static final long serialVersionUID = 1L;

    private final Map<UUID, UUID> reportCreatedByMap = new HashMap<>();

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DriverSearchAuditReportRequested.class).apply(reportRequested ->
                        this.reportCreatedByMap.put(reportRequested.getId(), reportRequested.getUserId())
                ),
                when(DriverSearchAuditReportCreated.class).apply(reportCreated -> {
                }),
                when(DriverSearchAuditReportStored.class).apply(reportStored -> {
                }),
                when(DriverSearchAuditReportDeleted.class).apply(reportDeleted ->
                        this.reportCreatedByMap.remove(reportDeleted.getId())
                ),
                when(DriverSearchAuditReportDeletionFailed.class).apply(reportDeletionFailed -> {
                }),
                otherwiseDoNothing());
    }

    public Stream<Object> generateAuditReport(final UUID reportId, final UUID userId, final String zonedDateTime, final DriverAuditReportSearchCriteria driverAuditReportSearchCriteria) {
        return Stream.of(DriverSearchAuditReportRequested.driverSearchAuditReportRequested()
                .withId(reportId)
                .withUserId(userId)
                .withDateTime(zonedDateTime)
                .withReportSearchCriteria(driverAuditReportSearchCriteria)
                .build());
    }

    public Stream<Object> auditReportCreated(final DriverRecordSearchAuditReportCreated auditReportCreated) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(DriverSearchAuditReportCreated.driverSearchAuditReportCreated()
                .withId(auditReportCreated.getId())
                .withReportFileId(auditReportCreated.getReportFileId())
                .withMaterialId(randomUUID())
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> auditReportStored(final DriverRecordSearchAuditReportStored auditReportStored) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(DriverSearchAuditReportStored.driverSearchAuditReportStored()
                .withId(auditReportStored.getId())
                .withMaterialId(auditReportStored.getMaterialId())
                .build());

        return apply(streamBuilder.build());
    }

    public Stream<Object> deleteAuditReport(final UUID reportId, final UUID userId, final UUID materialId) {
        if (!isReportCreatedByUser(reportId, userId)) {
            return Stream.of(DriverSearchAuditReportDeletionFailed.driverSearchAuditReportDeletionFailed()
                    .withId(reportId)
                    .withMaterialId(materialId)
                    .build());
        }
        return Stream.of(DriverSearchAuditReportDeleted.driverSearchAuditReportDeleted()
                .withId(reportId)
                .withMaterialId(materialId)
                .build());
    }

    private boolean isReportCreatedByUser(final UUID reportId, final UUID userId) {
        return this.reportCreatedByMap.containsKey(reportId) && reportCreatedByMap.get(reportId).equals(userId);
    }

    //should be used only in test
    public void setReportCreatedByMap(final UUID reportId, final UUID userId) {
        this.reportCreatedByMap.put(reportId, userId);
    }
}
