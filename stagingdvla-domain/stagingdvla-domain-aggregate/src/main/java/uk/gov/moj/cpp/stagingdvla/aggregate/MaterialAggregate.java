package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.cpp.stagingdvla.command.RecordEmailDeliveryOutcome;
import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDelivered;
import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDeliveryFailed;
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
            // The notification id is minted here, not at the point of sending.
            return Stream.of(new EmailNotificationSent(this.details, randomUUID()));
        }
        return null;
    }

    /**
     * Records the outcome notificationnotify reported for the D20 email notification.
     *
     * <p>Note the naming. The existing {@link EmailNotificationSent} above fires <em>before</em> the
     * email is sent - it is what triggers the send - so "sent" already means close to the opposite
     * of a delivery confirmation. These events are named delivered / delivery-failed instead.
     *
     * <p>No aggregate state is held for this: the events record what an external system reported,
     * and idempotency under redelivery is enforced where the projection is written, by treating an
     * already resolved status as sticky.
     *
     * @param outcome the reported outcome, keyed by the materialId that owns this stream
     * @return the single event to append
     */
    public Stream<Object> recordEmailDeliveryOutcome(final RecordEmailDeliveryOutcome outcome) {
        if (TRUE.equals(outcome.getDelivered())) {
            return apply(Stream.of(EmailNotificationDelivered.emailNotificationDelivered()
                    .withMaterialId(outcome.getMaterialId())
                    .withNotificationId(outcome.getNotificationId())
                    .withOccurredAt(outcome.getOccurredAt())
                    .build()));
        }
        return apply(Stream.of(EmailNotificationDeliveryFailed.emailNotificationDeliveryFailed()
                .withMaterialId(outcome.getMaterialId())
                .withNotificationId(outcome.getNotificationId())
                .withOccurredAt(outcome.getOccurredAt())
                .withErrorMessage(outcome.getErrorMessage())
                .withStatusCode(outcome.getStatusCode())
                .build()));
    }

}
