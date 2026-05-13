package uk.gov.moj.cpp.stagingdvla.aggregate;


import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.JsonPathAssertions.jsonPathAssertions;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.defendantAggregateScenario;

import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.Scenario;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefendantAggregateAppealsAppScenariosTest {

    public static Stream<Arguments> simpleScenario() {
        return Stream.of(
                Arguments.of("CIMD-3237-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("oatsEndorsements", List.of("NE98" ))
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("CIMD-3237-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))

                ),
                //Case urn is not matching with the AC, need to confirm the case
                Arguments.of("CIMD-3237-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("oatsEndorsements", List.of("NE98" ))
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("CIMD-3237-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("oatsEndorsements", List.of("NE98" ))
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("CIMD-3240-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/reopening/dd-39832/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("oatsEndorsements", List.of("NE98" ))
                                                .add("previous", notNullValue()))

                )
                // Additional scenarios can be added here
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("simpleScenario")
    void shouldCreateDVLANotificationForReopeningApplication(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}