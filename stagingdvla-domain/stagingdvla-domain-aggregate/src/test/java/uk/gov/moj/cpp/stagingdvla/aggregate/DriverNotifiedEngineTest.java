package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.cpp.stagingdvla.event.Results.results;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.CONVICTING_COURT;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DATE_OF_CONVICTION;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DEFAULT_DVLA_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DEFENDANT_DRIVING_LICENCE_NUMBER;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DVLACODE_FOR_OFFENCE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.FINE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.LICENCE_ISSUE_NUMBER;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.LICENCE_PRODUCED_IN_COURT;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.NOTIONAL_PENALTY_POINTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.PENALTY_POINTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.POINTS_DISQUALIFICATION_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AACA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AACD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AASA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AASD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ACSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ADJ;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.APA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ASV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AW;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDDTL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDOTEL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDPL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRAL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRI;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRNL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRVL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DSPA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DSPAS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.RFSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.getDistinctPromptReferences;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.DriverNotifiedEngine.transformDriverNotified;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.CASE_ID;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.checkForEndorsableOffences;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getCaseReferences;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getCases;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getCasesWithMultipleOffences;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getCourtApplications;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getDefendant;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getMultipleCases;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getOrderingCourt;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getPreviousDriverNotified;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getPrompt;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getPromptsForDurationSeqNull;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getPromptsForDurationSeqOne;
import static uk.gov.moj.cpp.stagingdvla.aggregate.utils.AggregateTestHelper.getPromptsForDurationSeqTwo;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;
import uk.gov.justice.cpp.stagingdvla.event.Results;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverNotifiedEngineTest {

    private final static String prefix = "current";
    private final static String prefixForPrevious = "previous";
    public static final String previousOrderDate = "2021-01-20";
    public static final String orderDate = "2021-01-21";
    public static final String amendmentDate = "2021-01-22";
    public static final String previousConvictionDate = "2021-01-20";
    public static final String convictionDate = "2021-01-21";
    private final static CourtCentre crownCourt = getOrderingCourt(prefix, false);
    private final static CourtCentre magsCourt = getOrderingCourt(prefix, true);
    private final static Nowdefendant defendant = getDefendant(prefix);
    private final static List<Cases> cases = getCases(prefix, true, convictionDate);
    private final static List<Cases> casesConvicted = getCases(prefix, true, convictionDate, null, null, 3);
    private final static List<Cases> casesNotConvicted = getCases(prefix, false, null, getPrompt("5"), null, 3);
    private final static List<String> caseReferences = getCaseReferences();
    private final static DriverNotified previous = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false, previousOrderDate);
    private final static Map<String, DriverNotified> previousByCase = new HashMap<>();
    private final static UUID hearingId = randomUUID();

    private final static String promptValue1 = "4 months";
    private final static String promptValue2 = "2 months";
    private final static String promptValueWrong = "21/01/2021";
    private final static List<Prompts> promptsForDurationSeqOne = getPromptsForDurationSeqOne(promptValue1);
    private final static List<Prompts> promptsForWrongFormatDurationSeqOne = getPromptsForDurationSeqOne(promptValueWrong);
    private final static List<Prompts> promptsForNullDurationSeqOne = getPromptsForDurationSeqNull(promptValue1);
    private final static List<Prompts> promptsForDurationSeqTwo = getPromptsForDurationSeqTwo(promptValue1, promptValue2);
    private final static List<Cases> casesWithDurationSeqOnePrompt = getCases(true, convictionDate, promptsForDurationSeqOne, "A");
    private final static List<Cases> casesWithDurationSeqOneWrongFormatPrompt = getCases(true, convictionDate, promptsForWrongFormatDurationSeqOne, "A");
    private final static List<Cases> casesWithDurationSeqOneNullPrompt = getCases(true, convictionDate, promptsForNullDurationSeqOne, "A");
    private final static List<Cases> casesWithDurationSeqTwoPromptDvlaCodeA = getCases(true, convictionDate, promptsForDurationSeqTwo, "A");
    private final static List<Cases> casesWithDurationSeqTwoPromptDvlaCodeC = getCases(true, convictionDate, promptsForDurationSeqTwo, "C");

    private final static String VALUE1 = "someValue1";
    private final static String VALUE2 = "someValue2";
    private final static String VALUE3 = "3";
    private final static String NORMAL_FORMATTED_DATE = "2020-01-17";
    private final static String PROMPT_FORMATTED_DATE = "17/01/2020";

    private final static String APP1 = "APP1";

    private final static String SS30 = "SS30";
    private final static String SS40 = "SS40";
    private final static String SS50 = "SS50";
    private final static String SS60 = "SS60";

    private final static String C_DVLA1 = "C_DVLA1";
    private final static String C_DVLA2 = "C_DVLA2";
    private final static String C_DVLA3 = "C_DVLA3";
    private final static String P_DVLA1 = "P_DVLA1";
    private final static String P_DVLA2 = "P_DVLA2";
    private final static String P_DVLA3 = "P_DVLA3";

    private final static String OFF1 = "Off1";
    private final static String OFF2 = "Off2";
    private final static String OFF3 = "Off3";
    private final static String OFF4 = "Off4";

    private final static String TT99 = "TT99";
    private final static String OFF1_TT99 = "Off1TT99";
    private final static String OFF2_TT99 = "Off2TT99";

    @BeforeEach
    public void setup() {
        previousByCase.clear();
    }

    @Test
    public void shouldTransformDriverNotified() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<DriverNotified> driverNotifiedList = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                cases, hearingId, null);

        final DriverNotified transformed = driverNotifiedList.get(0);

        assertThat(transformed.getOrderDate(), is(equalTo(orderDate)));
        assertThat(transformed.getAmendmentDate(), is(equalTo(amendmentDate)));

        assertThat(transformed.getOrderingCourt().getName(), is(equalTo(crownCourt.getName())));
        assertThat(transformed.getOrderingCourt().getCode(), is(equalTo(crownCourt.getCode())));
        assertThat(transformed.getOrderingCourt().getAddress().getAddress1(), is(equalTo(crownCourt.getAddress().getAddress1())));

        assertThat(transformed.getDefendant().getFirstName(), is(equalTo(defendant.getFirstName())));
        assertThat(transformed.getDefendant().getAddress().getLine1(), is(equalTo(defendant.getAddress().getLine1())));

        assertThat(transformed.getCases().get(0).getCaseId(), is(equalTo(cases.get(0).getCaseId())));
        assertThat(transformed.getCases().get(0).getReference(), is(equalTo(cases.get(0).getReference())));

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getWording(), is(equalTo(cases.get(0).getDefendantCaseOffences().get(0).getWording())));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getStartDate(), is(equalTo(cases.get(0).getDefendantCaseOffences().get(0).getStartDate())));

        assertThat(transformed.getCaseApplicationReferences().containsAll(caseReferences), is(true));
        assertThat(transformed.getOrderingHearingId().toString(), is(equalTo(hearingId.toString())));

        IntStream.range(0, cases.size()).forEach(caseIndex -> {
            final Cases originalCase = cases.get(caseIndex);
            final Cases transformedCase = transformed.getCases().get(caseIndex);

            IntStream.range(0, originalCase.getDefendantCaseOffences().size()).forEach(offenceIndex -> {
                final DefendantCaseOffences originalOffence = originalCase.getDefendantCaseOffences().get(offenceIndex);
                final DefendantCaseOffences transformedOffence = transformedCase.getDefendantCaseOffences().get(offenceIndex);

                assertThat(transformedOffence.getConvictingCourtCode(), is(equalTo(originalOffence.getConvictingCourtCode())));
            });

        });
        assertNotNull(transformed.getIdentifier());
    }

    @Test
    public void shouldTransformDriverNotifiedWithCorrectOrderingAndConvictingCourtAndDateInformation() {
        DriverNotified previousNullCurrentConvictedCrown = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                casesConvicted, hearingId, null).get(0);

        assertThat(previousNullCurrentConvictedCrown.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNullCurrentConvictedCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(convictionDate)));
        assertThat(previousNullCurrentConvictedCrown.getOrderingCourtCode(), is(equalTo(crownCourt.getCourtLocationCode())));

        DriverNotified previousNullCurrentAmendedConvictedMags = transformDriverNotified(
                previousByCase, orderDate, magsCourt, orderDate, defendant,
                casesConvicted, hearingId, null).get(0);

        assertThat(previousNullCurrentAmendedConvictedMags.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNullCurrentAmendedConvictedMags.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(convictionDate)));
        assertThat(previousNullCurrentAmendedConvictedMags.getOrderingCourtCode(), is(equalTo(magsCourt.getLja().getLjaCode())));

        DriverNotified previousNullCurrentNotConvictedCrown = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesNotConvicted, hearingId, null).get(0);

        assertThat(previousNullCurrentNotConvictedCrown.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNullCurrentNotConvictedCrown.getConvictionDate(), is(equalTo(null)));
        assertThat(previousNullCurrentNotConvictedCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(null)));
        assertThat(previousNullCurrentNotConvictedCrown.getOrderingCourtCode(), is(equalTo(crownCourt.getCourtLocationCode())));
        assertThat(previousNullCurrentNotConvictedCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode(), is(equalTo(null)));


        DriverNotified previousNullCurrentConvictedInDifferentDateCrown = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                getCases(prefix, true, previousConvictionDate, null, null, 3), hearingId, null).get(0);

        assertThat(previousNullCurrentConvictedInDifferentDateCrown.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNullCurrentConvictedInDifferentDateCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(previousConvictionDate)));
        assertThat(previousNullCurrentConvictedInDifferentDateCrown.getOrderingCourtCode(), is(equalTo(crownCourt.getCourtLocationCode())));


        DriverNotified previousNullCurrentConvictedAmendedInDifferentDateCrown = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                getCases(prefix, true, previousConvictionDate, null, null, 3), hearingId, null).get(0);

        assertThat(previousNullCurrentConvictedAmendedInDifferentDateCrown.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNullCurrentConvictedAmendedInDifferentDateCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(previousConvictionDate)));
        assertThat(previousNullCurrentConvictedAmendedInDifferentDateCrown.getOrderingCourtCode(), is(equalTo(crownCourt.getCourtLocationCode())));


        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, true, previousOrderDate);
        previousByCase.put(previous1.getCases().get(0).getReference(),  previous1);

        DriverNotified previousNotNullAndBothConvictedMags = transformDriverNotified(
                previousByCase, orderDate, magsCourt, null, defendant,
                casesConvicted, hearingId, null).get(0);

        assertThat(previousNotNullAndBothConvictedMags.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNotNullAndBothConvictedMags.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(convictionDate)));
        assertThat(previousNotNullAndBothConvictedMags.getOrderingCourtCode(), is(equalTo(magsCourt.getLja().getLjaCode())));
        assertThat(previousNotNullAndBothConvictedMags.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode(), is(equalTo("previousConvictingCourtCode")));


        DriverNotified previous2 = getPreviousDriverNotified(prefixForPrevious, false, null, false, previousOrderDate);
        previousByCase.put(previous2.getCases().get(0).getReference(),  previous2);

        DriverNotified previousNotNullAndBothNotConvictedCrown = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesNotConvicted, hearingId, null).get(0);

        assertThat(previousNotNullAndBothNotConvictedCrown.getOrderDate(), is(equalTo(orderDate)));
        assertThat(previousNotNullAndBothNotConvictedCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(null)));
        assertThat(previousNotNullAndBothNotConvictedCrown.getOrderingCourtCode(), is(equalTo(crownCourt.getCourtLocationCode())));
        assertThat(previousNotNullAndBothNotConvictedCrown.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode(), is(equalTo(null)));
    }

    @Test
    public void shouldTransformAndAddDistinctPrompts() {

        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(EMPTY).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build(),
                        Prompts.prompts().withPromptReference("PromptIdentifierForNotDistinctPrompt1").withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference("PromptIdentifierForNotDistinctPrompt2").withValue(VALUE2).build()));

        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(prefixForPrevious.concat(DEFENDANT_DRIVING_LICENCE_NUMBER)).build(),
                        Prompts.prompts().withPromptReference(LICENCE_ISSUE_NUMBER).withValue(prefixForPrevious.concat(LICENCE_ISSUE_NUMBER)).build(),
                        Prompts.prompts().withPromptReference(DATE_OF_CONVICTION).withValue(prefixForPrevious.concat(DATE_OF_CONVICTION)).build(),
                        Prompts.prompts().withPromptReference(CONVICTING_COURT).withValue(prefixForPrevious.concat(CONVICTING_COURT)).build()), false, previousOrderDate);

        previousByCase.put(previous1.getCases().get(0).getReference(),  previous1);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                cases1, hearingId, null).get(0);

        assertThat(transformed.getDistinctPrompts().size(), is(equalTo(5)));

        List<String> promptReferences = new ArrayList<>();
        transformed.getDistinctPrompts().forEach(prompt -> {
            assertTrue(isNotEmpty(prompt.getValue()));
            promptReferences.add(prompt.getPromptReference());
        });

        assertArrayEquals(promptReferences.stream().sorted().toArray(),
                getDistinctPromptReferences().stream().sorted().toArray());

        assertThat(transformed.getPrevious().getDistinctPrompts(), is(equalTo(null)));

    }

    @Test
    public void shouldTransformWithNoDistinctPrompts() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate);
        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false,  previousOrderDate);
        previousByCase.put(previous1.getCases().get(0).getReference(),  previous1);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                cases1, hearingId, null).get(0);

        assertThat(transformed.getDistinctPrompts().size(), is(equalTo(0)));
        assertThat(transformed.getPrevious().getDistinctPrompts(), is(equalTo(null)));
    }

    @Test
    public void shouldTransformAndMergeOffenceAndResults_WhenCurrentOffenceHasOption1AttributesAndPreviousHasOption2Attributes() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(prefix.concat(DEFENDANT_DRIVING_LICENCE_NUMBER)).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build()), null, 1);

        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(LICENCE_ISSUE_NUMBER).withValue(prefixForPrevious.concat(LICENCE_ISSUE_NUMBER)).build()), 2, false, previousOrderDate);
        previousByCase.put(previous1.getCases().get(0).getReference(),  previous1);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases1, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getResults().get(0).getPrompts().size(), is(equalTo(2)));
        assertThat(transformed.getDistinctPrompts().size(), is(equalTo(3)));

        DefendantCaseOffences transformedOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences currentOffence = cases1.get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences previousOffence = previous1.getCases().get(0).getDefendantCaseOffences().get(0);

        assertOffenceMergedAttributes(transformedOffence, currentOffence, previousOffence);
        assertOffenceNonMergedAttributes(transformedOffence, currentOffence);

        assertResultMergedAttributes(transformedOffence.getResults().get(0),
                currentOffence.getResults().get(0),
                previousOffence.getResults().get(0));
        assertResultNonMergedAttributes(transformedOffence.getResults().get(0),
                currentOffence.getResults().get(0));
    }

    @Test
    public void shouldTransformAndMergeOffenceAndResults_WhenCurrentOffenceHasOption2AttributesAndPreviousHasOption1Attributes() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(LICENCE_ISSUE_NUMBER).withValue(prefixForPrevious.concat(LICENCE_ISSUE_NUMBER)).build()), null, 2);

        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(prefix.concat(DEFENDANT_DRIVING_LICENCE_NUMBER)).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build()), false, previousOrderDate);
        previousByCase.put(previous1.getCases().get(0).getReference(),  previous1);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases1, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getResults().get(0).getPrompts().size(), is(equalTo(1)));
        assertThat(transformed.getDistinctPrompts().size(), is(equalTo(3)));

        DefendantCaseOffences transformedOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences currentOffence = cases1.get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences previousOffence = previous1.getCases().get(0).getDefendantCaseOffences().get(0);

        assertOffenceMergedAttributes(transformedOffence, previousOffence, currentOffence);
        assertOffenceNonMergedAttributes(transformedOffence, currentOffence);

        assertResultMergedAttributes(transformedOffence.getResults().get(0),
                previousOffence.getResults().get(0),
                currentOffence.getResults().get(0));
        assertResultNonMergedAttributes(transformedOffence.getResults().get(0),
                currentOffence.getResults().get(0));
    }

    private void assertOffenceMergedAttributes(final DefendantCaseOffences transformed, final DefendantCaseOffences option1, final DefendantCaseOffences option2) {
        assertThat(transformed.getTitle(), is(equalTo(option1.getTitle())));
        assertThat(transformed.getCivilOffence(), is(equalTo(option2.getCivilOffence())));
        assertThat(transformed.getStartDate(), is(equalTo(option1.getStartDate())));
        assertThat(transformed.getEndDate(), is(equalTo(option2.getEndDate())));
        assertThat(transformed.getPlea(), is(equalTo(option1.getPlea())));
        assertThat(transformed.getVehicleRegistration(), is(equalTo(option2.getVehicleRegistration())));
        assertThat(transformed.getModeOfTrial(), is(equalTo(option2.getModeOfTrial())));
        assertThat(transformed.getAllocationDecision(), is(equalTo(option1.getAllocationDecision())));
        assertThat(transformed.getWording(), is(equalTo(option2.getWording())));
        assertThat(transformed.getAlcoholReadingAmount(), is(equalTo(option2.getAlcoholReadingAmount())));
        assertThat(transformed.getAlcoholReadingMethodCode(), is(equalTo(option2.getAlcoholReadingMethodCode())));
        assertThat(transformed.getAlcoholReadingMethodDescription(), is(equalTo(option2.getAlcoholReadingMethodDescription())));
        assertThat(transformed.getEndorsableFlag(), is(equalTo(option2.getEndorsableFlag())));
    }

    private void assertOffenceNonMergedAttributes(final DefendantCaseOffences transformed, final DefendantCaseOffences current) {
        assertThat(transformed.getMainOffenceCode(), is(equalTo(current.getMainOffenceCode())));
        assertThat(transformed.getCode(), is(equalTo(current.getCode())));
        assertTrue((isEmpty(current.getDvlaCode()) && DEFAULT_DVLA_CODE.equalsIgnoreCase(transformed.getDvlaCode()))
                || current.getDvlaCode().equalsIgnoreCase(transformed.getDvlaCode()));
        assertThat(transformed.getConvictionDate(), is(equalTo(current.getConvictionDate())));
        assertThat(transformed.getConvictingCourtCode(), is(equalTo(current.getConvictingCourtCode())));
        assertThat(transformed.getConvictionStatus(), is(equalTo(current.getConvictionStatus())));
    }

    private void assertResultMergedAttributes(final Results transformed, final Results option1, final Results option2) {
        assertThat(transformed.getNowRequirementText().get(0).getLabel(), is(equalTo(option1.getNowRequirementText().get(0).getLabel())));
        assertThat(transformed.getNowRequirementText().get(0).getValue(), is(equalTo(option1.getNowRequirementText().get(0).getValue())));
        assertThat(transformed.getPublishedForNows(), is(equalTo(option2.getPublishedForNows())));
        assertThat(transformed.getResultWording(), is(equalTo(option1.getResultWording())));
        assertThat(transformed.getResultDefinitionGroup(), is(equalTo(option2.getResultDefinitionGroup())));
        assertThat(transformed.getSequence(), is(equalTo(option1.getSequence())));
        assertThat(transformed.getDrivingTestStipulation(), is(equalTo(option1.getDrivingTestStipulation())));
    }

    private void assertResultNonMergedAttributes(final Results transformed, final Results current) {
        assertThat(transformed.getLabel(), is(equalTo(current.getLabel())));
        assertThat(transformed.getResultIdentifier(), is(equalTo(current.getResultIdentifier())));
        assertThat(transformed.getPointsDisqualificationCode(), is(equalTo(current.getPointsDisqualificationCode())));
        assertThat(transformed.getD20(), is(equalTo(current.getD20())));
    }

    @Test
    public void shouldTransformAndMergePrompts() {

        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(EMPTY).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build(),
                        Prompts.prompts().withPromptReference("PromptIdentifierForNotDistinctPrompt1").withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference("PromptIdentifierForNotDistinctPrompt2").withValue(VALUE2).build()));

        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(prefixForPrevious.concat(DEFENDANT_DRIVING_LICENCE_NUMBER)).build(),
                        Prompts.prompts().withPromptReference(LICENCE_ISSUE_NUMBER).withValue(prefixForPrevious.concat(LICENCE_ISSUE_NUMBER)).build()), false, previousOrderDate);
        previousByCase.put(previous1.getCases().get(0).getReference(),  previous1);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases1, hearingId, null).get(0);

        List<Prompts> promptList = transformed.getCases().get(0).getDefendantCaseOffences().get(0).getResults().get(0).getPrompts();
        assertThat(promptList.size(), is(equalTo(4)));

        promptList.forEach(prompt -> {
            assertTrue(isNotEmpty(prompt.getValue()));
            if (prompt.getPromptReference().equals(DEFENDANT_DRIVING_LICENCE_NUMBER)) {
                assertThat(prompt.getValue(), is(equalTo(prefixForPrevious.concat(DEFENDANT_DRIVING_LICENCE_NUMBER))));
            } else if (prompt.getPromptReference().equals(LICENCE_PRODUCED_IN_COURT)) {
                assertThat(prompt.getValue(), is(equalTo(prefix.concat(LICENCE_PRODUCED_IN_COURT))));
            }
        });

    }

    @Test
    public void shouldTransformAndGetDvlaCodeFromAttribute() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(EMPTY).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build(),
                        Prompts.prompts().withPromptReference(DVLACODE_FOR_OFFENCE).withValue("dvlaCodeInPrompt").build()),
                null, 1);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases1, hearingId, null).get(0);

        assertTrue(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getDvlaCode().equalsIgnoreCase("currentcurrentDVLACode1"));
    }

    @Test
    public void shouldTransformAndGetDvlaCodeFromPrompt() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(EMPTY).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build(),
                        Prompts.prompts().withPromptReference(DVLACODE_FOR_OFFENCE).withValue("dvlaCodeInPrompt").build()),
                null, 2);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases1, hearingId, null).get(0);

        assertTrue(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getDvlaCode().equalsIgnoreCase("dvlaCodeInPrompt"));
    }

    @Test
    public void shouldTransformAndGetDvlaCodeFromDefaultValue() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(DEFENDANT_DRIVING_LICENCE_NUMBER).withValue(EMPTY).build(),
                        Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefix.concat(LICENCE_PRODUCED_IN_COURT)).build()),
                null, 2);

        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases1, hearingId, null).get(0);

        assertTrue(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getDvlaCode().equalsIgnoreCase(DEFAULT_DVLA_CODE));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPointsDisqualificationIsImposed() {

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("3 Years").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases2, hearingId, null).get(0);

        DefendantCaseOffences mainOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences pointsDisqOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(1);

        assertThat(mainOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(mainOffence.getCode(), is(equalTo("OffenceCode1")));
        assertThat(mainOffence.getDvlaCode(), is(equalTo("currentcurrentDVLACode1")));
        assertThat(mainOffence.getDisqualificationPeriod(), is(equalTo(null)));
        assertThat(mainOffence.getDateDisqSuspendedPendingAppeal(), is(equalTo(null)));

        assertThat(pointsDisqOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(pointsDisqOffence.getCode(), is(equalTo("OffenceCode1TT99")));
        assertThat(pointsDisqOffence.getDvlaCode(), is(equalTo(TT99)));
        assertThat(pointsDisqOffence.getSentencingCourtCode(), is(equalTo("0433")));
        assertThat(pointsDisqOffence.getDisqualificationPeriod(), is(equalTo("030000")));
        assertThat(pointsDisqOffence.getSentenceDate(), is(equalTo(orderDate)));
    }

    @Test
    public void shouldGetFormattedDisqualificationPeriodWhenGivenOnlyMonthsDays() {

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("20 Months 20 days").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases2, hearingId, null).get(0);

        DefendantCaseOffences mainOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences pointsDisqOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(1);

        assertThat(mainOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(mainOffence.getCode(), is(equalTo("OffenceCode1")));
        assertThat(mainOffence.getDvlaCode(), is(equalTo("currentcurrentDVLACode1")));
        assertThat(mainOffence.getDisqualificationPeriod(), is(equalTo(null)));
        assertThat(mainOffence.getDateDisqSuspendedPendingAppeal(), is(equalTo(null)));

        assertThat(pointsDisqOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(pointsDisqOffence.getCode(), is(equalTo("OffenceCode1TT99")));
        assertThat(pointsDisqOffence.getDvlaCode(), is(equalTo(TT99)));
        assertThat(pointsDisqOffence.getSentencingCourtCode(), is(equalTo("0433")));
        assertThat(pointsDisqOffence.getDisqualificationPeriod(), is(equalTo("002020")));
        assertThat(pointsDisqOffence.getSentenceDate(), is(equalTo(orderDate)));
    }

    @Test
    public void shouldGetFormattedDisqualificationPeriodWhenGivenYearsMonthsWeeksDays() {

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("5 years 20 Months 4 weeks 20 days").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases2, hearingId, null).get(0);

        DefendantCaseOffences mainOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences pointsDisqOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(1);

        assertThat(pointsDisqOffence.getSentencingCourtCode(), is(equalTo("0433")));
        assertThat(pointsDisqOffence.getDisqualificationPeriod(), is(equalTo("052048")));
        assertThat(pointsDisqOffence.getSentenceDate(), is(equalTo(orderDate)));
    }

    @Test
    public void shouldGetFormattedDisqualificationPeriodWhenGivenOnlyWeeksDays() {

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("4 weeks 15 days").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases2, hearingId, null).get(0);

        DefendantCaseOffences mainOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences pointsDisqOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(1);

        assertThat(pointsDisqOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(pointsDisqOffence.getCode(), is(equalTo("OffenceCode1TT99")));
        assertThat(pointsDisqOffence.getDvlaCode(), is(equalTo(TT99)));
        assertThat(pointsDisqOffence.getSentencingCourtCode(), is(equalTo("0433")));
        assertThat(pointsDisqOffence.getDisqualificationPeriod(), is(equalTo("000043")));
        assertThat(pointsDisqOffence.getSentenceDate(), is(equalTo(orderDate)));
    }

    @Test
    public void shouldGetFormattedDisqualificationPeriodWhenGivenAllParametersWithinMinMaxRange() {

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("40 years 20 months 4 weeks 15 days").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases2, hearingId, null).get(0);

        DefendantCaseOffences mainOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences pointsDisqOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(1);

        assertThat(pointsDisqOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(pointsDisqOffence.getCode(), is(equalTo("OffenceCode1TT99")));
        assertThat(pointsDisqOffence.getDvlaCode(), is(equalTo(TT99)));
        assertThat(pointsDisqOffence.getSentencingCourtCode(), is(equalTo("0433")));
        assertThat(pointsDisqOffence.getDisqualificationPeriod(), is(equalTo("402043")));
        assertThat(pointsDisqOffence.getSentenceDate(), is(equalTo(orderDate)));
    }

    @Test
    public void shouldTransformDriverNotified_WhenDistinctPromptsPresent() {
        List<Cases> cases1 = getCases(prefix, true, previousConvictionDate,  asList(Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefixForPrevious.concat(VALUE2)).build()));

        DriverNotified previous1 = getPreviousDriverNotified(prefixForPrevious, true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(prefixForPrevious.concat(VALUE1)).build()), false, previousOrderDate);

        previousByCase.put(previous1.getCases().get(0).getReference(), previous1);
        DriverNotified transformedPrevious = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases1, hearingId, null).get(0);

        String distinctPromptReferencePrevious = transformedPrevious.getDistinctPrompts().get(0).getPromptReference();
        assertThat(distinctPromptReferencePrevious, is(equalTo(LICENCE_PRODUCED_IN_COURT)));
        String previousValue = transformedPrevious.getDistinctPrompts().get(0).getValue();
        assertThat(previousValue, is(equalTo(prefixForPrevious.concat(VALUE2))));

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                asList(Prompts.prompts().withPromptReference(LICENCE_PRODUCED_IN_COURT).withValue(VALUE1).build()), null);

        previousByCase.clear();
        previousByCase.put(transformedPrevious.getCases().get(0).getReference(), previous1);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases2, hearingId, null).get(0);

        String distinctPromptReference = transformed.getDistinctPrompts().get(0).getPromptReference();
        assertThat(distinctPromptReference, is(equalTo(LICENCE_PRODUCED_IN_COURT)));
        String value = transformed.getDistinctPrompts().get(0).getValue();
        assertThat(value, is(equalTo(VALUE1)));
    }

    @Test
    public void shouldTransformDriverNotifiedWithDurationSequenceNull() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesWithDurationSeqOneNullPrompt, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getOtherSentence(), is(equalTo("A000")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getSuspendedSentence(), is(nullValue()));
    }

    @Test
    public void shouldTransformDriverNotifiedWithDurationSequenceOne() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesWithDurationSeqOnePrompt, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getOtherSentence(), is(equalTo("A04M")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getSuspendedSentence(), is(nullValue()));
    }

    @Test
    public void shouldTransformDriverNotifiedWithNullDurationSequenceOne() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesWithDurationSeqOneNullPrompt, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getOtherSentence(), is(equalTo("A000")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getSuspendedSentence(), is(nullValue()));
    }

    @Test
    public void shouldTransformDriverNotifiedWithWrongFormatDurationSequenceOne() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesWithDurationSeqOneWrongFormatPrompt, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getOtherSentence(), is(equalTo("A000")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getSuspendedSentence(), is(nullValue()));
    }

    @Test
    public void shouldTransformDriverNotifiedWithDurationSequenceTwoDvlaCodeA() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesWithDurationSeqTwoPromptDvlaCodeA, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getOtherSentence(), is(equalTo("A04M")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getSuspendedSentence(), is(nullValue()));
    }

    @Test
    public void shouldTransformDriverNotifiedWithDurationSequenceTwoDvlaCodeC() {
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                casesWithDurationSeqTwoPromptDvlaCodeC, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getOtherSentence(), is(equalTo("C04M")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getSuspendedSentence(), is(equalTo("02M")));
    }

    @Test
    public void shouldTransformWithCorrectUpdatedAndRemovedEndorsementsList() {
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE), asList(C_DVLA1), false, null, null, null, 1, false);
        DriverNotified previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(2)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA1)));

        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.TRUE, Boolean.FALSE), asList(C_DVLA1, C_DVLA2), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(3, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2, P_DVLA3), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(P_DVLA1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA2)));


        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.FALSE, Boolean.TRUE), null, false, null, null, null, 2, false);
        previous = getPreviousDriverNotified(3, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2, P_DVLA3), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(P_DVLA2)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA1)));

        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.FALSE, Boolean.FALSE), asList(C_DVLA1, C_DVLA2), false, null, null, null, 2, false);
        previous = getPreviousDriverNotified(3, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2, P_DVLA3), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(3)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA1)));
        assertThat(transformed.getRemovedEndorsements().get(1), is(equalTo(P_DVLA2)));


        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE), asList(C_DVLA1), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE), null, false, null, false, null, 2, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(DEFAULT_DVLA_CODE)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));


        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.FALSE, Boolean.FALSE), null, false, null, null, null, 2, false);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE), null, false, null, false, null, 2, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(2)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(DEFAULT_DVLA_CODE)));
        assertThat(transformed.getRemovedEndorsements().get(1), is(equalTo(DEFAULT_DVLA_CODE)));


        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE), asList(C_DVLA1), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(2)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA1)));


        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.TRUE, Boolean.FALSE), asList(C_DVLA1, C_DVLA2), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(3, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2, P_DVLA3), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(previous.getCases().get(0).getDefendantCaseOffences().get(0).getDvlaCode())));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA2)));


        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.FALSE, Boolean.TRUE), asList(C_DVLA1, C_DVLA2), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(3, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2, P_DVLA3), false, null, false, null, 1, false, previousOrderDate);
        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(previous.getCases().get(0).getDefendantCaseOffences().get(1).getDvlaCode())));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA1)));


        cases = getCasesWithMultipleOffences(3, EMPTY, asList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE), asList(C_DVLA1, C_DVLA2, C_DVLA3), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(3, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE), null, false, null, false, null, 2, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(2)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(DEFAULT_DVLA_CODE)));
        assertThat(transformed.getUpdatedEndorsements().get(1), is(equalTo(DEFAULT_DVLA_CODE)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(DEFAULT_DVLA_CODE)));


        cases = getCasesWithMultipleOffences(3, EMPTY, asList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE), null, false, null, null, null, 2, false);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE), asList(P_DVLA1, P_DVLA2), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(previous.getCases().get(0).getDefendantCaseOffences().get(0).getDvlaCode())));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(P_DVLA2)));


        cases = getCasesWithMultipleOffences(3, EMPTY, asList(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE), null, false, null, null, null, 2, false);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE), null, false, null, false, null, 2, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(DEFAULT_DVLA_CODE)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(DEFAULT_DVLA_CODE)));


        cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS50, SS60), asList(OFF3, OFF4), asList(OFF3, OFF4), false, null, null, null, 1, false, null);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, SS40), asList(OFF1, "v"), asList(OFF1, OFF2), false, null, false, null, 1, false, null, previousOrderDate, null);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(2)));
    }

    @Test
    public void shouldTransformRemovedAndUpdatedEndorsementCorrectly_WhenPointsDisqualitifactionExists() {
        List<Cases> cases = getCasesWithMultipleOffences(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, SS50), asList(OFF1, OFF3), asList(OFF1, OFF3), false, null, null, POINTS_DISQUALIFICATION_CODE, 1, false, null);
        DriverNotified previous = getPreviousDriverNotified(4, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                asList(SS30, TT99, SS40, TT99), asList(OFF1, OFF1, OFF2, OFF2), asList(OFF1, OFF1_TT99, OFF2, OFF2_TT99), false, null, false, null, 1, false, null, previousOrderDate, null);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(2)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30, TT99).stream().sorted().toArray())));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getMainOffenceCode(), is(equalTo(OFF1)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(1).getMainOffenceCode(), is(equalTo(OFF1)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getCode(), is(equalTo(OFF1)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(1).getCode(), is(equalTo(OFF1_TT99)));

        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(2)));


        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS40), asList(OFF2), asList(OFF2), false, null, null, POINTS_DISQUALIFICATION_CODE, 1, false, null);
        previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, TT99), asList(OFF1, OFF1), asList(OFF1, OFF1_TT99), false, null, false, null, 1, false, null, previousOrderDate, null);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(2)));
    }

    @Test
    public void shouldTransformRemovedAndUpdatedEndorsementCorrectly_WhenAmendAndReshareNoApplication() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRAL.id), previousOrderDate, null);
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, amendmentDate, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));


        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, getPrompt("3"), 1, false, asList(DDRAL.id), previousOrderDate, null);
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, getPrompt("5"), null, 1, false, asList(DDRAL.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, amendmentDate, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesNotMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
    }
    @Test
    void shouldNotGenerateRemoveEndorsementsOnApplicationRefused() {
        final List<Results> results = singletonList(results().withResultIdentifier(RFSD.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRAL.id), previousOrderDate, null);
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        final List<DriverNotified> driverNotified = transformDriverNotified(previousByCase, orderDate,
                crownCourt, amendmentDate, defendant, cases, hearingId, courtApplications);

       assertThat(driverNotified.size(), is(equalTo(0)));
    }

    @Test
    public void shouldTransformRemovedAndUpdatedEndorsementCorrectly_WhenNotAmendAndReshareNoApplication() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRVL.id), previousOrderDate, null);
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<DriverNotified> transformedList = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null);

        assertThat(transformedList.size(), equalTo(1));

        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRAL.id), previousOrderDate, null);
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(DDOTEL.id));

        assertOffenceAttributesBeforeTransform(cases);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));


        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRAL.id), previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(DDRI.id));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(DDOTEL.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));


        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRAL.id), previousOrderDate, null);
        courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(UUID.randomUUID().toString()));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(SV.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
    }

    @Test
    public void shouldTransformRemovedAndUpdatedEndorsementCorrectly_WhenAACA() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDRAL.id), previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AACA.id));
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(DDOTEL.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(false)));


        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AACA.id, DDRE.id));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, convictionDate, null, null, 1, false, asList(SV.id, DDDTL.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(true)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo(null)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode(), is(equalTo(null)));
    }

    @Test
    public void shouldTransformRemovedAndUpdatedEndorsementCorrectly_WhenSentenceVaried() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AACD.id));
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(SV.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(true)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), is(equalTo("2021-01-20")));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode(), is(equalTo("ConvictingCourtCode")));

        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AASA.id));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(SV.id, DDDTL.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesNotMerged(cases, previous, transformed);
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(true)));

        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(DDRE.id));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(SV.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(true)));
    }

    @Test
    public void shouldTransformRemovedAndUpdatedEndorsementCorrectly_WhenNotAACAAndSentenceNotVaried() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(DDRE.id));
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(DDPL.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(false)));


        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AACD.id));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(DDPL.id));

        assertOffenceAttributesBeforeTransform(cases);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30).stream().sorted().toArray())));
        assertOffenceAttributesMerged(cases, previous, transformed);
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getCases().get(0).getHasSV(), is(equalTo(false)));


        previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        courtApplications = getCourtApplications(asList(APP1), prefix, true, null, null, SS30, 1, asList(AACD.id));
        cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(DDPL.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<DriverNotified> transformed2 = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications);

        assertThat(transformed2.size(), is(equalTo(1)));
    }

    private void assertOffenceAttributesBeforeTransform(List<Cases> cases) {
        assertTrue(isEmpty(cases.get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
    }

    private void assertOffenceAttributesMerged(List<Cases> cases, DriverNotified previous, DriverNotified transformed) {
        assertTrue(isNotEmpty(cases.get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
        assertTrue(isNotEmpty(previous.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
        assertTrue(isNotEmpty(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
        assertEquals(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate(), previous.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate());
        assertTrue(isNotEmpty(cases.get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode()));
        assertTrue(isNotEmpty(previous.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode()));
        assertTrue(isNotEmpty(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode()));
        assertEquals(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode(), previous.getCases().get(0).getDefendantCaseOffences().get(0).getConvictingCourtCode());
    }

    private void assertOffenceAttributesNotMerged(List<Cases> cases, DriverNotified previous, DriverNotified transformed) {
        assertTrue(isEmpty(cases.get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
        assertTrue(isNotEmpty(previous.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
        assertTrue(isEmpty(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getConvictionDate()));
    }

    @Test
    public void shouldTransformWithCorrectRemovedEndorsementsListWhenEndorsementRemovedFollowingAppeal() {
        List<Cases> cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.FALSE), asList(C_DVLA1), true, convictionDate, null, null, 1, false);
        DriverNotified previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false, null, 1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));

        boolean isOffencePresent = checkForEndorsableOffences(transformed.getCases());
        assertThat(isOffencePresent, is(equalTo(false)));
    }

    @Test
    public void shouldTransformWithCorrectRemovedEndorsementsListWhenEndorsementRemovedAndUpdatedFollowingAppeal() {
        List<Cases> cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.FALSE), null, null, null, false, null, asList(Prompts.prompts().withPromptReference(FINE).withValue(VALUE1).build()), null, 1, false, asList(randomUUID().toString()));
        DriverNotified previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(prefixForPrevious.concat(VALUE1)).build(),
                        Prompts.prompts().withPromptReference(LICENCE_ISSUE_NUMBER).withValue(prefixForPrevious.concat(LICENCE_ISSUE_NUMBER)).build()),
                1, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));

        boolean isOffencePresent = checkForEndorsableOffences(transformed.getCases());
        assertThat(isOffencePresent, is(equalTo(false)));
    }

    @Test
    public void shouldTransformWithCorrectRemovedEndorsementsListWhenEndorsementRemovedFollowingAppealHasRemovedTheirConviction() {
        List<Cases> cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.FALSE), asList(C_DVLA1), false, null, null, null, 1, false);
        DriverNotified previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(DATE_OF_CONVICTION).withValue(prefixForPrevious.concat(DATE_OF_CONVICTION)).build(),
                        Prompts.prompts().withPromptReference(CONVICTING_COURT).withValue(prefixForPrevious.concat(CONVICTING_COURT)).build()),
                1, false, previousOrderDate);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getDistinctPrompts().size(), is(equalTo(2)));

        transformed.getDistinctPrompts().forEach(prompt -> {
            assertTrue(isNotEmpty(prompt.getValue()));
            if (prompt.getPromptReference().equals(CONVICTING_COURT)) {
                assertThat(prompt.getValue(), is(equalTo(prefixForPrevious.concat(CONVICTING_COURT))));
            } else if (prompt.getPromptReference().equals(DATE_OF_CONVICTION)) {
                assertThat(prompt.getValue(), is(equalTo(prefixForPrevious.concat(DATE_OF_CONVICTION))));
            }
        });
    }

    @Test
    public void shouldTransformWithCorrectSentenceDateAndCourtCodeWhenOrderDateIsDifferentToConvictionDate() {
        List<Cases> cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), true, previousConvictionDate, null, null, 1, false);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        transformed.getCases().stream()
                .forEach(aCase -> aCase.getDefendantCaseOffences().stream()
                        .forEach(aOffence -> {
                            assertThat(aOffence.getSentenceDate(), is(equalTo(orderDate)));
                        }));
    }

    @Test
    public void shouldTransformWithCorrectSentenceDateAndCourtCodeWhenOrderDateIsDifferentToConvictionDate_AndSentenceDateWasPopulatedOnPreviousNotification() {
        List<Cases> cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), false, null, null, null, 1, false);
        DriverNotified previous1 = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                1, true, previousOrderDate);

        previousByCase.put(previous1.getCases().get(0).getReference(), previous1);

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        transformed.getCases().stream()
                .forEach(aCase -> aCase.getDefendantCaseOffences().stream()
                        .forEach(aOffence -> {
                            assertThat(aOffence.getSentenceDate(), is(equalTo("SentenceDate")));
                            assertThat(aOffence.getSentencingCourtCode(), is(equalTo("SentencingCourtCode")));
                        }));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPointsDisqualificationIsImposed_DSPA() {

        List<Cases> cases2 = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("2 Years 1 Month 4 Days").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE, 1, asList(DSPA.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases2, hearingId, null).get(0);

        DefendantCaseOffences mainOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(0);
        DefendantCaseOffences pointsDisqOffence = transformed.getCases().get(0).getDefendantCaseOffences().get(1);

        assertThat(mainOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(mainOffence.getCode(), is(equalTo("OffenceCode1")));
        assertThat(mainOffence.getDvlaCode(), is(equalTo("currentcurrentDVLACode1")));
        assertThat(mainOffence.getDisqualificationPeriod(), nullValue());
        assertThat(mainOffence.getDateDisqSuspendedPendingAppeal(), nullValue());
        assertThat(mainOffence.getDttpDtetp(), nullValue());

        assertThat(pointsDisqOffence.getMainOffenceCode(), is(equalTo("OffenceCode1")));
        assertThat(pointsDisqOffence.getCode(), is(equalTo("OffenceCode1TT99")));
        assertThat(pointsDisqOffence.getDvlaCode(), is(equalTo(TT99)));
        assertThat(pointsDisqOffence.getSentencingCourtCode(), is(equalTo("0433")));
        assertThat(pointsDisqOffence.getSentenceDate(), is(equalTo(orderDate)));
        assertThat(pointsDisqOffence.getDisqualificationPeriod(), is(equalTo("020104")));
        assertThat(pointsDisqOffence.getDateDisqSuspendedPendingAppeal(), is(equalTo(orderDate)));
        assertThat(pointsDisqOffence.getDttpDtetp(), is(equalTo("1")));

        assertThat(pointsDisqOffence.getStartDate(), nullValue());
        assertThat(pointsDisqOffence.getEndDate(), nullValue());
        assertThat(pointsDisqOffence.getWording(), is(equalTo(mainOffence.getWording())));
        assertThat(pointsDisqOffence.getFine(), nullValue());
        assertThat(pointsDisqOffence.getPenaltyPoints(), nullValue());
        assertThat(pointsDisqOffence.getOtherSentence(), nullValue());
        assertThat(pointsDisqOffence.getSuspendedSentence(), nullValue());
        assertThat(pointsDisqOffence.getInterimImposedFinalSentence(), nullValue());
        assertThat(pointsDisqOffence.getDateFromWhichDisqRemoved(), nullValue());
        assertThat(pointsDisqOffence.getDateDisqReimposedFollowingAppeal(), nullValue());
        assertThat(pointsDisqOffence.getConvictingCourtCode(), is(equalTo(cases2.get(0).getDefendantCaseOffences().get(1).getConvictingCourtCode())));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20Exists_CurrentNoResults_ApplicationResultsOtherThanAACAAndDDRE() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AACD.id));
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements().size(), is(0));
        assertThat(transformed.getUpdatedEndorsements().size(), is(1));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20Exists_CurrentNoOffenceResults_ApplicationResultsDDRE() {
        DriverNotified previous = getPreviousDriverNotified(2, EMPTY, asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, TT99), asList(OFF1, OFF1), asList(OFF1, OFF1_TT99), true, previousConvictionDate, true, null, 1, false, null, previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(DDRE.id));
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(asList(SS30, TT99).stream().sorted().toArray())));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().size(), is(equalTo(2)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getDvlaCode(), is(equalTo(SS30)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getCode(), is(equalTo(OFF1)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(1).getDvlaCode(), is(equalTo(TT99)));
        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(1).getCode(), is(equalTo(OFF1_TT99)));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20Exists_CurrentNoResults_WhenAACA() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, null);
        List<CourtApplications> courtApplications = getCourtApplications(asList(APP1), prefix, false, null, null, SS30, 1, asList(AACA.id));
        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, NORMAL_FORMATTED_DATE, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getOrderDate(), is(equalTo(orderDate)));
    }

    @Test
    public void shouldNotTransformDriverNotified_WhenPreviousD20ExistsAndAdjourned_CurrentNoEndorsements() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, NORMAL_FORMATTED_DATE);

        List<Cases> cases = getCasesWithMultipleOffences(1, EMPTY, asList(Boolean.FALSE),
                asList("AD"), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, asList(ADJ.id));

        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        List<DriverNotified> transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null);

        assertThat(transformed.size(), is(0));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20ExistsAndAdjourned_CurrentAdjourned() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, NORMAL_FORMATTED_DATE);

        List<Cases> cases = getCasesWithMultipleOffences(0, EMPTY, asList(Boolean.FALSE),
                asList("AD"), asList(OFF1), asList(OFF1), false, null, null, null, 1, false, null);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        List<DriverNotified> transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null);

        assertThat(transformed, is(notNullValue()));
    }

    /**
     * 1.Enter result Adj + 1 Interim Disqualification
     * 2.Amend and Share - add Adj but remove interim disqualification
     * 3.Should generate d20 removal driver notification
    */

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20WithAdjournedInterimDisq_CurrentWithAdjOnly() {
        DriverNotified previous = getPreviousDriverNotified(1,
                                                            EMPTY,
                                                            asList(Boolean.FALSE, Boolean.TRUE),
                                                            asList(SS30),
                                                            asList(OFF1),
                                                            asList(OFF1),
                                                            false,
                                                            null,
                                                            true,
                                                            null,
                                                            1,
                                                            false,
                                                            asList("AD", DDRI.id),
                                                            previousOrderDate, NORMAL_FORMATTED_DATE);
        final Results interimRes = previous.getCases().get(0).getDefendantCaseOffences().get(0).getResults().get(1);
        ReflectionUtil.setField(interimRes, "d20", true);
        List<Cases> cases = getCasesWithMultipleOffences(1,
                                                        EMPTY,
                                                        asList(Boolean.FALSE),
                                                        asList(SS30),
                                                        asList(OFF1),
                                                        asList(OFF1),
                                                        false,
                                                        null,
                                                        null, null, 3, false, asList("AD"));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(previousByCase,
                                                             orderDate,
                                                             crownCourt,
                                                             null,
                                                             defendant,
                                                             cases, previous.getOrderingHearingId(), null).get(0);

        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements().size(), is(1));
        assertThat(transformed.getUpdatedEndorsements().size(), is(0));
    }

    /**
     * 1.Enter result with 2 D20 Interim Disqualification
     * 2.Remove all interims
     * 3.Should not generate d20 removal driver notification
     **/

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20InterimDisqs_CurrentWithAllRemoved() {
        DriverNotified previous = getPreviousDriverNotified(1,
                                                            EMPTY,
                                                            asList(Boolean.TRUE, Boolean.TRUE),
                                                            asList(SS30),
                                                            asList(OFF1),
                                                            asList(OFF1),
                                                            false,
                                                            null,
                                                            true,
                                                            null,
                                                            1,
                                                            false,
                                                            asList(DDRNL.id, DDRI.id),
                                                            previousOrderDate, NORMAL_FORMATTED_DATE);
        List<Cases> cases = getCasesWithMultipleOffences(1,
                                                        EMPTY,
                                                        asList(Boolean.FALSE),
                                                        asList(SS30),
                                                        asList(OFF1),
                                                        asList(OFF1),
                                                        false,
                                                        null,
                                                        null,
                                                        null, 1, false, singletonList(ADJ.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<DriverNotified> transformed = transformDriverNotified(previousByCase,
                                                             orderDate,
                                                             crownCourt,
                                                             null,
                                                             defendant,
                                                             cases, hearingId, null);

        assertThat(transformed.size(), is(0));
    }

    /**
     * 1.Enter 2 offences with results 2 D20 Interim Disqualification
     * 2.Remove one endorsed interim from each offence
     * 3.Should not generate d20 removal driver notification for each offence type
     **/

    @Test
    public void shouldTransformDriverNotified_WhenPreviousMultipleOffencesAndD20_CurrentWithOneRemoved() {
        DriverNotified previous = getPreviousDriverNotified(2,
                EMPTY,
                asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, SS40),
                asList(OFF1, OFF2),
                asList(OFF1, OFF2),
                false,
                null,
                true,
                null,
                1,
                false,
                asList(DDRNL.id, DDRI.id),
                previousOrderDate, NORMAL_FORMATTED_DATE);
        List<Cases> cases = getCasesWithMultipleOffences(2,
                EMPTY,
                asList(Boolean.FALSE, Boolean.FALSE),
                asList(SS30, SS40),
                asList(OFF1, OFF2),
                asList(OFF1, OFF2),
                false,
                null,
                null,
                null, 3, false, asList(DDRI.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<DriverNotified> transformed = transformDriverNotified(previousByCase,
                orderDate,
                crownCourt,
                null,
                defendant,
                cases, hearingId, null);

        assertThat(transformed.size(), is(1));
        assertThat(transformed.get(0).getRemovedEndorsements().size(), is(2));
        assertThat(transformed.get(0).getRemovedEndorsements().get(0), is(SS30));
        assertThat(transformed.get(0).getRemovedEndorsements().get(1), is(SS40));
    }

    /**
     * 1.Enter 2 offences with results 2 D20 Interim Disqualification
     * 2.Remove one endorsed interim from the offence for case reference 1
     * 3.Should generate d20 removal driver notification from the offence for case ref 1
     **/

    @Test
    public void shouldTransformDriverNotified_WhenPreviousMultipleCasesAndD20_CurrentWithOneRemoved() {
        DriverNotified previous = getPreviousDriverNotified(2,
                EMPTY,
                asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, SS40),
                asList("OffenceCode1", "OffenceCode2"),
                asList("OffenceCode1", "OffenceCode2"),
                false,
                null,
                true,
                null,
                1,
                false,
                asList(DDRNL.id, DDRI.id),
                previousOrderDate, NORMAL_FORMATTED_DATE);
        final Cases prevCase = previous.getCases().get(0);
        ReflectionUtil.setField(prevCase, "reference", "CaseReference1");
        List<Cases> cases = getMultipleCases(3,
                prefix,
                asList(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE),
                false,
                null,
                null,
                null, 1, asList(DDRNL.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase,
                orderDate,
                crownCourt,
                null,
                defendant,
                cases, hearingId, null).get(0);

        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements().size(), is(1));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(SS30)));
        assertThat(transformed.getUpdatedEndorsements().size(), is(0));
    }

    /**
     * 1.Enter result with 2 None D20 Interim Disqualification
     * 2.Remove one of the interims
     * 3.Should NOT generate d20 removal as previous was not D20
    **/
    @Test
    public void shouldNotTransformDriverNotifiedRemoved_WhenPreviousNoneD20InterimDisqs_CurrentWithOneRemoved() {
        DriverNotified previous = getPreviousDriverNotified(1,
                                                            EMPTY,
                                                            asList(Boolean.FALSE, Boolean.FALSE),
                                                            asList(SS30),
                                                            asList(OFF1),
                                                            asList(OFF1),
                                                            false,
                                                            null,
                                                            true,
                                                            null,
                                                            1,
                                                            false,
                                                            asList(DDRNL.id, DDRI.id),
                                                            previousOrderDate, NORMAL_FORMATTED_DATE);
        List<Cases> cases = getCasesWithMultipleOffences(1,
                                                        EMPTY,
                                                        asList(Boolean.FALSE),
                                                        asList(SS30),
                                                        asList(OFF1),
                                                        asList(OFF1),
                                                        false,
                                                        null,
                                                        null,
                                                        null, 1, false, asList(DDRNL.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        List<DriverNotified> transformed = transformDriverNotified(previousByCase,
                                                             orderDate,
                                                             crownCourt,
                                                             null,
                                                             defendant,
                                                             cases, hearingId, null);

        assertThat(transformed.size(), is(0));
    }

    @Test
    public void shouldNotTransformDriverNotifiedRemoved_WhenNoPreviousNotificationOrPreviousOffence() {
        List<Cases> cases = getCasesWithMultipleOffences(1,
                                                        EMPTY,
                                                        asList(Boolean.TRUE),
                                                        asList(SS30),
                                                        asList(OFF1),
                                                        asList(OFF1),
                                                        false,
                                                        null,
                                                        null,
                                                        null, 1, false, asList(DDRNL.id));

        DriverNotified transformed = transformDriverNotified(previousByCase,
                                                             orderDate,
                                                             crownCourt,
                                                             null,
                                                             defendant,
                                                             cases, hearingId, null).get(0);

        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements(), is(nullValue()));
        assertThat(transformed.getUpdatedEndorsements(), is(nullValue()));

        final DriverNotified previous = getPreviousDriverNotified(0,
                EMPTY,
                asList(Boolean.FALSE, Boolean.FALSE),
                asList(SS30),
                asList(OFF1),
                asList(OFF1),
                false,
                null,
                true,
                null,
                3,
                false,
                asList(DDRNL.id, DDRI.id),
                previousOrderDate, NORMAL_FORMATTED_DATE);

        cases = getCasesWithMultipleOffences(1,
                                            EMPTY,
                                            asList(Boolean.TRUE),
                                            asList(SS30),
                                            asList(OFF1),
                                            asList(OFF1),
                                            false,
                                            null,
                                            null,
                                            null, 1, false, asList(DDRNL.id));

        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        transformed = transformDriverNotified(previousByCase,
                                             orderDate,
                                             crownCourt,
                                             null,
                                             defendant,
                                             cases, hearingId, null).get(0);

        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements(), is(nullValue()));
        assertThat(transformed.getUpdatedEndorsements(), is(nullValue()));
    }

    @Test
    public void shouldTransformDriverNotified_WhenPreviousD20Exists_MultipleCases() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
                asList(SS30), asList(OFF1), asList(OFF1), true, previousConvictionDate, true, null, 1, false, asList(DDPL.id), previousOrderDate, NORMAL_FORMATTED_DATE);

        List<Cases> cases = getMultipleCases(3, prefix, asList(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE), false, null, null, null, 3, null);

        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<DriverNotified> transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null);

        assertThat(transformed.size(), equalTo(1));

        assertThat(transformed.get(0).getCases().get(0).getReference(), is(equalTo("CaseReference2")));
        assertThat(transformed.get(0).getCases().get(0).getDefendantCaseOffences().size(), is(equalTo(1)));
    }

    @Test
    public void shouldTransformAlcoholReadingAmountCorrectly() {
        List<Cases> cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), false, null, null, null, 1, false);
        DriverNotified previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                1, true, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getAlcoholReadingAmount(), equalTo(null));


        cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), true, convictionDate, null, null, 2, false);
        previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                1, true, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getAlcoholReadingAmount(), equalTo("002"));


        cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), false, null, null, null, 22, false);
        previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                1, true, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getAlcoholReadingAmount(), equalTo("022"));


        cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), true, convictionDate, null, null, 222, false);
        previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), true, previousConvictionDate, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                333, true, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);
        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getAlcoholReadingAmount(), equalTo("222"));


        cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), false, null, null, null, 1075, true);
        previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                333, false, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getAlcoholReadingAmount(), equalTo("999"));


        cases = getCasesWithMultipleOffences(1, prefix, asList(Boolean.TRUE), asList(C_DVLA1), false, null, null, null, 1, false);
        previous = getPreviousDriverNotified(1, prefixForPrevious, asList(Boolean.TRUE), asList(P_DVLA1), false, null, false,
                asList(Prompts.prompts().withPromptReference(STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION).withValue("2").build()),
                333, true, previousOrderDate);
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, null).get(0);

        assertThat(transformed.getCases().get(0).getDefendantCaseOffences().get(0).getAlcoholReadingAmount(), equalTo("333"));
    }

    @Test
    public void shouldTransformDriverNotifiedForNonEndorsableOffenceWithD20Results() {

        List<Cases> cases = getCases(prefix, true, previousConvictionDate,
                new LinkedList(asList(Prompts.prompts().withPromptReference(PENALTY_POINTS).withValue(VALUE1).build(),
                        Prompts.prompts().withPromptReference(DISQUALIFICATION_PERIOD).withValue("2 Years 1 Month 4 Days").build(),
                        Prompts.prompts().withPromptReference(NOTIONAL_PENALTY_POINTS).withValue(VALUE3).build())),
                POINTS_DISQUALIFICATION_CODE, 0, asList(DSPA.id));

        List<DriverNotified> driverNotifiedList = transformDriverNotified(
                previousByCase, orderDate, crownCourt, amendmentDate, defendant,
                cases, hearingId, null);

        assertThat(driverNotifiedList.size(), equalTo(1));
    }

    @Test
    public void shouldNotTransformDriverNotifiedForNonEndorsableOffenceWithNoD20Results() {

        List<Cases> cases = getCases(prefix, true, null, null);

        cases.get(0).getDefendantCaseOffences().get(0).getResults().clear();

        List<DriverNotified> driverNotifiedList = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases, hearingId, null);

        assertThat(driverNotifiedList.size(), equalTo(0));


        cases = getCases(prefix, true, null, null);
        cases.get(0).getDefendantCaseOffences().get(0).getResults().clear();
        cases.get(0).getDefendantCaseOffences().get(0).getResults().add(Results.results()
                        .withResultIdentifier("result")
                .withD20(false)
                .build());

        driverNotifiedList = transformDriverNotified(
                previousByCase, orderDate, crownCourt, null, defendant,
                cases, hearingId, null);

        assertThat(driverNotifiedList.size(), equalTo(0));
    }

    /**
     * Scenario - There are two offences, both the offences having same offence code. One of the offences D20 removed and the other one updated.
     * 1.Previous Case has two offences with D20 having same offence code
     * 2.Current case, one of the offence sentence varied and the D20 false, the second one updated with 3 Months disqualification
     * 3.The generated notification should have one removed endorsement and one updated endorsement.
     **/
    @Test
    public void shouldTransformDriverNotified_WhenPreviousMultipleCasesAndD20_OneOfThemD20RemovedAndOneOfThemUpdated() {
        DriverNotified previous = getPreviousDriverNotified(2,
                EMPTY,
                asList(Boolean.TRUE, Boolean.TRUE),
                asList(SS30, SS30),
                asList(OFF1,OFF1),
                asList(OFF1,OFF1),
                false,
                null,
                true,
                null,
                1,
                false,
                asList(DDRNL.id, DDRI.id),
                previousOrderDate, NORMAL_FORMATTED_DATE);
        final Cases prevCase = previous.getCases().get(0);
        ReflectionUtil.setField(prevCase, "reference", "CaseReference");
        final List<Cases> cases = getCasesWithMultipleOffences(2,
                EMPTY,
                asList(Boolean.FALSE,Boolean.TRUE),
                asList(SS30,SS30),
                asList(OFF1,OFF1),
                asList(OFF1,OFF1),
                false,
                null,
                null,
                "3 Months", 1, false, asList(SV.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase,
                orderDate,
                crownCourt,
                null,
                defendant,
                cases, hearingId, null).get(0);

        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements().size(), is(1));
        assertThat(transformed.getRemovedEndorsements().get(0), is(equalTo(SS30)));
        assertThat(transformed.getUpdatedEndorsements().size(), is(1));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(SS30)));
    }

    /**
     * Scenario - There are three offences, all of them having same offence code. Two of the offences D20 removed and the other one updated.
     * 1.Previous Case 3 offences with D20 having same offence code
     * 2.Current case, two offence's sentence varied and their D20 false, the third one updated with 3 Months disqualification
     * 3.The generated notification should have two removed endorsement and one updated endorsement.
     **/
    @Test
    public void shouldTransformDriverNotified_WhenPreviousMultipleCasesAndD20_TwoOfThemD20RemovedAndOneOfThemUpdated() {
        DriverNotified previous = getPreviousDriverNotified(3,
                EMPTY,
                asList(Boolean.TRUE, Boolean.TRUE,Boolean.TRUE),
                asList(SS30, SS30,SS30),
                asList(OFF1,OFF1,OFF1),
                asList(OFF1,OFF1,OFF1),
                false,
                null,
                true,
                null,
                1,
                false,
                asList(DDRNL.id, DDRI.id),
                previousOrderDate, NORMAL_FORMATTED_DATE);
        final Cases prevCase = previous.getCases().get(0);
        ReflectionUtil.setField(prevCase, "reference", "CaseReference");
        final List<Cases> cases = getCasesWithMultipleOffences(3,
                EMPTY,
                asList(Boolean.FALSE,Boolean.FALSE,Boolean.TRUE),
                asList(SS30,SS30,SS30),
                asList(OFF1,OFF1,OFF1),
                asList(OFF1,OFF1,OFF1),
                false,
                null,
                null,
                "3 Months", 1, false, asList(SV.id));
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        DriverNotified transformed = transformDriverNotified(previousByCase,
                orderDate,
                crownCourt,
                null,
                defendant,
                cases, hearingId, null).get(0);

        // Make sure that we have two removed endorsement with dvla code SS30
        assertThat(transformed, is(notNullValue()));
        assertThat(transformed.getRemovedEndorsements().size(), is(2));
        transformed.getRemovedEndorsements().forEach(dvlaCode->assertThat(dvlaCode,equalTo(SS30)));

        assertThat(transformed.getUpdatedEndorsements().size(), is(1));
        assertThat(transformed.getUpdatedEndorsements().get(0), is(equalTo(SS30)));
    }

    @Test
    public void shouldReturnRemoveWhenAACA() {
            DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                    singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
            List<Cases> cases = asList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
            previousByCase.put(previous.getCases().get(0).getReference(), previous);

            List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(AACA.id));

            DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                    crownCourt, amendmentDate, defendant, cases, hearingId, courtApplications).get(0);

            assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
            assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
            assertThat(transformed.getRemovedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenAASA() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(AASA.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, amendmentDate, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenAACD() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(AACD.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenAASD() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(AASD.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenACSD() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(ACSD.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenAPA() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(APA.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenASV() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(ASV.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenAW() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(AW.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }

    @Test
    public void shouldReturnRemoveWhenDDRE() {
        DriverNotified previous = getPreviousDriverNotified(1, EMPTY, singletonList(Boolean.TRUE),
                singletonList(SS30), singletonList(OFF1), singletonList(OFF1), true, previousConvictionDate, true, null, 1, false, singletonList(DSPAS.id), previousOrderDate, null);
        List<Cases> cases = singletonList(Cases.cases().withDefendantCaseOffences(new ArrayList<>()).withCaseId(CASE_ID).withReference("CaseReference").build());
        previousByCase.put(previous.getCases().get(0).getReference(), previous);

        List<CourtApplications> courtApplications = getCourtApplications(singletonList(APP1), prefix, false, null, null, SS30, 1, singletonList(DDRE.id));

        DriverNotified transformed = transformDriverNotified(previousByCase, orderDate,
                crownCourt, null, defendant, cases, hearingId, courtApplications).get(0);

        assertThat(transformed.getUpdatedEndorsements().size(), is(equalTo(1)));
        assertThat(transformed.getRemovedEndorsements().size(), is(equalTo(0)));
        assertThat(transformed.getUpdatedEndorsements().stream().sorted().toArray(), is(equalTo(Stream.of(SS30).toArray())));
    }
}