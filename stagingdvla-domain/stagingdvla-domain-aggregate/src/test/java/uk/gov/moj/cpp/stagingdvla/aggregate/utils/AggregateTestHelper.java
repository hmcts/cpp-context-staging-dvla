package uk.gov.moj.cpp.stagingdvla.aggregate.utils;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.CONVICTED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ADJ;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.nowdocument.NowText;
import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;
import uk.gov.justice.cpp.stagingdvla.event.Results;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AggregateTestHelper {

    public static final UUID CASE_ID = randomUUID();
    private static final UUID RESULT_IDENTIFIER = randomUUID();
    private static final int ONE = 1;
    private static final int TWO = 2;
    public static final String NHCC = "fbed768b-ee95-4434-87c8-e81cbc8d24c8";

    public static CourtCentre getOrderingCourt(final String prefix, final boolean isMagsCourt) {
        return CourtCentre.courtCentre()
                .withId(randomUUID())
                .withName(prefix.concat("CourtName"))
                .withCode(isMagsCourt ?
                        ("MagsCourtCodeFor").concat(prefix) :
                        ("CrownCourtCodeFor").concat(prefix))
                .withCourtLocationCode(isMagsCourt ? null : "0433")
                .withLja(isMagsCourt ? LjaDetails.ljaDetails()
                        .withLjaCode(prefix.concat("LjaCode"))
                        .build() : null)
                .withAddress(Address.address()
                        .withAddress1(prefix.concat("CourtAddressLine1"))
                        .withPostcode(prefix.concat("CourtAddressPostCode"))
                        .build())
                .build();
    }

    public static Nowdefendant getDefendant(final String prefix) {
        return Nowdefendant.nowdefendant()
                .withFirstName(prefix.concat("FirstName"))
                .withLastName(prefix.concat("LastName"))
                .withAddress(Nowaddress.nowaddress()
                        .withLine1(prefix.concat("DefendantAddressLine1"))
                        .withPostCode(prefix.concat("DefendantAddressPostCode"))
                        .build())
                .build();
    }

    public static List<Cases> getCases(final String prefix, final boolean convicted, final String convictionDate) {
        return getCases(prefix, convicted, convictionDate, getPrompt("6"));
    }

    public static List<Cases> getCases(final String prefix, final boolean convicted, final String convictionDate, final List<Prompts> prompts) {
        return getCases(prefix, convicted, convictionDate, prompts, null);
    }

    public static List<Cases> getCases(final String prefix, final boolean convicted, final String convictionDate, final List<Prompts> prompts, final String pointsDisqualificationCode) {
        return getCases(prefix, convicted, convictionDate, prompts, pointsDisqualificationCode, 1);
    }

    public static List<Cases> getCases(final String prefix, final boolean convicted, final String convictionDate, final List<Prompts> prompts, final String pointsDisqualificationCode, final List<String> resultIdentifiers) {
        return getCases(prefix, convicted, convictionDate, prompts, pointsDisqualificationCode, 1, resultIdentifiers);
    }

    public static List<Cases> getCases(final String prefix, final boolean convicted, final String convictionDate, final List<Prompts> prompts, final String pointsDisqualificationCode, final int option, final List<String> resultIdentifiers) {
        return new LinkedList<Cases>(asList(Cases.cases()
                .withCaseId(CASE_ID)
                .withReference("CaseReference")
                .withDefendantCaseOffences(getOffences(1, prefix, asList(Boolean.TRUE), null, null, null,
                        convicted, convictionDate, prompts, pointsDisqualificationCode, option, true, resultIdentifiers))
                .build()));
    }

    public static List<Cases> getMultipleCases(final int count, final String prefix, final List<Boolean> d20List, final boolean convicted, final String convictionDate, final List<Prompts> prompts, final String pointsDisqualificationCode, final int option, final List<String> resultIdentifiers) {
        List<Cases> cases = new LinkedList<>();
        for (int i = 1; i <= count; i++) {
            cases.add(Cases.cases()
                    .withCaseId(randomUUID())
                    .withReference("CaseReference".concat(Integer.toString(i)))
                    .withDefendantCaseOffences(getOffences(1, prefix, asList(d20List.get(i - 1)), null, null, null,
                            convicted, convictionDate, prompts, pointsDisqualificationCode, option, true, resultIdentifiers))
                    .build());
        }
        return cases;
    }

    public static List<Cases> getCases(final boolean convicted, final String convictionDate, final List<Prompts> prompts, final String dvlaCode) {
        return new LinkedList<Cases>(asList(Cases.cases()
                .withCaseId(CASE_ID)
                .withReference("CaseReference")
                .withDefendantCaseOffences(getOffences(1, "", asList(Boolean.TRUE), asList(dvlaCode), null, null,
                        convicted, convictionDate, prompts, null, 1, false, null))
                .build()));
    }

    public static List<Cases> getCases(final String prefix, final boolean convicted, final String convictionDate, final List<Prompts> prompts, final String pointsDisqualificationCode, final int option) {
        return new LinkedList<Cases>(asList(Cases.cases()
                .withCaseId(CASE_ID)
                .withReference("CaseReference")
                .withDefendantCaseOffences(getOffences(1, prefix, asList(Boolean.TRUE), null, null, null,
                        convicted, convictionDate, prompts, pointsDisqualificationCode, option, true, null))
                .build()));
    }

    public static List<Cases> getCasesWithMultipleOffences(final int offenceCount, final String prefix,
                                                           final List<Boolean> d20List, final List<String> dvlaCodeList,
                                                           final boolean convicted, final String convictionDate, final List<Prompts> prompts,
                                                           final String pointsDisqualificationCode, final int option, final boolean sentenced) {
        return getCasesWithMultipleOffences(offenceCount, prefix, d20List, dvlaCodeList, null, null, convicted, convictionDate, prompts, pointsDisqualificationCode, option, sentenced, null);
    }

    public static List<Cases> getCasesWithMultipleOffences(final int offenceCount, final String prefix,
                                                           final List<Boolean> d20List, final List<String> dvlaCodeList,
                                                           final List<String> mainOffenceCodeList,
                                                           final List<String> offenceCodeList,
                                                           final boolean convicted, final String convictionDate, final List<Prompts> prompts,
                                                           final String pointsDisqualificationCode, final int option, final boolean sentenced,
                                                           final List<String> resultIdentifiers) {
        return new LinkedList<Cases>(asList(Cases.cases()
                .withCaseId(CASE_ID)
                .withReference("CaseReference")
                .withDefendantCaseOffences(getOffences(offenceCount, prefix, d20List, dvlaCodeList, mainOffenceCodeList, offenceCodeList, convicted, convictionDate, prompts, pointsDisqualificationCode, option, sentenced, resultIdentifiers))
                .build()));
    }

    private static List<DefendantCaseOffences> getOffences(final int offenceCount, final String prefix,
                                                           final List<Boolean> d20List, final List<String> dvlaCodeList,
                                                           final List<String> mainOffenceCodeList,
                                                           final List<String> offenceCodeList,
                                                           final boolean convicted, final String convictionDate, final List<Prompts> prompts,
                                                           final String pointsDisqualificationCode, final int option,
                                                           final boolean sentenced, final List<String> resultIdentifiers) {
        if (offenceCount > 0) {
            LinkedList<DefendantCaseOffences> offences = new LinkedList<>();
            for (int i = 1; i <= offenceCount; i++) {
                offences.add(getOffence(prefix,
                        isNotEmpty(d20List) ? d20List.get(i - 1) : true,
                        isNotEmpty(dvlaCodeList) ? dvlaCodeList.get(i - 1) : prefix.concat("DVLACode").concat(Integer.toString(i)),
                        isNotEmpty(mainOffenceCodeList) ? mainOffenceCodeList.get(i - 1) : "OffenceCode".concat(Integer.toString(i)),
                        isNotEmpty(offenceCodeList) ? offenceCodeList.get(i - 1) : "OffenceCode".concat(Integer.toString(i)),
                        convicted, convictionDate, prompts, pointsDisqualificationCode, option, sentenced, resultIdentifiers));
            }
            return offences;
        } else {
            return null;
        }
    }

    private static DefendantCaseOffences getOffence(final String prefix,
                                                    final boolean d20, final String dvlaCode,
                                                    final String mainOffenceCode,
                                                    final String offenceCode,
                                                    final boolean convicted, final String convictionDate, final List<Prompts> prompts,
                                                    final String pointsDisqualificationCode, final int option,
                                                    final boolean sentenced, final List<String> resultIdentifiers) {

        DefendantCaseOffences.Builder builder = DefendantCaseOffences.defendantCaseOffences()
                .withMainOffenceCode(mainOffenceCode)
                .withCode(offenceCode)
                .withTitle(option == ONE ? prefix.concat("OffenceTitle") : EMPTY)
                .withCivilOffence(option >= TWO ? Boolean.TRUE : null)
                .withStartDate(option == ONE ? prefix.concat("OffenceStartDate") : EMPTY)
                .withEndDate(option >= TWO ? prefix.concat("OffenceEndDate") : null)
                .withConvictionDate(convicted ? convictionDate : null)
                .withConvictingCourtCode(convicted ? prefix.concat("ConvictingCourtCode") : null)
                .withConvictionStatus(convicted ? CONVICTED : null)
                .withPlea(option == ONE ? prefix.concat("Plea") : null)
                .withVehicleRegistration(option >= TWO ? prefix.concat("VehicleRegistration") : EMPTY)
                .withDvlaCode(option == ONE ? prefix.concat(dvlaCode) : EMPTY)
                .withModeOfTrial(option >= TWO ? prefix.concat("ModeOfTrial") : EMPTY)
                .withAllocationDecision(option == ONE ? prefix.concat("AllocationDecision") : null)
                .withWording(option >= TWO ? prefix.concat("OffenceWording") : EMPTY)
                .withAlcoholReadingAmount(option >= TWO ? Integer.toString(option) : null)
                .withAlcoholReadingMethodCode(option >= TWO ? prefix.concat("A") : null)
                .withAlcoholReadingMethodDescription(option >= TWO ? prefix.concat("AlcoholReadingMethodDescription") : null)
                .withEndorsableFlag(option >= ONE ? true : null)
                .withResults(getResults(prefix, d20, prompts, pointsDisqualificationCode, dvlaCode, option, resultIdentifiers));

        if (sentenced) {
            builder.withSentencingCourtCode("SentencingCourtCode");
            builder.withSentenceDate("SentenceDate");
        }

        return builder.build();
    }

    public static DefendantCaseOffences getOffences(final String prefix,
                                                    final boolean d20, final String dvlaCode,
                                                    final String mainOffenceCode,
                                                    final String offenceCode,
                                                    final boolean convicted, final String convictionDate, final List<Prompts> prompts,
                                                    final String pointsDisqualificationCode, final int option,
                                                    final boolean sentenced, final List<String> resultIdentifiers, final String startDate) {

        DefendantCaseOffences.Builder builder = DefendantCaseOffences.defendantCaseOffences()
                .withMainOffenceCode(mainOffenceCode)
                .withCode(offenceCode)
                .withTitle(option == ONE ? prefix.concat("OffenceTitle") : EMPTY)
                .withCivilOffence(option >= TWO ? Boolean.TRUE : null)
                .withStartDate(startDate)
                .withEndDate(option >= TWO ? prefix.concat("OffenceEndDate") : null)
                .withConvictionDate(convicted ? convictionDate : null)
                .withConvictingCourtCode(convicted ? prefix.concat("ConvictingCourtCode") : null)
                .withConvictionStatus(convicted ? CONVICTED : null)
                .withPlea(option == ONE ? prefix.concat("Plea") : null)
                .withVehicleRegistration(option >= TWO ? prefix.concat("VehicleRegistration") : EMPTY)
                .withDvlaCode(option == ONE ? prefix.concat(dvlaCode) : EMPTY)
                .withModeOfTrial(option >= TWO ? prefix.concat("ModeOfTrial") : EMPTY)
                .withAllocationDecision(option == ONE ? prefix.concat("AllocationDecision") : null)
                .withWording(option >= TWO ? prefix.concat("OffenceWording") : EMPTY)
                .withAlcoholReadingAmount(option >= TWO ? Integer.toString(option) : null)
                .withAlcoholReadingMethodCode(option >= TWO ? prefix.concat("A") : null)
                .withAlcoholReadingMethodDescription(option >= TWO ? prefix.concat("AlcoholReadingMethodDescription") : null)
                .withEndorsableFlag(option >= ONE ? true : null)
                .withResults(getResults(prefix, d20, prompts, pointsDisqualificationCode, dvlaCode, option, resultIdentifiers));

        if (sentenced) {
            builder.withSentencingCourtCode("SentencingCourtCode");
            builder.withSentenceDate("SentenceDate");
        }

        return builder.build();
    }

    private static List<Results> getResults(final String prefix,
                                            final boolean d20, final List<Prompts> prompts,
                                            final String pointsDisqualificationCode,
                                            final String dvlaCode, final int option,
                                            final List<String> resultIdentifiers) {
        LinkedList<Results> results = new LinkedList<>();
        if (isNotEmpty(resultIdentifiers)) {
            resultIdentifiers.stream().forEach(
                    resultIdentifier -> {
                        if(ADJ.id.equals(resultIdentifier)) {
                            results.add(getResult(prefix, d20, prompts, pointsDisqualificationCode, dvlaCode, option, NHCC));
                        }
                        results.add(getResult(prefix, d20, prompts, pointsDisqualificationCode, dvlaCode, option, resultIdentifier));
                    });
        } else {
            results.add(getResult(prefix, d20, prompts, pointsDisqualificationCode, dvlaCode, option, RESULT_IDENTIFIER.toString()));
        }
        return results;
    }

    private static Results getResult(final String prefix, final boolean d20, final List<Prompts> prompts,
                                     final String pointsDisqualificationCode, final String dvlaCode, final int option, final String resultIdentifier) {
        return Results.results()
                .withResultIdentifier(resultIdentifier)
                .withLabel(prefix.concat("Label"))
                .withNowRequirementText(option == ONE ?
                        asList(NowText.nowText().withLabel(prefix.concat("NowTextLabel")).withValue("NowTextValue").build()) : null)
                .withPublishedForNows(option == TWO ? true : null)
                .withPrompts((ADJ.id.equals(resultIdentifier)) ? asList(Prompts.prompts().withPromptIdentifier(NHCC).build()) : prompts)
                .withResultWording(option == ONE ? prefix.concat("ResultWording") : null)
                .withResultDefinitionGroup(option == TWO ? prefix.concat("ResultDefinitionGroup") : (NHCC.equals(resultIdentifier)) ? "Next hearing" : EMPTY)
                .withSequence(option == ONE ? BigDecimal.ONE : null)
                .withD20(d20)
                .withPointsDisqualificationCode(pointsDisqualificationCode)
                .withDrivingTestStipulation(option == ONE ? 1 : null)
                .withDvlaCode(option == ONE ? prefix.concat(dvlaCode) : EMPTY)
                .build();
    }

    public static List<String> getCaseReferences(final String prefix) {
        return asList(prefix.concat("CaseReference"));
    }

    public static List<String> getCaseReferences() {
        return asList("CaseReference");
    }

    public static DriverNotified getPreviousDriverNotified(final String prefix, final boolean convicted, final String convictionDate, final boolean isMagsCourt, final String orderDate) {
        return getPreviousDriverNotified(prefix, convicted, convictionDate, isMagsCourt, getPrompt("3"), false, orderDate);
    }

    public static DriverNotified getPreviousDriverNotified(final String prefix, final boolean convicted, final String convictionDate, final boolean isMagsCourt, final List<Prompts> prompts, final boolean sentenced, final String orderDate) {
        return getPreviousDriverNotified(prefix, convicted, convictionDate, isMagsCourt, prompts, 1, sentenced, orderDate);
    }

    public static DriverNotified getPreviousDriverNotified(final String prefix, final boolean convicted, final String convictionDate, final boolean isMagsCourt, final List<Prompts> prompts, final int option, final boolean sentenced, final String orderDate) {
        return getPreviousDriverNotified(1, prefix, asList(Boolean.TRUE), null, convicted, convictionDate, isMagsCourt, prompts, option, sentenced, orderDate);
    }

    public static DriverNotified getPreviousDriverNotified(final int offenceCount, final String prefix,
                                                           final List<Boolean> d20List, final List<String> dvlaCodeList,
                                                           final boolean convicted, final String convictionDate, final boolean isMagsCourt,
                                                           final List<Prompts> prompts, final int option,
                                                           final boolean sentenced, final String orderDate) {
        return getPreviousDriverNotified(offenceCount, prefix, d20List, dvlaCodeList, null, null, convicted, convictionDate, isMagsCourt, prompts, option, sentenced, null, orderDate, null);
    }

    public static DriverNotified getPreviousDriverNotified(final int offenceCount, final String prefix,
                                                           final List<Boolean> d20List, final List<String> dvlaCodeList,
                                                           final List<String> mainOffenceCodeList,
                                                           final List<String> offenceCodeList,
                                                           final boolean convicted, final String convictionDate, final boolean isMagsCourt,
                                                           final List<Prompts> prompts, final int option,
                                                           final boolean sentenced, final List<String> resultIdentifiers,
                                                           final String orderDate, final String amendmentDate) {
        DriverNotified.Builder builder = DriverNotified.driverNotified()
                .withOrderDate(orderDate)
                .withAmendmentDate(amendmentDate)
                .withConvictionDate(convicted ? convictionDate : null)
                .withMaterialId(randomUUID())
                .withOrderingHearingId(randomUUID())
                .withOrderingCourt(getOrderingCourt(prefix, isMagsCourt))
                .withDefendant(getDefendant(prefix))
                .withCaseApplicationReferences(getCaseReferences(prefix))
                .withNotificationWasPreviouslySent(false);

        builder.withCases(getCasesWithMultipleOffences(offenceCount, prefix, d20List, dvlaCodeList, mainOffenceCodeList, offenceCodeList, convicted, convictionDate, prompts, null, option, sentenced, resultIdentifiers));

        return builder.build();
    }

    public static List<Prompts> getPromptsForDurationSeqNull(String value1) {
        return new LinkedList<Prompts>(asList(
                Prompts.prompts()
                        .withPromptIdentifier("PromptIdentifierForPrompt")
                        .withPromptReference("PromptReference")
                        .withLabel("PromptLabel")
                        .withValue(value1)
                        .build()));
    }

    public static List<Prompts> getPrompt(String value) {
        return asList(Prompts.prompts()
                .withPromptIdentifier("PromptIdentifierForPrompt")
                .withPromptReference("PromptReference")
                .withLabel("PromptLabel")
                .withValue(value).build());
    }

    public static List<Prompts> getPromptsForDurationSeqOne(String value1) {
        return new LinkedList<Prompts>(asList(
                Prompts.prompts()
                        .withPromptIdentifier("PromptIdentifierForPrompt")
                        .withPromptReference("PromptReference")
                        .withLabel("PromptLabel")
                        .withDurationSequence(BigDecimal.ONE)
                        .withValue(value1)
                        .build()));
    }

    public static List<Prompts> getPromptsForDurationSeqTwo(String value1, String value2) {
        return new LinkedList<Prompts>(asList(
                Prompts.prompts()
                        .withPromptIdentifier("PromptIdentifierForPrompt")
                        .withPromptReference("PromptReference")
                        .withLabel("PromptLabel")
                        .withDurationSequence(BigDecimal.ONE)
                        .withValue(value1)
                        .build(),
                Prompts.prompts()
                        .withPromptIdentifier("PromptIdentifierForPrompt")
                        .withPromptReference("PromptReference")
                        .withLabel("PromptLabel")
                        .withDurationSequence(BigDecimal.valueOf(2L))
                        .withValue(value2)
                        .build()));
    }

    public static boolean checkForEndorsableOffences(final List<Cases> cases) {
        return cases.stream()
                .filter(Objects::nonNull)
                .anyMatch(aCase -> aCase.getDefendantCaseOffences().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(anOffence -> anOffence.getResults().stream()
                                .filter(Objects::nonNull)
                                .anyMatch(aResult -> nonNull(aResult.getD20()) && aResult.getD20())));
    }

    public static List<CourtApplications> getCourtApplications(final List<String> codes,
                                                               final String prefix,
                                                               final boolean d20, final List<Prompts> prompts,
                                                               final String pointsDisqualificationCode,
                                                               final String dvlaCode, final int option,
                                                               final List<String> resultIdentifiers) {
        List<CourtApplications> courtApplications = new ArrayList<>();

        codes.forEach(code ->
                courtApplications.add(
                        CourtApplications.courtApplications()
                                .withApplicationCode(code)
                                .withId(randomUUID())
                                .withApplicationReference(randomUUID().toString())
                                .withApplicationReceivedDate(LocalDate.now().toString())
                                .withResults(getResults(prefix, d20, prompts, pointsDisqualificationCode, dvlaCode, option, resultIdentifiers))
                                .build())
        );

        return courtApplications;
    }
}
