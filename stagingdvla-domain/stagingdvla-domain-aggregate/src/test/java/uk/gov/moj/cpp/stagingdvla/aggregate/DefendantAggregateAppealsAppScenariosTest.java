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
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac1/app-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac1/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("oatsEndorsements", List.of("IN10" ))
                                                .add("previous", notNullValue())
                                                )

                ),
                Arguments.of("CIMD-3237-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac2/case-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac2/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac2/app-resulted.json",
                                        null
                                )

                ),
                Arguments.of("CIMD-3237-ac4",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac4/case-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac4/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac4/app-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac4/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("oatsEndorsements", List.of("IN10","LC20" ))
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("DD-39832-ac5",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/case-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted with adj",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/app-resulted-adj.json",
                                        null)
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/app-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3237/ac5/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("previous", notNullValue()))

                ),
                Arguments.of("CIMD-3240-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/appeal/cimd-3240/ac1/app-resulted.json",
                                        null)

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