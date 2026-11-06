package uk.gov.moj.cpp.stagingdvla.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
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
import static uk.gov.justice.services.test.utils.core.matchers.JsonSchemaValidationMatcher.isValidJsonEnvelopeForSchema;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.command.handler.Cases;
import uk.gov.justice.cpp.stagingdvla.command.handler.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.command.handler.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverNotification;
import uk.gov.justice.cpp.stagingdvla.command.handler.NowContent;
import uk.gov.justice.cpp.stagingdvla.command.handler.Prompts;
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

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

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

    private static final Path IT_PAYLOADS = Paths.get("..", "..", "stagingdvla-integration-test", "src", "test", "resources");
    private static final Set<String> SCHEMA_INVALID_IT_PAYLOADS = Set.of(
            "applicationAmendReshare/scenario4a/command2.json",
            "appealAmendReshare/cimd_3240_ac1/command2.json",
            "appealAmendReshare/cimd_3240_ac2/command2.json");

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
                .withJudicialResultCategory(JudicialResultCategory.FINAL)
                .withDrivingTestStipulation(1)
                .withPointsDisqualificationCode("TT99")
                .withResultIdentifier(randomUUID().toString())
                .withPrompts(getPrompts())
                .build()));
    }

    private static List<Prompts> getPrompts() {
        return Collections.singletonList((Prompts.prompts()
                .withPromptIdentifier(randomUUID().toString())
                .withPromptReference("testPromptReference")
                .withDurationSequence(BigDecimal.ONE)
                .withLabel("Test Prompt")
                .withValue("Test Value")
                .withWelshLabel("Test Welsh Label")
                .withWelshValue("Test Welsh Value")
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

    /*
     * CHD-04 (BC-13) - Java 17 behaviour (inferred: the handler schema and the core-domain judicialResultCategory
     * enum are identical in 17.104.4 and 25.104.6): these three IT payloads are rejected by the handler schema
     * (courtApplications[0].applicationSubType is not allowed; judicialResultCategory "Intermediary" is not in
     * FINAL/INTERMEDIARY/ANCILLARY). The ITs that send them expect 0 notifications, so they pass because of the
     * rejection rather than the amend/reshare rule they are named after.
     */
    @Test
    public void chd04_shouldRejectTheThreeAmendReshareItPayloadsAgainstTheHandlerSchema() throws IOException {
        for (final String payload : SCHEMA_INVALID_IT_PAYLOADS) {
            assertThat(payload, driverNotificationHandlerEnvelopeFrom(IT_PAYLOADS.resolve(payload)), not(isValidJsonEnvelopeForSchema()));
        }
    }

    /*
     * CHD-06 (BC-15) - Java 17 behaviour: every other driver-notification payload the ITs send is valid against
     * the handler schema, including the core-domain schemas it references (courtCentre, nowdefendant,
     * courtsDefinitions). A core-domain model forked from a stale base would reject some of them.
     */
    @Test
    public void chd06_shouldAcceptEveryOtherDriverNotificationItPayloadAgainstTheHandlerSchema() throws IOException {
        final List<Path> payloads;
        try (Stream<Path> files = Files.walk(IT_PAYLOADS)) {
            payloads = files
                    .filter(path -> path.getFileName().toString().matches("command.*\\.json"))
                    .filter(path -> !SCHEMA_INVALID_IT_PAYLOADS.contains(IT_PAYLOADS.relativize(path).toString().replace('\\', '/')))
                    .sorted()
                    .toList();
        }
        assertThat(payloads.size(), greaterThan(200));

        final List<String> rejected = new java.util.ArrayList<>();
        for (final Path payload : payloads) {
            if (!isValidJsonEnvelopeForSchema().matches(driverNotificationHandlerEnvelopeFrom(payload))) {
                rejected.add(IT_PAYLOADS.relativize(payload).toString());
            }
        }
        assertThat(rejected, is(empty()));
    }

    private JsonEnvelope driverNotificationHandlerEnvelopeFrom(final Path payloadFile) throws IOException {
        final String json = Files.readString(payloadFile)
                .replace("%DEFENDANT_ID%", randomUUID().toString())
                .replace("%HEARING_ID%", randomUUID().toString());
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            final JsonObject payload = reader.readObject();
            return JsonEnvelope.envelopeFrom(
                    metadataWithRandomUUID(DriverNotificationHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION).build(), payload);
        }
    }
}
