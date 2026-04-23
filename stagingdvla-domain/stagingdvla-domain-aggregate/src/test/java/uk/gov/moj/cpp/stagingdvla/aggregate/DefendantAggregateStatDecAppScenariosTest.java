package uk.gov.moj.cpp.stagingdvla.aggregate;


import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.JsonPathAssertions.jsonPathAssertions;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.defendantAggregateScenario;

import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefendantAggregateStatDecAppScenariosTest {

    public static Stream<Arguments> simpleScenario() {
        return Stream.of(
                Arguments.of("DD-40382-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("removedEndorsements.size()", 1)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40382-ac1A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1A/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1A/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1A/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac1A/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("removedEndorsements.size()", 1)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40382-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac2/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac2/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac2/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac2/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("removedEndorsements.size()", 2)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40382-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac4/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac4/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac4/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40382/ac4/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("removedEndorsements.size()", 1)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("removedEndorsements.size()", 1)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac1A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1A/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1A/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1A/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac1A/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("removedEndorsements.size()", 1)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("removedEndorsements.size()", 2)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac2B",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2B/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2B/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2B/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2B/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("removedEndorsements.size()", 1)
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac2C",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/app-second-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac2C/app-second-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac3/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac3/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac3/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac3/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("previous", notNullValue()))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/app-amended-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4/app-amended-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac4A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("previous", notNullValue()))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/app-amended-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac4A/app-amended-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-40319-ac5",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/case-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/app-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Remove")
                                                .add("previous", notNullValue()))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/app-amended-resulted.json",
                                        "/testdata/drivernotifications/statdec/dd-40319/ac5/app-amended-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))

                )


                // Additional scenarios can be added here
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("simpleScenario")
    void shouldCreateDVLANotificationForStatDecApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}