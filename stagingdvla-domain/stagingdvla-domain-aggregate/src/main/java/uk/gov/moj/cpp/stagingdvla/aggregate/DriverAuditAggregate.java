package uk.gov.moj.cpp.stagingdvla.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.cpp.stagingdvla.event.DriverAuditRecord;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

@SuppressWarnings("squid:S1450")
public class DriverAuditAggregate implements Aggregate {
    private static final long serialVersionUID = 103L;

    private DriverAuditRecord auditRecord;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(DriverAuditRecord.class).apply(e -> auditRecord = e),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> createAudit(final DriverAuditRecord auditDetails) {
        return Stream.of(DriverAuditRecord.driverAuditRecord()
                .withId(auditDetails.getId())
                .withUserId(auditDetails.getUserId())
                .withDateTime(auditDetails.getDateTime())
                .withUserEmail(auditDetails.getUserEmail())
                .withSearchReason(auditDetails.getSearchReason())
                .withSearchCriteria(auditDetails.getSearchCriteria())
                .build());
    }
}
