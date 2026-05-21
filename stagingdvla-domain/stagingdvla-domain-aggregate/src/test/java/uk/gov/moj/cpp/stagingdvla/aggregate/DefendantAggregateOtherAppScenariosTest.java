package uk.gov.moj.cpp.stagingdvla.aggregate;

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

class DefendantAggregateOtherAppScenariosTest {

    public static Stream<Arguments> testScenarios() {
        return scenarios(
                Arguments.of("DD-40345-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac1/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("DD-40345-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40345-ac2A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2A/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40345-ac2A1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2A1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2A1/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE99"))))
                ),
                Arguments.of("DD-40345-ac2A2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2A2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A2/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac2A2/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40345-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac3/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40345/ac3/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac3/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("DD-40346-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40346/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40346/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40346/ac1/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("DD-40346-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40346/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40346/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40346/ac2/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40346/ac2/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40346-ac2A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40346/ac2A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40346/ac2A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40346/ac2A/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40346/ac2A/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40349-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40349/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40349/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40349/ac1/app-resulted.json",
                                        noExpectedEvents())

                ),
                Arguments.of("DD-40349-ac1A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40349/ac1A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/other/dd-40349/ac1A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40349/ac1A/app-resulted.json",
                                        noExpectedEvents())
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("testScenarios")
    void shouldCreateDVLANotificationForOtherApplications(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}