package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getCases;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getDefendant;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getOrderingCourt;

import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryCancelled;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryScheduled;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantAggregateTest {

    @InjectMocks
    private DefendantAggregate aggregate;

    private static final UUID CONVICTION_ID = randomUUID();
    private static final UUID MASTER_DEFENDANT_ID = randomUUID();

    @BeforeEach
    public void setUp() {
        aggregate = new DefendantAggregate();
    }

    @Test
    public void shouldNotifyDriver() {
        final UUID materialId = randomUUID();
        final DriverNotified driverNotified = DriverNotified.driverNotified()
                .withMaterialId(materialId)
                .build();
        aggregate.apply(driverNotified);

        final String prefix = "current";
        final List<Cases> currentCases = getCases(prefix, false, null, null, null, 3);
        final List<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), null,
                getDefendant(prefix), currentCases,
                randomUUID(), null, randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotified.class)));
    }

    @Test
    public void shouldNotifyDriverIfUpdated() {
        final UUID materialId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String prefix = "current";
        final List<Cases> cases = getCases(prefix, false, null);
        final DriverNotified driverNotified = DriverNotified.driverNotified()
                .withMaterialId(materialId)
                .withMasterDefendantId(masterDefendantId)
                .withCases(cases)
                .build();

        aggregate.apply(driverNotified);

        final List<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), null,
                getDefendant(prefix), cases,
                randomUUID(), null, randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotified.class)));
        assertThat(((DriverNotified) eventStream.get(0)).getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(((DriverNotified) eventStream.get(0)).getRemovedEndorsements().size(), is(equalTo(0)));
    }

    @Test
    void shouldNotNotifyWhenHearingAmendedButResultOfTheOffenceNotChanged(){

        final UUID materialId = randomUUID();
        final DriverNotified driverNotified = DriverNotified.driverNotified()
                .withMaterialId(materialId)
                .build();
        aggregate.apply(driverNotified);

        final String prefix = "current";
        List<Cases> currentCases = getCases(prefix, false, null, null, "TT99", 3);
        List<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), null,
                getDefendant(prefix), currentCases,
                randomUUID(), null, randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotified.class)));
        DriverNotified driverNotified1 = (DriverNotified) eventStream.get(0);
        assertThat(driverNotified1.getCases().get(0).getDefendantCaseOffences().size(), is(2));


       currentCases = getCases(prefix, false, null, null, "TT99", 3);
       final Stream<Object> events = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), LocalDate.now().toString(),
                getDefendant(prefix), currentCases,
                randomUUID(), null, randomUUID());


       assertThat(events, nullValue());
    }

    @Test
    void shouldNotifyWhenHearingAmendedAndAddedNewResultToTheOffence(){

        final UUID materialId = randomUUID();
        final DriverNotified driverNotified = DriverNotified.driverNotified()
                .withMaterialId(materialId)
                .build();
        aggregate.apply(driverNotified);

        final String prefix = "current";
        List<Cases> currentCases = getCases(prefix, false, null, null, "TT99", 3);
        // There is no result on first result of the hearing
        currentCases = currentCases.stream().map(c -> Cases.cases().withValuesFrom(c)
                .withDefendantCaseOffences(new ArrayList<>(c.getDefendantCaseOffences().stream().map(df -> DefendantCaseOffences.defendantCaseOffences()
                        .withValuesFrom(df)
                        .withResults(null)
                        .build()).toList()))
                .build()).toList();

        Stream<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), null,
                getDefendant(prefix), currentCases,
                randomUUID(), null, randomUUID());

        assertThat(eventStream, nullValue());

        currentCases = getCases(prefix, false, null, null, "TT99", 3);
        final List<Object> events = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), LocalDate.now().toString(),
                getDefendant(prefix), currentCases,
                randomUUID(), null, randomUUID()).toList();


        assertThat(events.size(), is(1));
        assertThat(events.get(0).getClass(), is(equalTo(DriverNotified.class)));
    }

    @Test
    public void shouldNotifyDriverIfRemoved() {
        final UUID materialId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String prefix = "current";
        final List<Cases> cases1 = getCases(prefix, false, null);
        final List<Cases> cases2 = getCases(prefix, false, null);

        final DriverNotified driverNotified = DriverNotified.driverNotified()
                .withMaterialId(materialId)
                .withMasterDefendantId(masterDefendantId)
                .withCases(cases1)
                .build();

        aggregate.apply(driverNotified);

        cases2.get(0).getDefendantCaseOffences().clear();

        final List<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), "amendDate",
                getDefendant(prefix), cases2,
                randomUUID(), null, randomUUID()).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotified.class)));
        assertThat(((DriverNotified) eventStream.get(0)).getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(((DriverNotified) eventStream.get(0)).getRemovedEndorsements().size(), is(equalTo(1)));
    }

    @Test
    public void shouldNotNotifyDriverIfNoOffences() {
        final String prefix = "current";
        final List<Cases> cases = getCases(prefix, false, null);

        cases.get(0).getDefendantCaseOffences().clear();

        final Stream<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), null,
                getDefendant(prefix), cases,
                randomUUID(), null, randomUUID());

        assertThat(eventStream, is(nullValue()));
    }

    @Test
    public void shouldNotifyDriverAndCancelRetryForPrevious_WhenNewNotificationReceivedWhileWaitingRetryForPrevious() {
        aggregate.apply(DriverNotified.driverNotified().withIdentifier(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());
        aggregate.apply(DriverNotifiedNextRetryScheduled.driverNotifiedNextRetryScheduled().withConvictionId(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());

        final String prefix = "current";
        final List<Cases> currentCases = getCases(prefix, false, null, null, null, 3);
        final List<Object> eventStream = aggregate.notifyDriver("orderDate",
                getOrderingCourt(prefix, false), null,
                getDefendant(prefix), currentCases,
                randomUUID(), null, MASTER_DEFENDANT_ID).collect(toList());

        assertThat(eventStream.size(), is(2));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotifiedNextRetryCancelled.class)));
        assertThat(((DriverNotifiedNextRetryCancelled) eventStream.get(0)).getConvictionId(), is(equalTo(CONVICTION_ID)));
        assertThat(((DriverNotifiedNextRetryCancelled) eventStream.get(0)).getMasterDefendantId(), is(equalTo(MASTER_DEFENDANT_ID)));
        assertThat(eventStream.get(1).getClass(), is(equalTo(DriverNotified.class)));
    }

    @Test
    public void shouldScheduleRetry_WhenScheduleCommandReceived() {
        final List<Object> eventStream = aggregate.scheduleNextRetryForDriverNotified(CONVICTION_ID, MASTER_DEFENDANT_ID).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotifiedNextRetryScheduled.class)));
        assertThat(((DriverNotifiedNextRetryScheduled) eventStream.get(0)).getConvictionId(), is(equalTo(CONVICTION_ID)));
        assertThat(((DriverNotifiedNextRetryScheduled) eventStream.get(0)).getMasterDefendantId(), is(equalTo(MASTER_DEFENDANT_ID)));
    }

    @Test
    public void shouldTriggerRetry_WhenTriggerCommandReceived() {
        aggregate.apply(DriverNotified.driverNotified().withIdentifier(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());
        aggregate.apply(DriverNotifiedNextRetryScheduled.driverNotifiedNextRetryScheduled().withConvictionId(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());

        final List<Object> eventStream = aggregate.triggerNextRetryForDriverNotified(CONVICTION_ID, MASTER_DEFENDANT_ID).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotified.class)));
        assertThat(((DriverNotified) eventStream.get(0)).getIdentifier(), is(equalTo(CONVICTION_ID)));
        assertThat(((DriverNotified) eventStream.get(0)).getMasterDefendantId(), is(equalTo(MASTER_DEFENDANT_ID)));
    }

    @Test
    public void shouldNotTriggerRetry_WhenTriggerCommandReceivedButRetryWasAlreadyCancelled() {
        aggregate.apply(DriverNotified.driverNotified().withIdentifier(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());
        aggregate.apply(DriverNotifiedNextRetryCancelled.driverNotifiedNextRetryCancelled().withConvictionId(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());

        final List<Object> eventStream = aggregate.triggerNextRetryForDriverNotified(CONVICTION_ID, MASTER_DEFENDANT_ID).collect(toList());

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldNotTriggerButCancelRetry_WhenTriggerReceivedButIdentifierIsDifferentThanPrevious() {
        aggregate.apply(DriverNotified.driverNotified().withIdentifier(randomUUID()).withMasterDefendantId(MASTER_DEFENDANT_ID).build());
        aggregate.apply(DriverNotifiedNextRetryScheduled.driverNotifiedNextRetryScheduled().withConvictionId(CONVICTION_ID).withMasterDefendantId(MASTER_DEFENDANT_ID).build());

        final List<Object> eventStream = aggregate.triggerNextRetryForDriverNotified(CONVICTION_ID, MASTER_DEFENDANT_ID).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass(), is(equalTo(DriverNotifiedNextRetryCancelled.class)));
        assertThat(((DriverNotifiedNextRetryCancelled) eventStream.get(0)).getConvictionId(), is(equalTo(CONVICTION_ID)));
        assertThat(((DriverNotifiedNextRetryCancelled) eventStream.get(0)).getMasterDefendantId(), is(equalTo(MASTER_DEFENDANT_ID)));
    }
}
