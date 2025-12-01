package uk.gov.moj.cpp.stagingdvla.aggregate;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.defendantAggregateScenario;

import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregateTestSteps.Scenario;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DefendantAggregateScenariosTest {

    public static Stream<Arguments> simpleScenario() {
        return Stream.of(
                Arguments.of("Case resulted with offences endorsed - Notify Driver",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/case-resulted-events-expected.json"
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