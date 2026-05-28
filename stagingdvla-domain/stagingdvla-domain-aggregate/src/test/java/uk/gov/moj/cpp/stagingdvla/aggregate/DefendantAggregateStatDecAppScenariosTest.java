package uk.gov.moj.cpp.stagingdvla.aggregate;

import static org.hamcrest.CoreMatchers.notNullValue;
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

class DefendantAggregateStatDecAppScenariosTest {

    public static Stream<Arguments> testScenarios() {
        return scenarios(
                Arguments.of("DD-40382-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac1/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("removedEndorsements.size()", 1)
                                                        .add("previous", notNullValue())))
                ),
                Arguments.of("DD-40382-ac1A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac1A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1A/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac1A/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("removedEndorsements.size()", 1)
                                                        .add("previous", notNullValue())))
                ),
                Arguments.of("DD-40382-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac2/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac2/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("removedEndorsements.size()", 2)
                                                        .add("previous", notNullValue())))
                ),
                Arguments.of("DD-40382-ac3A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac3A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "sjp refer to cc resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A/refer-to-cc-resulted.json",
                                        noExpectedEvents())
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac3A/app-resulted-events.json"))
                ),
                Arguments.of("DD-40382-ac3A1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac3A1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))

                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac3A1/app-resulted-events.json"))
                                .withNotifyDriverStep(
                                        "sjp refer to cc hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A1/refer-to-cc-resulted.json",
                                        noExpectedEvents())
                ),

                Arguments.of("DD-40382-ac3A1_2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac3A1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "sjp refer to cc hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac3A1/app-resulted-events.json"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac3A1/app-resulted.json",
                                        noExpectedEvents())
                ),
                Arguments.of("DD-40382-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac4/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac4/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac4/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40382/ac4/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("removedEndorsements.size()", 1)
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40319-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac1/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("removedEndorsements.size()", 1)
                                                        .add("previous", notNullValue())))
                ),
                Arguments.of("DD-40319-ac1A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac1A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1A/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac1A/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("removedEndorsements.size()", 1)
                                                        .add("previous", notNullValue())))
                ),
                Arguments.of("DD-40319-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("removedEndorsements.size()", 2)
                                                        .add("previous", notNullValue())))
                ),
                Arguments.of("DD-40319-ac2B",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2B/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2B/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2B/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2B/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("removedEndorsements.size()", 1)
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                ),
                Arguments.of("DD-40319-ac2C Both offences have endorsement results in the both application hearings",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/app-second-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C/app-second-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98", "NE98"))))

                ),
                Arguments.of("DD-40319-ac2C1 fist offence resulted with endorsement and second offence is adjourn in the first " +
                                "application hearing. In the adjourn hearing first offence doesn't have any result and second offence has endorsement result",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C1/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C1/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C1/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C1/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("CU84"))))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C1/app-second-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C1/app-second-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("CU80", "CU84"))))
                ),
                Arguments.of("DD-40319-ac2C2 fist offence resulted with endorsement and second offence is adjourn in the first " +
                                "application hearing. In the adjourn hearing first offence has OATS result and second offence has endorsement result",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C2/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C2/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C2/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C2/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)
                                                        .add("updatedEndorsements", List.of("NE98"))))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C2/app-second-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac2C2/app-second-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 1)
                                                        .add("updatedEndorsements", List.of("NE98", "NE98"))))
                ),
                Arguments.of("DD-40319-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac3/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac3/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac3/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac3/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 2)))
                ),
                Arguments.of("DD-40319-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac4/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "New")
                                                        .add("cases[0].defendantCaseOffences.size()", 1)))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac4/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("previous", notNullValue())))
                                .withNotifyDriverStep(
                                        "application hearing amended",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/app-amended-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac4/app-amended-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 0)))
                ),
                Arguments.of("DD-40319-ac4A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac4A/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac4A/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("previous", notNullValue())))
                                .withNotifyDriverStep(
                                        "application hearing amended",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/app-amended-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac4A/app-amended-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 0)))
                ),
                Arguments.of("DD-40319-ac5",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/case-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac5/case-resulted-events.json")
                                                .withAssertions(jsonPathAssertions().add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac5/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("previous", notNullValue())))
                                .withNotifyDriverStep(
                                        "application adjourned hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/app-amended-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac5/app-amended-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("previous", notNullValue())
                                                        .add("cases[0].defendantCaseOffences.size()", 1)
                                                        .add("previous.cases[0].defendantCaseOffences.size()", 0)))
                ),
                Arguments.of("DD-40319-ac6",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac6/case-resulted.json",
                                        noExpectedEvents())
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac6/app-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac6/app-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "New")))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac6/app-amended-resulted.json",
                                        expectedEventsJson("/testdata/drivernotifications/statdec/dd-40319/ac6/app-amended-resulted-events.json")
                                                .withAssertions(jsonPathAssertions()
                                                        .add("notificationType", "Remove")
                                                        .add("previous", notNullValue())))
                )
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("testScenarios")
    void shouldCreateDVLANotificationForStatDecApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}
