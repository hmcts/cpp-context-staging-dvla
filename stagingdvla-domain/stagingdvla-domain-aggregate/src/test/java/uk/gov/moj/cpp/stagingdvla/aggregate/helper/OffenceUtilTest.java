package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.cpp.stagingdvla.event.Prompts.prompts;
import static uk.gov.justice.cpp.stagingdvla.event.Results.results;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.AACMC;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.ACP;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.APPRO;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.REMOVE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.UPDATE_MERGE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AACA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AACD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AASD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ADJ;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.APA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.AW;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.COV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DINE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DINI;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DISC;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DISCH;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DISM;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DSPAS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ERR;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.G;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.OATS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.RFSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SUMRCC;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.TEXT;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.WDRN;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.WDRNOFF;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getEndorsementStatus;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyD20Removed;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyResult;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyResultOrPromptModified;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyResultType;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealRefusedResult;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealResult;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealResultOrGranted;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasD20Endorsement;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasPointsDisqualificationCode;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasResultType;

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

        boolean d20Removed = hasAnyD20Removed(previousCases, currentCases, null);

        assertThat(d20Removed, is(false));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseOnApplicationRefused() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(RFSD.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean isApplicationRefused = hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(isApplicationRefused, is(false));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseOnERR() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(ERR.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean isApplicationRefused = hasAnyD20Removed(previousCases, currentCases, courtApplications);

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

        boolean isApplicationRefused = hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(isApplicationRefused, is(true));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseOnCovGranted() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = asList(results().withResultIdentifier(G.id).build(), results().withResultIdentifier(COV.id).build());
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).build());

        boolean hasAnyD20Removed = hasAnyD20Removed(previousCases, currentCases, courtApplications);

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

        boolean hasAnyD20Removed = hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(true));
    }

    @Test
    void shouldIgnoreD20SearchWhenCaseInactiveAndApplicationTypeIsACP() {
        final Cases currentCases = Cases.cases().withReference(caseReference).withCaseStatus("INACTIVE").build();
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withApplicationTypeId(ACP.id).build());

        boolean hasAnyD20Removed = hasAnyD20Removed(null, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(false));
    }

    @Test
    void shouldRemoveD20ForStDec() {
        final List<Results> results = singletonList(results().withResultIdentifier(G.id).build());
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).withCaseStatus("ACTIVE").build();
        final List<CourtApplications> courtApplications = singletonList(CourtApplications.courtApplications().withResults(results).withApplicationType("Appearance to make statutory declaration (other than SJP)").build());

        boolean hasAnyD20Removed = hasAnyD20Removed(previousCases, currentCases, courtApplications);

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

        boolean d20Removed = hasAnyD20Removed(previousCases, currentCases, null);

        assertThat(d20Removed, is(true));
    }

    @Test
    void shouldGetEndorsementStatusDSPAS() {
        final List<CourtApplications> courtApplications = getCourtApplications(DSPAS);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(UPDATE_MERGE));
    }

    @Test
    void shouldGetEndorsementStatusDISM() {
        final List<CourtApplications> courtApplications = getCourtApplications(DISM);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusDINE() {
        final List<CourtApplications> courtApplications = getCourtApplications(DINE);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusDINI() {
        final List<CourtApplications> courtApplications = getCourtApplications(DINI);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusDISC() {
        final List<CourtApplications> courtApplications = getCourtApplications(DISC);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusDISCH() {
        final List<CourtApplications> courtApplications = getCourtApplications(DISCH);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusWDRN() {
        final List<CourtApplications> courtApplications = getCourtApplications(WDRN);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
    }

    @Test
    void shouldGetEndorsementStatusWDRNOFF() {
        final List<CourtApplications> courtApplications = getCourtApplications(WDRNOFF);

        final AggregateConstants.EndorsementStatus endorsementStatus = getEndorsementStatus(false, null, null, courtApplications, emptyList());

        assertThat(endorsementStatus, is(REMOVE));
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

    private List<CourtApplications> getCourtApplications(final AggregateConstants.ResultType resultType) {
        final CourtApplications applications = CourtApplications.courtApplications().withResults(singletonList(results()
                .withD20(Boolean.TRUE)
                .withResultIdentifier(resultType.id)
                .withPrompts(singletonList(prompts().withPromptReference(PROMPT_REFERENCE).withValue("2").build()))
                .build())).build();

        return singletonList(applications);
    }

    @Test
    void shouldReturnTrueForCaseReopenWithAPPROApplicationTypeId() {
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withApplicationTypeId(APPRO.id)
                        .build()
        );

        assertThat(courtApplications.get(0).getApplicationTypeId(), is(APPRO.id));
    }

    @Test
    void shouldReturnTrueForCaseReopenWithAPPROApplicationType() {
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withApplicationType(APPRO.appType)
                        .build()
        );

        assertThat(courtApplications.get(0).getApplicationType(), is("Application to reopen case"));
    }

    @Test
    void shouldHandleCaseReopenWithGrantedResult() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference).build();

        final List<Results> results = singletonList(results().withResultIdentifier(G.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withApplicationTypeId(APPRO.id)
                        .withResults(results)
                        .build()
        );

        boolean hasAnyD20Removed = hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(true));
    }

    @Test
    void shouldIgnoreD20SearchWhenCaseInactiveAndApplicationTypeIsACPWithId() {
        final Cases currentCases = Cases.cases()
                .withReference(caseReference)
                .withCaseStatus("INACTIVE")
                .build();
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withApplicationTypeId(ACP.id)
                        .build()
        );

        boolean hasAnyD20Removed = hasAnyD20Removed(null, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(false));
    }

    @Test
    void shouldIgnoreD20SearchWhenCaseInactiveAndApplicationTypeIsACPWithAppType() {
        final Cases currentCases = Cases.cases()
                .withReference(caseReference)
                .withCaseStatus("INACTIVE")
                .build();
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withApplicationType(ACP.appType)
                        .build()
        );

        boolean hasAnyD20Removed = hasAnyD20Removed(null, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(false));
    }

    @Test
    void shouldProcessD20WhenCaseActiveAndApplicationTypeIsACP() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases()
                .withReference(caseReference)
                .withCaseStatus("ACTIVE")
                .withDefendantCaseOffences(singletonList(
                        DefendantCaseOffences.defendantCaseOffences()
                                .withMainOffenceCode(MAIN_OFFENCE_CODE)
                                .withResults(singletonList(
                                        results()
                                                .withD20(true)
                                                .withResultIdentifier(AACA.id)
                                                .build()))
                                .build()))
                .build();

        final List<Results> results = singletonList(results().withResultIdentifier(AACA.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withApplicationTypeId(ACP.id)
                        .withResults(results)
                        .build()
        );

        boolean hasAnyD20Removed = hasAnyD20Removed(previousCases, currentCases, courtApplications);

        assertThat(hasAnyD20Removed, is(true));
    }

    @Test
    void shouldHasAnyD20RemovedReturnFalseWhenSjpCaseReferToCC() {
        final Cases previousCases = buildCases(RESULT_IDENTIFIER, Boolean.TRUE);
        final Cases currentCases = Cases.cases().withReference(caseReference)
                .withDefendantCaseOffences(singletonList(DefendantCaseOffences.defendantCaseOffences()
                        .withResults(singletonList(results()
                                .withResultIdentifier(SUMRCC.id)
                                .build()))
                        .build()))
                .build();

        boolean hasAnyD20Removed = hasAnyD20Removed(previousCases, currentCases, emptyList());

        assertThat(hasAnyD20Removed, is(false));
    }

    @Test
    void shouldReturnTrueForHasAppealResultWithAACA() {
        final List<Results> results = singletonList(results().withResultIdentifier(AACA.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealResult = hasAppealResult(courtApplications);

        assertThat(hasAppealResult, is(true));
    }

    @Test
    void shouldReturnTrueForHasAppealResultWithDDRE() {
        final List<Results> results = singletonList(results().withResultIdentifier(DDRE.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealResult = hasAppealResult(courtApplications);

        assertThat(hasAppealResult, is(true));
    }

    @Test
    void shouldReturnFalseForHasAppealResultWithNonAppealResult() {
        final List<Results> results = singletonList(results().withResultIdentifier(OATS.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealResult = hasAppealResult(courtApplications);

        assertThat(hasAppealResult, is(false));
    }

    @Test
    void shouldReturnTrueForHasAppealRefusedResultWithAACD() {
        final List<Results> results = singletonList(results().withResultIdentifier(AACD.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealRefusedResult = hasAppealRefusedResult(courtApplications);

        assertThat(hasAppealRefusedResult, is(true));
    }

    @Test
    void shouldReturnTrueForHasAppealRefusedResultWithAASD() {
        final List<Results> results = singletonList(results().withResultIdentifier(AASD.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealRefusedResult = hasAppealRefusedResult(courtApplications);

        assertThat(hasAppealRefusedResult, is(true));
    }

    @Test
    void shouldReturnTrueForHasAppealRefusedResultWithAPA() {
        final List<Results> results = singletonList(results().withResultIdentifier(APA.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealRefusedResult = hasAppealRefusedResult(courtApplications);

        assertThat(hasAppealRefusedResult, is(true));
    }

    @Test
    void shouldReturnTrueForHasAppealRefusedResultWithAW() {
        final List<Results> results = singletonList(results().withResultIdentifier(AW.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealRefusedResult = hasAppealRefusedResult(courtApplications);

        assertThat(hasAppealRefusedResult, is(true));
    }

    @Test
    void shouldReturnFalseForHasAppealRefusedResultWithNonRefusedResult() {
        final List<Results> results = singletonList(results().withResultIdentifier(AACA.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealRefusedResult = hasAppealRefusedResult(courtApplications);

        assertThat(hasAppealRefusedResult, is(false));
    }

    @Test
    void shouldReturnTrueForHasAppealResultOrGrantedWithAppealResult() {
        final List<Results> results = singletonList(results().withResultIdentifier(AACA.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealResultOrGranted = hasAppealResultOrGranted(courtApplications);

        assertThat(hasAppealResultOrGranted, is(true));
    }

    @Test
    void shouldReturnTrueForHasAppealResultOrGrantedWithGrantedResult() {
        final List<Results> results = singletonList(results().withResultIdentifier(G.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withApplicationCode(AACMC.code).withResults(results).build()
        );

        boolean hasAppealResultOrGranted = hasAppealResultOrGranted(courtApplications);

        assertThat(hasAppealResultOrGranted, is(true));
    }

    @Test
    void shouldReturnFalseForHasAppealResultOrGrantedWithOtherResult() {
        final List<Results> results = singletonList(results().withResultIdentifier(OATS.id).build());
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications().withResults(results).build()
        );

        boolean hasAppealResultOrGranted = hasAppealResultOrGranted(courtApplications);

        assertThat(hasAppealResultOrGranted, is(false));
    }

    @Test
    void shouldReturnTrueWhenOffenceHasSpecificResult() {
        final DefendantCaseOffences offence = DefendantCaseOffences.defendantCaseOffences()
                .withResults(singletonList(results().withResultIdentifier(SV.id).build()))
                .build();

        boolean hasResult = hasResultType(offence, SV);

        assertThat(hasResult, is(true));
    }

    @Test
    void shouldReturnFalseWhenOffenceDoesNotHaveSpecificResult() {
        final DefendantCaseOffences offence = DefendantCaseOffences.defendantCaseOffences()
                .withResults(singletonList(results().withResultIdentifier(OATS.id).build()))
                .build();

        boolean hasResult = hasResultType(offence, SV);

        assertThat(hasResult, is(false));
    }

    @Test
    void shouldReturnTrueWhenOffenceHasAnyOfTheSpecifiedResults() {
        final DefendantCaseOffences offence = DefendantCaseOffences.defendantCaseOffences()
                .withResults(singletonList(results().withResultIdentifier(DISM.id).build()))
                .build();

        boolean hasAnyResult = hasAnyResult(offence, asList(DISM, DINE, WDRN));

        assertThat(hasAnyResult, is(true));
    }

    @Test
    void shouldReturnFalseWhenOffenceDoesNotHaveAnyOfTheSpecifiedResults() {
        final DefendantCaseOffences offence = DefendantCaseOffences.defendantCaseOffences()
                .withResults(singletonList(results().withResultIdentifier(ADJ.id).build()))
                .build();

        boolean hasAnyResult = hasAnyResult(offence, asList(DISM, DINE, WDRN));

        assertThat(hasAnyResult, is(false));
    }

    @Test
    void shouldReturnTrueWhenApplicationHasSpecificResult() {
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withResults(singletonList(results().withResultIdentifier(RFSD.id).build()))
                        .build()
        );

        boolean hasResult = hasResultType(courtApplications, RFSD);

        assertThat(hasResult, is(true));
    }

    @Test
    void shouldReturnFalseWhenApplicationDoesNotHaveSpecificResult() {
        final List<CourtApplications> courtApplications = singletonList(
                CourtApplications.courtApplications()
                        .withResults(singletonList(results().withResultIdentifier(G.id).build()))
                        .build()
        );

        boolean hasResult = hasResultType(courtApplications, RFSD);

        assertThat(hasResult, is(false));
    }

    @Test
    void shouldReturnFalseWhenApplicationListIsEmpty() {
        final List<CourtApplications> courtApplications = emptyList();

        boolean hasResult = hasResultType(courtApplications, RFSD);

        assertThat(hasResult, is(false));
    }

    // Tests for hasAnyResultType
    @Test
    void shouldReturnTrueWhenResultsContainAnyOfTheSpecifiedTypes() {
        final List<Results> results = asList(
                results().withResultIdentifier(TEXT.id).build(),
                results().withResultIdentifier(OATS.id).build()
        );

        boolean hasAnyResultType = hasAnyResultType(results, asList(OATS.id, ADJ.id));

        assertThat(hasAnyResultType, is(true));
    }

    @Test
    void shouldReturnFalseWhenResultsDoNotContainAnyOfTheSpecifiedTypes() {
        final List<Results> results = asList(
                results().withResultIdentifier(TEXT.id).build(),
                results().withResultIdentifier(ERR.id).build()
        );

        boolean hasAnyResultType = hasAnyResultType(results, asList(OATS.id, ADJ.id));

        assertThat(hasAnyResultType, is(false));
    }

    @Test
    void shouldReturnTrueWhenSingleResultMatchesAnyOfTheSpecifiedTypes() {
        final Results result = results().withResultIdentifier(ADJ.id).build();

        boolean hasAnyResultType = hasAnyResultType(result, asList(OATS.id, ADJ.id));

        assertThat(hasAnyResultType, is(true));
    }

    @Test
    void shouldReturnFalseWhenSingleResultDoesNotMatchAnyOfTheSpecifiedTypes() {
        final Results result = results().withResultIdentifier(TEXT.id).build();

        boolean hasAnyResultType = hasAnyResultType(result, asList(OATS.id, ADJ.id));

        assertThat(hasAnyResultType, is(false));
    }

    @Test
    void shouldReturnTrueWhenResultsHaveD20Endorsement() {
        final List<Results> results = asList(
                results().withD20(false).build(),
                results().withD20(true).build()
        );

        boolean hasD20 = hasD20Endorsement(results);

        assertThat(hasD20, is(true));
    }

    @Test
    void shouldReturnFalseWhenResultsDoNotHaveD20Endorsement() {
        final List<Results> results = asList(
                results().withD20(false).build(),
                results().withD20(false).build()
        );

        boolean hasD20 = hasD20Endorsement(results);

        assertThat(hasD20, is(false));
    }

    @Test
    void shouldReturnTrueWhenResultsHavePointsDisqualificationCode() {
        final List<Results> results = asList(
                results().withPointsDisqualificationCode("TT99").build(),
                results().build()
        );

        boolean hasPointsDisqCode = hasPointsDisqualificationCode(results);

        assertThat(hasPointsDisqCode, is(true));
    }

    @Test
    void shouldReturnFalseWhenResultsDoNotHavePointsDisqualificationCode() {
        final List<Results> results = asList(
                results().build(),
                results().build()
        );

        boolean hasPointsDisqCode = hasPointsDisqualificationCode(results);

        assertThat(hasPointsDisqCode, is(false));
    }

    @Test
    void shouldHandleNullCourtApplicationsInHasAppealResult() {
        boolean hasAppealResult = hasAppealResult(null);

        assertThat(hasAppealResult, is(false));
    }

    @Test
    void shouldHandleEmptyCourtApplicationsInHasAppealResult() {
        boolean hasAppealResult = hasAppealResult(emptyList());

        assertThat(hasAppealResult, is(false));
    }

    @Test
    void shouldHandleNullOffenceInOffenceHasResult() {
        boolean hasResult = hasResultType((DefendantCaseOffences) null, SV);

        assertThat(hasResult, is(false));
    }

    @Test
    void shouldHandleOffenceWithNullResultsInOffenceHasResult() {
        final DefendantCaseOffences offence = DefendantCaseOffences.defendantCaseOffences().build();

        boolean hasResult = hasResultType(offence, SV);

        assertThat(hasResult, is(false));
    }
}
