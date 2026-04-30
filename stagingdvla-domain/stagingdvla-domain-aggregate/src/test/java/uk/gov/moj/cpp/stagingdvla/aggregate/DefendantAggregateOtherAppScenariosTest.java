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

class DefendantAggregateOtherAppScenariosTest {

    public static Stream<Arguments> simpleScenario() {
        return Stream.of(
                Arguments.of("DD-40345-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/other/dd-40345/ac1/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac1/app-resulted.json",
                                        null)

                ),
                Arguments.of("DD-40345-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2/case-resulted.json",
                                        "/testdata/drivernotifications/other/dd-40345/ac2/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2/app-resulted.json",
                                        "/testdata/drivernotifications/other/dd-40345/ac2/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("cases[0].defendantCaseOffences.size()", 1))
                ),
                Arguments.of("DD-40345-ac2A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A/case-resulted.json",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A/app-resulted.json",
                                        "/testdata/drivernotifications/other/dd-40345/ac2A/app-resulted-events.json",
                                        jsonPathAssertions()
                                                .add("notificationType", "Update")
                                                .add("cases[0].defendantCaseOffences.size()", 1))
                ),
                Arguments.of("DD-40345-ac3",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac3/case-resulted.json",
                                        "/testdata/drivernotifications/other/dd-40345/ac3/case-resulted-events.json",
                                        jsonPathAssertions().add("notificationType", "New"))
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/other/dd-40345/ac3/app-resulted.json",
                                        null)
                )


                // Additional scenarios can be added here
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("simpleScenario")
    void shouldCreateDVLANotificationForOtherApplications(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}