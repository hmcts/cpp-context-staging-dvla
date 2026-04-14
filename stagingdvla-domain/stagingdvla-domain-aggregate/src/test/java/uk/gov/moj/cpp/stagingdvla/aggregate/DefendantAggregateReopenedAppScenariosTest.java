package uk.gov.moj.cpp.stagingdvla.aggregate;


import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.JsonPathAssertions.jsonPathAssertions;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.defendantAggregateScenario;

import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.Scenario;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefendantAggregateReopenedAppScenariosTest {

    public static Stream<Arguments> simpleScenario() {
        return Stream.of(
//                Arguments.of("DD-39832-ac1",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("oatsEndorsements", List.of("NE98" ))
//                                                .add("previous", notNullValue()))
//
//                ),
//                Arguments.of("DD-39832-ac2",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac2/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac2/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac2/app-resulted.json",
//                                        null
//                                )
//
//                ),
//                Arguments.of("DD-39832-ac3",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac3/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac3/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac3/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac3/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("oatsEndorsements", List.of("NE98","NE98" ))
//                                                .add("previous", notNullValue()))
//                ),
//                Arguments.of("DD-39832-ac4a",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac4a/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac4a/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac4a/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac4a/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("oatsEndorsements", List.of("NE98","NE98" ))
//                                                .add("previous", notNullValue()))
//                ),
//                Arguments.of("DD-39832-ac5",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac5/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac5/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted with adj",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac5/app-resulted-adj.json",
//                                        null)
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac5/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac5/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("oatsEndorsements", List.of("NE98","NE98" ))
//                                                .add("previous", notNullValue()))
//
//                ),
//                Arguments.of("DD-39832-ac6",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac6/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac6/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac6/app-resulted.json",
//                                        null)
//                                .withNotifyDriverStep(
//                                        "application hearing amended ",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac6/app-amended-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac6/app-amended-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("oatsEndorsements", List.of("NE98" ))
//                                                .add("previous", notNullValue()))
//
//                ),
//                Arguments.of("DD-39832-ac7",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac7/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac7/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac7/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-39832/ac7/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("oatsEndorsements", List.of("NE98","NE98" ))
//                                                .add("previous", notNullValue()))
//                ),
//                Arguments.of("DD-40326-ac1",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac1/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac1/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac1/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac1/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("previous", notNullValue()))
//                ),
//                Arguments.of("DD-40326-ac2",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac2/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac2/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40326/ac2/app-resulted.json",
//                                        null)
//                ),
//                Arguments.of("DD-40325-ac1",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac1/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac1/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac1/app-resulted.json",
//                                        null)
//                ),
//                Arguments.of("DD-40325-ac2",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac2/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac2/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac2/app-resulted.json",
//                                        null
//                                )
//                ),
//                Arguments.of("DD-40325-ac3",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac3/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac3/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac3/app-resulted.json",
//                                        null
//                                )
//                ),
//                Arguments.of("DD-40325-ac4",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "sjp case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac4/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac4/case-resulted-events.json"
//                                )
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac4/app-resulted.json",
//                                        null
//                                )
//                ),
//                Arguments.of("DD-40325-ac5",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "sjp case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac5/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac5/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac5/referred-to-cc-resulted.json",
//                                        null
//                                )
//                                .withNotifyDriverStep(
//                                        "cc case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40325/ac5/reopen-created-resulted.json",
//                                        null
//                                )
//
//                ),
//                Arguments.of("DD-40358-ac1",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac1/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac1/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac1/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac1/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Remove")
//                                                .add("previous", notNullValue()))
//
//                ),
//                Arguments.of("DD-40358-ac2",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac2/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac2/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac2/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac2/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Remove")
//                                                .add("previous", notNullValue()))),
//                Arguments.of("DD-40358-ac3",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac3/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac3/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac3/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac3/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Remove")
//                                                .add("previous", notNullValue()))),
//
//                Arguments.of("DD-40358-ac4",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac4/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac4/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac4/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40358/ac4/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("previous", notNullValue()))),
//                Arguments.of("DD-40328-ac1",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac1/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac1/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac1/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac1/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("previous", notNullValue())
//                                                .add("cases[0].defendantCaseOffences[0].results.size()", 3)
//                                                .add("cases[0].defendantCaseOffences[0].results[2].label", "Conditional discharge"))),
//                Arguments.of("DD-40328-ac2",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac2/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac2/case-resulted-events.json",
//                                        jsonPathAssertions().add("notificationType", "New"))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac2/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac2/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("previous", notNullValue()))),
//                Arguments.of("DD-40328-ac3",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac3/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac3/case-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "New")
//                                                .add("cases[0].defendantCaseOffences.size()", 1))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac3/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac3/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("cases[0].defendantCaseOffences.size()", 2)
//                                                .add("previous", notNullValue()))),
//                Arguments.of("DD-40328-ac4",
//                        defendantAggregateScenario()
//                                .withNotifyDriverStep(
//                                        "case hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac4/case-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac4/case-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "New")
//                                                .add("cases[0].defendantCaseOffences.size()", 2))
//                                .withNotifyDriverStep(
//                                        "application hearing resulted",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac4/app-resulted.json",
//                                        "/testdata/drivernotifications/reopening/dd-40328/ac4/app-resulted-events.json",
//                                        jsonPathAssertions()
//                                                .add("notificationType", "Update")
//                                                .add("cases[0].defendantCaseOffences.size()", 2)
//                                                .add("previous", notNullValue()))),
                        Arguments.of("DD-41665-ac1",
                                defendantAggregateScenario()
                                        .withNotifyDriverStep(
                                                "case hearing resulted",
                                                "/testdata/drivernotifications/reopening/dd-41665/ac1/case-resulted.json",
                                                "/testdata/drivernotifications/reopening/dd-41665/ac1/case-resulted-events.json",
                                                jsonPathAssertions()
                                                        .add("notificationType", "New")
                                                        .add("cases[0].defendantCaseOffences.size()", 2))
                                        .withNotifyDriverStep(
                                                "application hearing resulted",
                                                "/testdata/drivernotifications/reopening/dd-41665/ac1/app-resulted.json",
                                                "/testdata/drivernotifications/reopening/dd-41665/ac1/app-resulted-events.json",
                                                jsonPathAssertions()
                                                        .add("notificationType", "Update")
                                                        .add("cases[0].defendantCaseOffences.size()", 2)
                                                        .add("previous", notNullValue())))


                // Additional scenarios can be added here
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("simpleScenario")
    void shouldCreateDVLANotificationForReopeningApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}