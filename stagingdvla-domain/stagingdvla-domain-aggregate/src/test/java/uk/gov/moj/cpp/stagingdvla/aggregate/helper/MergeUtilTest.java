package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.cpp.stagingdvla.event.CourtApplications.courtApplications;
import static uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences.defendantCaseOffences;
import static uk.gov.justice.cpp.stagingdvla.event.Results.results;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.STDEC;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.G;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.MergeUtil.mergeOffence;

import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MergeUtilTest {

    private static final String ORDER_DATE = "2026-05-15";
    private static final String ORDERING_COURT_CODE = "2578";

    private static final String D20_RESULT_IDENTIFIER = "cee54856-4450-4f28-a8a9-72b688726201";

    // CHD-3361 AC13, BR4, BR7: granted STAT DEC - plea, conviction date and verdict are not back-filled from the previous D20

    static Stream<Arguments> convictionFields() {
        return Stream.of(
                Arguments.of("plea", (Function<DefendantCaseOffences, String>) DefendantCaseOffences::getPlea,
                        (BiFunction<DefendantCaseOffences.Builder, String, DefendantCaseOffences.Builder>) DefendantCaseOffences.Builder::withPlea),
                Arguments.of("convictionDate", (Function<DefendantCaseOffences, String>) DefendantCaseOffences::getConvictionDate,
                        (BiFunction<DefendantCaseOffences.Builder, String, DefendantCaseOffences.Builder>) DefendantCaseOffences.Builder::withConvictionDate),
                Arguments.of("convictionStatus", (Function<DefendantCaseOffences, String>) DefendantCaseOffences::getConvictionStatus,
                        (BiFunction<DefendantCaseOffences.Builder, String, DefendantCaseOffences.Builder>) DefendantCaseOffences.Builder::withConvictionStatus)
        );
    }

    @ParameterizedTest(name = "{0} absent on current offence is not back-filled")
    @MethodSource("convictionFields")
    void shouldNotBackFillConvictionFieldFromPreviousOffenceWhenStatDecGranted(final String field,
                                                                              final Function<DefendantCaseOffences, String> getter,
                                                                              final BiFunction<DefendantCaseOffences.Builder, String, DefendantCaseOffences.Builder> setter) {
        final DefendantCaseOffences previous = setter.apply(defendantCaseOffences().withValuesFrom(offence()), "PREVIOUS").build();

        final DefendantCaseOffences merged = mergeOffence(offence(), previous, ORDER_DATE, ORDERING_COURT_CODE, statDecGrantedContext());

        assertThat(field, getter.apply(merged), is(nullValue()));
    }

    private static DefendantCaseOffences offence() {
        return defendantCaseOffences()
                .withCode("CODE1")
                .withResults(singletonList(results().withResultIdentifier(D20_RESULT_IDENTIFIER).withD20(Boolean.TRUE).build()))
                .build();
    }

    private static CourtApplicationsContext statDecGrantedContext() {
        return new CourtApplicationsContext(singletonList(courtApplications()
                .withApplicationType(STDEC.appType)
                .withResults(singletonList(results().withResultIdentifier(G.id).build()))
                .build()), emptyList());
    }
}
