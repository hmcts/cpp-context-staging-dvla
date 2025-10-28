package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;

import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.NOTIONAL_PENALTY_POINTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.PENALTY_POINTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.DisqualificationPeriodHelper.getDisqualificationPeriod;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getOffences;
import java.util.LinkedList;
import org.junit.Test;

public class DisqualificationPeriodHelperTest {

    public static final String convictionDate = "2021-01-20";
    public static final String orderDate = "2021-01-21";
    public static final String hearingDate = "2021-01-21";
    private final static String VALUE1 = "someValue1";
    private final static String VALUE3 = "3";


    @Test
    public void shouldTransformDisqualificationPeriodGivenMonthsAndDays() {
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("100 Months 2 days");

        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);

        assertThat(disqualificationPeriod, is(equalTo("080402")));
    }

    @Test
    public void shouldTransformDisqualificationPeriodGivenMaxDays() {
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("365 days");

        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);

        assertThat(disqualificationPeriod, is(equalTo("010000")));
    }

    @Test
    public void shouldTransformDisqualificationPeriodGivenDays() {
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("2 days");

        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);

        assertThat(disqualificationPeriod, is(equalTo("000002")));
    }

    @Test
    public void shouldTransformDisqualificationPeriodGivenMaxWeeks() {
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("99 weeks");

        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);

        assertThat(disqualificationPeriod, is(equalTo("011024")));
    }

    @Test
    public void shouldTransformDisqualificationPeriodGivenAllValues() {

        //given
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("5 years 120 Months 4 weeks 20 days");
        //when
        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);
        //then
        assertThat(disqualificationPeriod, is(equalTo("150117")));
    }

    @Test
    public void shouldTransformDisqualificationPeriodGivenExceedingMaxYears() {
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("100 years 2 days");

        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);

        assertThat(disqualificationPeriod, is(equalTo("1000002")));
    }

    @Test
    public void shouldTransformDisqualificationPeriodGivenWeeks() {
        DefendantCaseOffences defendantCaseOffences = getDefendantCaseOffences("15 Weeks");

        final String disqualificationPeriod = getDisqualificationPeriod(defendantCaseOffences,hearingDate);

        assertThat(disqualificationPeriod, is(equalTo("000315")));
    }

    private DefendantCaseOffences getDefendantCaseOffences(String disqualificationPeriod) {
        return getOffences("", true, "dvlaCode",
                "mainOffenceCode", "offenceCode", true, convictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue(disqualificationPeriod).build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())), "", 2, true, null, hearingDate);

    }
}
