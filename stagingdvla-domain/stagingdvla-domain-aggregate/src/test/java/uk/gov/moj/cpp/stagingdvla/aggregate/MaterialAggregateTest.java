package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.EmailNotificationSent;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.notification.EmailChannel;
import uk.gov.justice.cpp.stagingdvla.command.RecordEmailDeliveryOutcome;
import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDelivered;
import uk.gov.justice.cpp.stagingdvla.event.EmailNotificationDeliveryFailed;

import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialAggregateTest {

    @InjectMocks
    private MaterialAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new MaterialAggregate();
    }

    @Test
    public void shouldCreate() {
        final UUID applicationId = randomUUID();
        final UUID materialId = randomUUID();
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withApplicationId(applicationId)
                .withMaterialId(materialId)
                .build();
        aggregate.apply(materialDetails);
        final List<Object> eventStream = aggregate.create(materialDetails).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowsMaterialRequestRecorded.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(NowsMaterialRequestRecorded.class)));
    }

    @Test
    public void shouldMintANotificationIdOntoTheEmailNotificationSentEvent() {
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withMaterialId(randomUUID())
                .withEmailNotifications(singletonList(EmailChannel.emailChannel().build()))
                .build();
        aggregate.apply(NowsMaterialRequestRecorded.nowsMaterialRequestRecorded()
                .withContext(materialDetails).build());

        final List<Object> eventStream = aggregate.dvlaMaterialAdded().collect(toList());

        assertThat(eventStream.size(), is(1));
        final EmailNotificationSent event = (EmailNotificationSent) eventStream.get(0);
        // recorded on the event so the send is traceable through to notificationnotify
        assertThat(event.getNotificationId(), is(notNullValue()));
    }

    @Test
    public void shouldMintADistinctNotificationIdForEachNotification() {
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withMaterialId(randomUUID())
                .withEmailNotifications(singletonList(EmailChannel.emailChannel().build()))
                .build();
        aggregate.apply(NowsMaterialRequestRecorded.nowsMaterialRequestRecorded()
                .withContext(materialDetails).build());

        final UUID first = ((EmailNotificationSent) aggregate.dvlaMaterialAdded().collect(toList()).get(0)).getNotificationId();
        final UUID second = ((EmailNotificationSent) aggregate.dvlaMaterialAdded().collect(toList()).get(0)).getNotificationId();

        // a material can be notified more than once, so the ids must not collide
        assertThat(first, is(not(second)));
    }

    @Test
    public void shouldEmitDeliveredEventWhenEmailWasDelivered() {
        final UUID materialId = randomUUID();
        final UUID notificationId = randomUUID();

        final List<Object> eventStream = aggregate.recordEmailDeliveryOutcome(
                RecordEmailDeliveryOutcome.recordEmailDeliveryOutcome()
                        .withMaterialId(materialId)
                        .withDelivered(true)
                        .withNotificationId(notificationId)
                        .build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(EmailNotificationDelivered.class)));
        final EmailNotificationDelivered event = (EmailNotificationDelivered) eventStream.get(0);
        assertThat(event.getMaterialId(), is(materialId));
        assertThat(event.getNotificationId(), is(notificationId));
    }

    @Test
    public void shouldEmitDeliveryFailedEventCarryingTheErrorWhenEmailFailed() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordEmailDeliveryOutcome(
                RecordEmailDeliveryOutcome.recordEmailDeliveryOutcome()
                        .withMaterialId(materialId)
                        .withDelivered(false)
                        .withErrorMessage("Email address is not valid")
                        .withStatusCode(400)
                        .build()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(EmailNotificationDeliveryFailed.class)));
        final EmailNotificationDeliveryFailed event = (EmailNotificationDeliveryFailed) eventStream.get(0);
        assertThat(event.getMaterialId(), is(materialId));
        assertThat(event.getErrorMessage(), is("Email address is not valid"));
        assertThat(event.getStatusCode(), is(400));
    }

}
