package uk.gov.moj.cpp.stagingdvla.aggregate;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.defendantAggregateScenario;

import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Disabled
class DefendantAggregateScenariosTest {

    public static Stream<Arguments> simpleScenario() {
        return Stream.of(
                Arguments.of("DD-40319-ac1",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac1/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac1/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac1/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac1/application-hearing-resulted-events-expected.json"
                                )

                )
                // Additional scenarios can be added here
        );
    }

    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("simpleScenario")
    void shouldCreateNCESNotificationForFinCaseSingleOffenceAppAmendments(final String name, final Scenario scenario) {
        assertDoesNotThrow(() -> scenario.run(name, new DefendantAggregate()));
    }
}