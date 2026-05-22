package uk.gov.moj.cpp.stagingdvla.aggregate;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.ExpectedEventsAssertion.expectedEventsJson;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.ExpectedEventsAssertion.noExpectedEvents;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.JsonPathAssertions.jsonPathAssertions;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.defendantAggregateScenario;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.scenarios;

import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.Scenario;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefendantAggregateAppealsAppScenariosTest {

    public static Stream<Arguments> testScenarios() {
        return scenarios(
                Arguments.of("CIMD-3237-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "New")
                                                        .add("previous", nullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac1/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("oatsEndorsements", List.of("IN10"))
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("LC20"))))
                ),
                Arguments.of("CIMD-3237-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac2/app-resulted.json",
                                        noExpectedEvents()
                                )
                ),
                Arguments.of("CIMD-3237-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac3/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac3/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac3/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac3/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("oatsEndorsements", List.of("IN10", "LC20"))
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 3)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("IN14"))
                                                ))
                ),
                Arguments.of("CIMD-3237-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac4/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac4/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac4/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac4/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("oatsEndorsements", List.of("IN10", "LC20"))
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 3)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("IN14"))))
                ),
                Arguments.of("DD-39832-ac5",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac5/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted with adj",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/app-resulted-adj.json",
                                        noExpectedEvents())
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac5/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 3)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("IN14", "IN10", "LC20"))))
                ),
                Arguments.of("CIMD-3237-ac6",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac6/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3237/ac6/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac6/app-resulted.json",
                                        noExpectedEvents()
                                )
                ),
                Arguments.of("CIMD-3240-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3240/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac1/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("CIMD-3240-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3240/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac2/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("CIMD-3240-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac3/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3240/ac3/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac3/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("cimd-3241-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3241/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3241/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3241/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3241/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("oatsEndorsements", List.of("IN14"))
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 4)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 5)
                                                        .add("updatedEndorsements", List.of("BA10", "IN14", "LC20"))))
                ),
                Arguments.of("cimd-3241-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3241/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3241/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3241/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3241/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("oatsEndorsements", List.of("IN14"))
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 4)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 5)
                                                        .add("updatedEndorsements", List.of("BA10", "IN14", "LC20"))))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3241/app-resulted-2.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3241/app-resulted-events-2.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 4)
                                                        .add("updatedEndorsements", List.of("LC20", "IN14", "IN14", "BA10"))))
                ),
                Arguments.of("cimd-3232-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3232/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3232/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3232/ac1/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3232/ac1/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "Remove")))
                ),
                Arguments.of("cimd-3242-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac1/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac1/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "Remove")))
                ),
                Arguments.of("cimd-3242-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac2/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac2/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "Remove")))
                ),
                Arguments.of("cimd-3242-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac3/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac3/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac3/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac3/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "Remove")))
                ),
                Arguments.of("cimd-3242-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac4/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac4/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3242/ac4/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3242/ac4/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)))
                ),
                Arguments.of("cimd-3236-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "New")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac1/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac1/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("SP40"))))
                ),
                Arguments.of("cimd-3236-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac2/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac2/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 3)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("IN10", "LC20"))))
                ),
                Arguments.of("cimd-3236-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac3/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac3/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac3/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac3/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("LC20"))))
                ),
                Arguments.of("cimd-3236-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac4/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac4/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3236/ac4/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3236/ac4/application-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("IN10", "LC20"))))
                ),
                Arguments.of("CIMD-3230-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3230/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3230/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3230/ac1/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3230/ac1/application-resulted-events.json")
                                                .withAssertions(
                                                        jsonPathAssertions()
                                                                .add("notificationType", "Update")
                                                                .add("previous", notNullValue())
                                                                .add("cases[0].defendantCaseOffences.size()", 2)
                                                                .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                                .add("updatedEndorsements", List.of("IN10", "LC20"))))
                ),
                Arguments.of("cimd-3232-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3232/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3232/ac2/case-resulted-events.json").withAssertions(
                                                jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3232/ac2/application-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3232/ac2/application-resulted-events.json").withAssertions(
                                                jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)))
                ),
                Arguments.of("CIMD-3235-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3235/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3235/ac1/case-resulted-events.json").withAssertions(
                                                jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3235/ac1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/appeal/cimd-3235/ac1/app-resulted-events.json").withAssertions(
                                                jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("LC20", "IN10"))))
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("testScenarios")
    void shouldCreateDVLANotificationForReopeningApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}
