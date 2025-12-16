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

                ),
                Arguments.of("DD-40319-ac2",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2/application-hearing-resulted-events-expected.json"
                                )

                ),
                Arguments.of("DD-40319-ac2A",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2A/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2A/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2A/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2A/application-hearing-resulted-events-expected.json"
                                )

                ),
                Arguments.of("DD-40319-ac2B",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2B/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2B/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2B/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed/ac2B/application-hearing-resulted-events-expected.json"
                                )

                ),
                Arguments.of("DD-40319-AC4 mend and reshare of application result to refused - original endorsements sent as update notification",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4/application-hearing-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing reshared with refused result",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4/application-hearing-resulted-with-refused.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4/application-hearing-resulted-with-refused-events-expected.json"
                                )
                ),
                Arguments.of("DD-40319-AC4A Amend and reshare of application result to adjourned after granted removed - no result on cloned offences - original endorsements sent as update notification",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/application-hearing-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing reshared with adjourned result",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/application-hearing-resulted-with-nexh.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/application-hearing-resulted-with-nexh-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing reshared with adjourned result",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac4A/application-hearing-resulted-with-nexh1.json",
                                        null
                                )
                ),
                Arguments.of("DD-40319-AC5 Amend and reshare of application result to adjourned and cloned offence results adjourned - original endorsements sent as update notification",
                        defendantAggregateScenario()
                                .withNotifyDriverStep(
                                        "case hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac5/case-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac5/case-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing resulted",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac5/application-hearing-resulted.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac5/application-hearing-resulted-events-expected.json"
                                )
                                .withNotifyDriverStep(
                                        "application hearing reshared with adjourned result for application and cloned offences",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac5/application-hearing-resulted-with-nexh.json",
                                        "/testdata/drivernotifications/statdecapp/app-granted-offences-endorsed-remove-result/ac5/application-hearing-resulted-with-nexh-events-expected.json"
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