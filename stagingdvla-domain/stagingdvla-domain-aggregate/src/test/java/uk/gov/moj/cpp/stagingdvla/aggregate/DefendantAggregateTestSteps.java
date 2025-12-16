package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.isNull;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.stagingdvla.aggregate.utils.JsonMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.IOUtils;
import org.hamcrest.core.IsNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefendantAggregateTestSteps {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantAggregateTestSteps.class);
    private static final StringToJsonObjectConverter stringToJsonConverter = new StringToJsonObjectConverter();
    private static final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private static final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    static DefendantAggregateTestSteps.Scenario defendantAggregateScenario() {
        return new DefendantAggregateTestSteps.Scenario();
    }

    static class Scenario {
        static final List<String> FIELDS_TO_CHECK_PRESENCE_ONLY = Arrays.asList("identifier", "materialId");
        List<StepData> steps = new ArrayList<>();

        public void run(final String name, final DefendantAggregate aggregate) {
            for (StepData step : steps) {
                LOGGER.info("Running {} step", step.stepName);
                final Stream<Object> eventStream = aggregate.notifyDriver(
                        step.input.orderDate(),
                        step.input.orderingCourt(),
                        step.input.amendmentDate(),
                        step.input.defendant(),
                        step.input.currentCases(),
                        step.input.hearingId(),
                        step.input.courtApplications(),
                        step.input.masterDefendantId(),
                        step.input.isReshare
                );
                if(isNull(step.expectedEventsJsonFile)){
                    assertThat(name + " - No events were produced", eventStream, IsNull.nullValue());
                }else{
                    assertThat(name + " - No events were produced", eventStream, IsNull.notNullValue());
                    final List<String> actualEvents = eventStream
                            .map(objectToJsonObjectConverter::convert)
                            .map(Object::toString)
                            .toList();
                    final String expectedEventsJson = payloadAsString(step.expectedEventsJsonFile(), Map.of());
                    final JsonArray expectedEventsArray = jsonStringToArray(expectedEventsJson);
                    assertThat(actualEvents.size() + " events were produced, expected " + expectedEventsArray.size() + "\nactualEvents:\n" + String.join("\n", actualEvents), actualEvents.size() == expectedEventsArray.size());
                    for (int i = 0; i < expectedEventsArray.size(); i++) {
                        final String expectedEventPayload = objectToJsonObjectConverter.convert(expectedEventsArray.getJsonObject(i)).toString();
                        final String actualEventPayload = actualEvents.get(i);
                        assertThat(actualEventPayload, JsonMatcher.matchesJson(expectedEventPayload, FIELDS_TO_CHECK_PRESENCE_ONLY));
                    }
                }

            }
        }

        public Scenario withNotifyDriverStep(final String stepName, final String notificationJsonFile, final String expectedEventsJsonFile) {
            final JsonObject notification = stringToJsonConverter.convert(payloadAsString(notificationJsonFile, Map.of()));
            final Boolean isReshare = notification.getBoolean("isReshare");
            final JsonObject nowContent = notification.getJsonObject("nowContent");
            final JsonArray cases = nowContent.getJsonArray("cases");
            List<Cases> currentCases = new ArrayList<>();
            for (int i = 0; i < cases.size(); i++) {
                currentCases.add(jsonToObjectConverter.convert(cases.getJsonObject(i), Cases.class));
            }
            List<CourtApplications> courtApplications = new ArrayList<>();
            final JsonArray courtApplicationsArray = nowContent.getJsonArray("courtApplications");
            if (courtApplicationsArray != null) {
                for (int i = 0; i < courtApplicationsArray.size(); i++) {
                    courtApplications.add(jsonToObjectConverter.convert(courtApplicationsArray.getJsonObject(i), CourtApplications.class));
                }
            }

            steps.add(new StepData(stepName,
                    new NotificationData(
                            nowContent.getString("orderDate"),
                            jsonToObjectConverter.convert(notification.getJsonObject("orderingCourt"), CourtCentre.class),
                            nowContent.containsKey("amendmentDate") ? nowContent.getString("amendmentDate") : null,
                            jsonToObjectConverter.convert(nowContent.getJsonObject("defendant"), Nowdefendant.class),
                            currentCases,
                            UUID.fromString(notification.getString("orderingHearingId")),
                            courtApplications,
                            UUID.fromString(notification.getString("masterDefendantId")),
                            isReshare
                    ), expectedEventsJsonFile));
            return this;
        }
    }

    static String payloadAsString(final String path, final Map<String, String> parameters) {
        try (final InputStream resourceAsStream = Scenario.class.getResourceAsStream(path)) {
            assertThat(path, resourceAsStream, IsNull.notNullValue());
            String payload = IOUtils.toString(resourceAsStream, defaultCharset());
            for (final Map.Entry<String, String> entry : parameters.entrySet()) {
                payload = payload.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return payload;
        } catch (final IOException e) {
            throw new AssertionError("Failed to read payload from file" + path, e);
        }
    }

    record StepData(String stepName, NotificationData input, String expectedEventsJsonFile) {
    }

    record NotificationData(String orderDate,
                            CourtCentre orderingCourt,
                            String amendmentDate,
                            Nowdefendant defendant,
                            List<Cases> currentCases,
                            UUID hearingId,
                            List<CourtApplications> courtApplications,
                            UUID masterDefendantId,
                            Boolean isReshare) {

    }

    private static JsonArray jsonStringToArray(final String source) {
        if (source.startsWith("[")) {
            try (final JsonReader reader = Json.createReader(new StringReader(source))) {
                return reader.readArray();
            }
        } else if (source.startsWith("{")) {
            try (final JsonReader reader = Json.createReader(new StringReader(source))) {
                JsonObject obj = reader.readObject();
                return Json.createArrayBuilder().add(obj).build();
            }
        } else {
            throw new IllegalArgumentException("Source is not valid JSON: " + source);
        }
    }
}
