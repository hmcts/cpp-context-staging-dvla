package uk.gov.moj.cpp.stagingdvla.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.command.handler.Cases;
import uk.gov.justice.cpp.stagingdvla.command.handler.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.command.handler.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverNotification;
import uk.gov.justice.cpp.stagingdvla.command.handler.NowContent;
import uk.gov.justice.cpp.stagingdvla.command.handler.Results;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverNotificationHandlerTest {

    private static final String STAGINGDVLA_EVENT_DRIVER_NOTIFIED = "stagingdvla.event.driver-notified";

    private static final UUID MASTER_DEFENDANT_ID = randomUUID();
    private static final UUID ORDER_HEARING_ID = randomUUID();
    private static final String COURT_CENTER_NAME = "Liverpool Crown Court";
    private static final String DEFENDANT_FIRST_NAME = "Edward";
    private static final String ORDER_DATE = LocalDate.now().toString();
    private static final String AMENDMENT_DATE = LocalDate.now().toString();
    private static final String START_DATE = LocalDate.now().toString();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();
    private static final String CONVICTING_COURT_CODE = "2577";

    @InjectMocks
    private DriverNotificationHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(DriverNotified.class);

    @Test
    public void shouldHandleDriverNotification() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleDriverNotification")
                        .thatHandles(DriverNotificationHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION)));
    }

    @Test
    public void shouldProcessDriverNotificationAndRaiseEvent() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);

        final Envelope<DriverNotification> envelope = createDriverNotification("J");
        handler.handleDriverNotification(envelope);
        verifyDriverNotificationEventCreated("J");
    }

    @Test
    public void shouldProcessDriverNotificationAndRaiseEventWhenNoInitiationCode() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);

        final Envelope<DriverNotification> envelope = createDriverNotification(null);
        handler.handleDriverNotification(envelope);
        verifyDriverNotificationEventCreated(StringUtils.EMPTY);
    }

    @Test
    void shouldProcessDriverNotificationAndRaiseEventWhenCaseStatusProvided() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);

        final Envelope<DriverNotification> envelope = createDriverNotification("J", "ACTIVE", null);
        handler.handleDriverNotification(envelope);
        verifyDriverNotificationEventCreated("J");
    }

    @Test
    void shouldProcessDriverNotificationAndRaiseEventWhenApplicationTypeIdProvided() throws Exception {
        final DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);

        final Envelope<DriverNotification> envelope = createDriverNotification("J", null, randomUUID().toString());
        handler.handleDriverNotification(envelope);
        verifyDriverNotificationEventCreated("J");
    }

    private Envelope<DriverNotification> createDriverNotification(String initCode){
        return createDriverNotification(initCode, null, null);
    }
    private Envelope<DriverNotification> createDriverNotification(String initCode, String caseStatus, String applicationTypeId) {
        DriverNotification driverNotification = DriverNotification.driverNotification()
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withOrderingCourt(CourtCentre.courtCentre()
                        .withName(COURT_CENTER_NAME).build())
                .withNowContent(NowContent.nowContent()
                        .withOrderDate(ORDER_DATE)
                        .withAmendmentDate(AMENDMENT_DATE)
                        .withDefendant(Nowdefendant.nowdefendant()
                                .withFirstName(DEFENDANT_FIRST_NAME).build())
                        .withCaseApplicationReferences(asList(CASE_ID.toString()))
                        .withCases(asList(Cases.cases()
                                .withReference(CASE_ID.toString())
                                .withInitiationCode(initCode)
                                .withCaseStatus(caseStatus)
                                .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                        .withStartDate(START_DATE)
                                        .withConvictingCourtCode(CONVICTING_COURT_CODE)
                                        .withEndorsableFlag(true)
                                        .withResults(getOffenceResults())
                                        .build()))
                                .build()))
                        .withCourtApplications(asList(CourtApplications.courtApplications()
                                .withId(APPLICATION_ID)
                                .withApplicationTypeId(applicationTypeId)
                                .build()))
                        .build())
                .withOrderingHearingId(ORDER_HEARING_ID)
                .withIsReshare(false)
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(driverNotification)
                .withName(DriverNotificationHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION)
                .withMetadataFrom(requestEnvelope);
    }

    private static List<Results> getOffenceResults() {
        return Collections.singletonList((Results.results()
                .withD20(true)
                .withDrivingTestStipulation(1)
                .withPointsDisqualificationCode("TT99")
                .withResultIdentifier(randomUUID().toString())
                .build()));
    }

    private void verifyDriverNotificationEventCreated(String initCode) throws EventStreamException {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(STAGINGDVLA_EVENT_DRIVER_NOTIFIED),
                        payload().isJson(allOf(
                                withJsonPath("$.orderingCourt.name", is(COURT_CENTER_NAME)),
                                withJsonPath("$.orderDate", is(ORDER_DATE)),
                                withJsonPath("$.amendmentDate", is(AMENDMENT_DATE)),
                                withJsonPath("$.defendant.firstName", is(DEFENDANT_FIRST_NAME)),
                                withJsonPath("$.caseApplicationReferences[0]", is(CASE_ID.toString())),
                                withJsonPath("$.cases[0].reference", is(CASE_ID.toString())),
                                withJsonPath("$.cases[0].initiationCode", is(initCode)),
                                withJsonPath("$.cases[0].defendantCaseOffences[0].startDate", is(START_DATE))
                        ))
                )
        ));
    }
}
