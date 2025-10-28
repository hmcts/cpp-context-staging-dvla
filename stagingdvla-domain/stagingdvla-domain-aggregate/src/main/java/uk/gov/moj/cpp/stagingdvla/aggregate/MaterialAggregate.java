package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Objects.nonNull;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

public class MaterialAggregate implements Aggregate {
    private static final long serialVersionUID = 101L;
    private MaterialDetails details;
    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(NowsMaterialRequestRecorded.class).apply(e ->
                        details = e.getContext()
                ),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> create(final MaterialDetails materialDetails) {
        return apply(Stream.of(NowsMaterialRequestRecorded
                .nowsMaterialRequestRecorded()
                .withContext(materialDetails).build()));
    }

    public Stream<Object> dvlaMaterialAdded() {
        if(nonNull(details) && nonNull(details.getEmailNotifications())) {
            return Stream.of(new EmailNotificationSent(this.details));
        }
        return null;
    }

}
