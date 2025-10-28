package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.cpp.stagingdvla.event.Prompts.prompts;
import static uk.gov.justice.cpp.stagingdvla.event.Results.results;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.ACP;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.REMOVE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.UPDATE_MERGE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AACA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AACD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AASA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AASD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ACSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.APA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ASV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AW;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.COV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DSPAS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ERR;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.G;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.RFSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyResultOrPromptModified;

import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;
import uk.gov.justice.cpp.stagingdvla.event.Results;

import java.util.List;

import org.junit.jupiter.api.Test;

class OffenceUtilTest {

    public static final String RESULT_IDENTIFIER = "RI01";
    public static final String PROMPT_REFERENCE = "PENT";
    public static final String caseReference = "CASE REF01";
    public static final String MAIN_OFFENCE_CODE = "CODE1";

    @Test
    void shouldRemoveD20DuringAmendAndReShare() {
        Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        Cases currentCases = buildCases(RESULT_IDENTIFIER, Boolean.FALSE);

        boolean d20Removed = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, null);

        assertThat(d20Removed, is(false));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseOnApplicationRefused() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(RFSD.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean isApplicationRefused = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(isApplicationRefused, is(false));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseOnERR() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(ERR.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean isApplicationRefused = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(isApplicationRefused, is(false));
    }

    @Test
    void shouldHasAnyD20RemovedReturnTrueOnApplicationRefusedResults() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withDefendantCaseOffences(singletonList(
                DefendantCaseOffences.defendantCaseOffences()
                        .withMainOffenceCode(MAIN_OFFENCE_CODE)
                        .withResults(singletonList(
                                results()
                                        .withD20(true)
                                        .withResultIdentifier(AACA.id)
                                        .build()))
                        .build())).withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(AACA.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean isApplicationRefused = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(isApplicationRefused, is(true));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseOnCovGranted() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = asList(results().withResultIdentifier(G.id).build(), results().withResultIdentifier(COV.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean hasAnyD20Removed = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(false));
    }

    @Test
    void shouldHasAnyD20RemovedReturnTrueOnNonCovGranted() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withDefendantCaseOffences(singletonList(
                DefendantCaseOffences.defendantCaseOffences()
                        .withMainOffenceCode(MAIN_OFFENCE_CODE)
                        .withResults(singletonList(
                                results()
                                        .withD20(true)
                                        .withResultIdentifier(AACA.id)
                                        .build()))
                        .build())).withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(G.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean hasAnyD20Removed = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(true));
    }

    @Test
    void shouldIgnoreD20SearchWhenCaseInactiveAndApplicationTypeIsACP() {
        final Cases currentCases = Cases.cases().withReference(caseReference).withCaseStatus("INACTIVE").build();
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withApplicationTypeId(ACP.id).build());

        boolean hasAnyD20Removed = OffenceUtil.hasAnyD20Removed(null, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(false));
    }

    @Test
    void shouldRemoveD20ForStDec() {
        final List<Results> results = singletonList(results().withResultIdentifier(G.id).build());
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).withCaseStatus("ACTIVE").build();
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).withApplicationType("Appearance to make statutory declaration (other than SJP)").build());

        boolean hasAnyD20Removed = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(true));
    }

    @Test
    void shouldReturnFalse_whenEndorsedResultPromptValueNotChangedDuringAmendAndReShare() {
        Cases previousCases = buildCasesWithPrompt("2");
        Cases currentCases = buildCasesWithPrompt("2");

        boolean hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCases, currentCases);

        assertThat(hasResultOrPromptModified, is(false));

    }

    @Test
    void shouldReturnTrue_whenEndorsedResultPromptValueChangedDuringAmendAndReShare() {

        Cases previousCases = buildCasesWithPrompt("2");
        Cases currentCases = buildCasesWithPrompt("3");

        boolean hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCases, currentCases);

        assertThat(hasResultOrPromptModified, is(true));
    }

    @Test
    void shouldReturnFalse_whenEndorsedResultWithMultiplePromptValueNotChangedDuringAmendAndReShare() {

        List<Prompts> prevPrompts = asList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("2").build(),
                prompts().withPromptReference("LicenceNumber").withValue("ABIDE0,0010BCF").build());

        List<Prompts> currPrompt = asList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("2").build(),
                prompts().withPromptReference("LicenceNumber").withValue("ABIDE0,0010BCF").build());

        Cases previousCases = buildCasesWithPrompt(RESULT_IDENTIFIER, prevPrompts);
        Cases currentCases = buildCasesWithPrompt(RESULT_IDENTIFIER, currPrompt);

        boolean hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCases, currentCases);

        assertThat(hasResultOrPromptModified, is(false));
    }

    @Test
    void shouldReturnTrue_whenEndorsedResultWithMultiplePromptValueChangedDuringAmendAndReShare() {
        List<Prompts> prevPrompts = asList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("2").build(),
                prompts().withPromptReference("LicenceNumber").withValue("ABIDE0,0010BCF").build());

        List<Prompts> currPrompt = asList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("3").build(),
                prompts().withPromptReference("LicenceNumber").withValue("XXX XXX,0010BCF").build());

        Cases previousCases = buildCasesWithPrompt(RESULT_IDENTIFIER, prevPrompts);
        Cases currentCases = buildCasesWithPrompt(RESULT_IDENTIFIER, currPrompt);

        boolean hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCases, currentCases);

        assertThat(hasResultOrPromptModified, is(true));
    }

    @Test
    void shouldReturnTrue_whenEndorsedResultRemovedDuringAmendAndReShare() {

        List<Prompts> prevPrompts = asList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("2").build(),
                prompts().withPromptReference("LicenceNumber").withValue("ABIDE0,0010BCF").build());

        List<Prompts> currPrompt = singletonList(prompts().withPromptReference("FINE").withValue("100").build());

        Cases previousCases = buildCasesWithPrompt(RESULT_IDENTIFIER, prevPrompts);
        Cases currentCases = buildCasesWithPrompt("RI02", currPrompt);

        boolean hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCases, currentCases);

        assertThat(hasResultOrPromptModified, is(true));
    }

    @Test
    void shouldRemoveD20DuringNextHearing() {
        Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);

        Cases currentCases = buildCases("RI02", Boolean.FALSE);

        boolean d20Removed = OffenceUtil.hasAnyD20Removed(previousCases, currentCases, null);

        assertThat(d20Removed, is(true));
    }

    @Test
    void shouldGetEndorsementStatusDSPAS() {
        final List<CourtApplications> courtApplications = getCourtApplications(DSPAS);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusAACA() {
        final List<CourtApplications> courtApplications = getCourtApplications(AACA);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusAASA() {
        final List<CourtApplications> courtApplications = getCourtApplications(AASA);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusAACD() {
        final List<CourtApplications> courtApplications = getCourtApplications(AACD);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusAASD() {
        final List<CourtApplications> courtApplications = getCourtApplications(AASD);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusACSD() {
        final List<CourtApplications> courtApplications = getCourtApplications(ACSD);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusAPA() {
        final List<CourtApplications> courtApplications = getCourtApplications(APA);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusASV() {
        final List<CourtApplications> courtApplications = getCourtApplications(ASV);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusAW() {
        final List<CourtApplications> courtApplications = getCourtApplications(AW);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusDDRE() {
        final List<CourtApplications> courtApplications = getCourtApplications(DDRE);

        final AggregateConstants.EndorsementStatus endorsementStatus = OffenceUtil.getEndorsementStatus(false, null, courtApplications);

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    private Cases buildCases(String resultIdentifier, boolean d20) {
        return Cases.cases()
                .withReference(caseReference)
                .withDefendantCaseOffences(singletonList(
                        DefendantCaseOffences.defendantCaseOffences()
                                .withMainOffenceCode(MAIN_OFFENCE_CODE)
                                .withResults(singletonList(
                                        results()
                                                .withD20(d20)
                                                .withResultIdentifier(resultIdentifier)
                                                .build()))
                                .build()))
                .build();
    }

    private Cases buildCasesWithPrompt(String promptValue) {
        return Cases.cases()
                .withReference(caseReference)
                .withDefendantCaseOffences(singletonList(
                        DefendantCaseOffences.defendantCaseOffences()
                                .withMainOffenceCode(MAIN_OFFENCE_CODE)
                                .withCode(MAIN_OFFENCE_CODE)
                                .withResults(singletonList(
                                        results()
                                                .withD20(Boolean.TRUE)
                                                .withResultIdentifier(RESULT_IDENTIFIER)
                                                .withPrompts(singletonList(prompts().withPromptReference(PROMPT_REFERENCE).withValue(promptValue).build()))
                                                .build()))
                                .build()))
                .build();
    }


    private Cases buildCasesWithPrompt(String resultIdentifier, List<Prompts> prompts) {
        return Cases.cases()
                .withReference(caseReference)
                .withDefendantCaseOffences(singletonList(
                        DefendantCaseOffences.defendantCaseOffences()
                                .withMainOffenceCode(MAIN_OFFENCE_CODE)
                                .withCode(MAIN_OFFENCE_CODE)
                                .withResults(singletonList(
                                        results()
                                                .withD20(Boolean.TRUE)
                                                .withResultIdentifier(resultIdentifier)
                                                .withPrompts(prompts)
                                                .build()))
                                .build()))
                .build();
    }

    private List<CourtApplications> getCourtApplications (final AggregateConstants.ResultType resultType) {
        final CourtApplications applications = CourtApplications.courtApplications().withResults(singletonList(results()
                .withD20(Boolean.TRUE)
                .withResultIdentifier(resultType.id)
                .withPrompts(singletonList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("2").build()))
                .build())).build();

        return singletonList(applications);
    }
}
