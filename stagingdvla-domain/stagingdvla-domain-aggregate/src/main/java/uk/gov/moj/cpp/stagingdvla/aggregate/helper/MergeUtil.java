package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.getDistinctPromptReferences;

import uk.gov.justice.core.courts.nowdocument.NowText;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DistinctPrompts;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;
import uk.gov.justice.cpp.stagingdvla.event.Results;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"squid:S1118", "squid:S1188"})
public class MergeUtil {

    public static DefendantCaseOffences mergeOffence(DefendantCaseOffences offence, DefendantCaseOffences previousOffence) {
        return DefendantCaseOffences.defendantCaseOffences()
                .withValuesFrom(offence)
                .withTitle((String) mergeValue(offence.getTitle(), previousOffence.getTitle()))
                .withCivilOffence((Boolean) mergeValue(offence.getCivilOffence(), previousOffence.getCivilOffence()))
                .withStartDate((String) mergeValue(offence.getStartDate(), previousOffence.getStartDate()))
                .withEndDate((String) mergeValue(offence.getEndDate(), previousOffence.getEndDate()))
                .withConvictionDate((String) mergeValue(offence.getConvictionDate(), previousOffence.getConvictionDate()))
                .withConvictingCourtCode((String) mergeValue(offence.getConvictingCourtCode(), previousOffence.getConvictingCourtCode()))
                .withConvictionStatus((String) mergeValue(offence.getConvictionStatus(), previousOffence.getConvictionStatus()))
                .withPlea((String) mergeValue(offence.getPlea(), previousOffence.getPlea()))
                .withVehicleRegistration((String) mergeValue(offence.getVehicleRegistration(), previousOffence.getVehicleRegistration()))
                .withDvlaCode((String) mergeValue(offence.getDvlaCode(), previousOffence.getDvlaCode()))
                .withModeOfTrial((String) mergeValue(offence.getModeOfTrial(), previousOffence.getModeOfTrial()))
                .withAllocationDecision((String) mergeValue(offence.getAllocationDecision(), previousOffence.getAllocationDecision()))
                .withWording((String) mergeValue(offence.getWording(), previousOffence.getWording()))
                .withAlcoholReadingAmount((String) mergeValue(offence.getAlcoholReadingAmount(), previousOffence.getAlcoholReadingAmount()))
                .withAlcoholReadingMethodCode((String) mergeValue(offence.getAlcoholReadingMethodCode(), previousOffence.getAlcoholReadingMethodCode()))
                .withAlcoholReadingMethodDescription((String) mergeValue(offence.getAlcoholReadingMethodDescription(), previousOffence.getAlcoholReadingMethodDescription()))
                .withEndorsableFlag((Boolean) mergeValue(offence.getEndorsableFlag(), previousOffence.getEndorsableFlag()))
                .withResults(mergeResults(offence.getResults(), previousOffence.getResults()))
                .withFine((String) mergeValue(offence.getFine(), previousOffence.getFine()))
                .withPenaltyPoints((String) mergeValue(offence.getPenaltyPoints(), previousOffence.getPenaltyPoints()))
                .withDisqualificationPeriod((String) mergeValue(offence.getDisqualificationPeriod(), previousOffence.getDisqualificationPeriod()))
                .withOtherSentence((String) mergeValue(offence.getOtherSentence(), previousOffence.getOtherSentence()))
                .withSuspendedSentence((String) mergeValue(offence.getSuspendedSentence(), previousOffence.getSuspendedSentence()))
                .withDttpDtetp((String) mergeValue(offence.getDttpDtetp(), previousOffence.getDttpDtetp()))
                .withInterimImposedFinalSentence((String) mergeValue(offence.getInterimImposedFinalSentence(), previousOffence.getInterimImposedFinalSentence()))
                .withSentencingCourtCode(offence.getSentencingCourtCode())
                .withSentenceDate(offence.getSentenceDate())
                .withDateFromWhichDisqRemoved((String) mergeValue(offence.getDateFromWhichDisqRemoved(), previousOffence.getDateFromWhichDisqRemoved()))
                .withDateDisqSuspendedPendingAppeal((String) mergeValue(offence.getDateDisqSuspendedPendingAppeal(), previousOffence.getDateDisqSuspendedPendingAppeal()))
                .withDateDisqReimposedFollowingAppeal((String) mergeValue(offence.getDateDisqReimposedFollowingAppeal(), previousOffence.getDateDisqReimposedFollowingAppeal()))
                .build();
    }

    private static List<Results> mergeResults(final List<Results> results, final List<Results> previousResults) {
        if (isNotEmpty(results) && isNotEmpty(previousResults)) {
            final List<Results> mergedResults = new ArrayList<>();
            results.forEach(result -> {
                if (isNotEmpty(result.getResultIdentifier())) {
                    final Results previousResult = previousResults.stream()
                            .filter(bResult -> result.getResultIdentifier().equals(bResult.getResultIdentifier())).findFirst().orElse(null);
                    if (nonNull(previousResult)) {
                        mergedResults.add(mergeResult(result, previousResult));
                    } else {
                        mergedResults.add(result);
                    }
                } else {
                    mergedResults.add(result);
                }
            });
            return mergedResults;
        } else {
            return results;
        }
    }

    private static Results mergeResult(final Results result, final Results previousResult) {
        return Results.results()
                .withValuesFrom(result)
                .withNowRequirementText((List<NowText>) mergeValue(result.getNowRequirementText(), previousResult.getNowRequirementText()))
                .withPublishedForNows((Boolean) mergeValue(result.getPublishedForNows(), previousResult.getPublishedForNows()))
                .withPrompts(mergePrompts(result.getPrompts(), previousResult.getPrompts()))
                .withResultWording((String) mergeValue(result.getResultWording(), previousResult.getResultWording()))
                .withResultDefinitionGroup((String) mergeValue(result.getResultDefinitionGroup(), previousResult.getResultDefinitionGroup()))
                .withSequence((BigDecimal) mergeValue(result.getSequence(), previousResult.getSequence()))
                .withD20((Boolean) mergeValue(result.getD20(), previousResult.getD20()))
                .withPointsDisqualificationCode((String) mergeValue(result.getPointsDisqualificationCode(), previousResult.getPointsDisqualificationCode()))
                .withDrivingTestStipulation((Integer) mergeValue(result.getDrivingTestStipulation(), previousResult.getDrivingTestStipulation()))
                .withDvlaCode((String) mergeValue(result.getDvlaCode(), previousResult.getDvlaCode()))
                .build();
    }

    private static List<Prompts> mergePrompts(final List<Prompts> prompts, final List<Prompts> previousPrompts) {
        if (isNotEmpty(prompts) && isNotEmpty(previousPrompts)) {
            final List<Prompts> mergedPrompts = new ArrayList<>();
            prompts.forEach(prompt -> {
                if (isNotEmpty(prompt.getPromptReference())) {
                    final Prompts previousPrompt = previousPrompts.stream()
                            .filter(bPrompt -> prompt.getPromptReference().equals(bPrompt.getPromptReference())).findFirst().orElse(null);
                    if (nonNull(previousPrompt)) {
                        mergedPrompts.add(mergePrompt(prompt, previousPrompt));
                    } else {
                        mergedPrompts.add(prompt);
                    }
                } else {
                    mergedPrompts.add(prompt);
                }
            });
            return mergedPrompts;
        } else {
            return prompts;
        }
    }

    private static Prompts mergePrompt(Prompts prompt, Prompts previousPrompt) {
        return Prompts.prompts()
                .withValuesFrom(prompt)
                .withLabel((String) mergeValue(prompt.getLabel(), previousPrompt.getLabel()))
                .withWelshLabel((String) mergeValue(prompt.getWelshLabel(), previousPrompt.getWelshLabel()))
                .withValue((String) mergeValue(prompt.getValue(), previousPrompt.getValue()))
                .withWelshValue((String) mergeValue(prompt.getWelshValue(), previousPrompt.getWelshValue()))
                .withDurationSequence((BigDecimal) mergeValue(prompt.getDurationSequence(), previousPrompt.getDurationSequence()))
                .build();
    }

    private static Object mergeValue(Object current, Object previous) {
        if (current instanceof String) {
            if (isNotEmpty((String) current)) {
                return current;
            } else {
                return previous;
            }
        } else if (current instanceof List) {
            if (isNotEmpty((List) current)) {
                return current;
            } else {
                return previous;
            }
        } else {
            if (nonNull(current)) {
                return current;
            } else {
                return previous;
            }
        }
    }

    public static Map<String, DistinctPrompts> getDistinctPrompts(final List<Cases> cases, final DriverNotified previousDriverNotified) {
        final List<Cases> combinedCases = new ArrayList<>();
        if (isNotEmpty(cases)) {
            combinedCases.addAll(cases);
        }
        if (nonNull(previousDriverNotified) && isNotEmpty(previousDriverNotified.getCases())) {
            combinedCases.addAll(previousDriverNotified.getCases());
        }

        final Map<String, DistinctPrompts> distinctPrompts = new HashMap<>();
        if (isNotEmpty(combinedCases)) {
            combinedCases.stream()
                    .filter(aCase -> nonNull(aCase.getDefendantCaseOffences()))
                    .forEach(aCase -> aCase.getDefendantCaseOffences().stream()
                            .filter(offence -> nonNull(offence.getResults()))
                            .forEach(offence -> offence.getResults().stream()
                                    .filter(result -> nonNull(result.getPrompts()))
                                    .forEach(result -> result.getPrompts().stream()
                                            .filter(prompt -> isNotEmpty(prompt.getPromptReference()))
                                            .forEach(prompt -> {
                                                        if (getDistinctPromptReferences().contains(prompt.getPromptReference())
                                                                && !distinctPrompts.containsKey(prompt.getPromptReference())
                                                                && isNotEmpty(prompt.getValue())) {
                                                            final DistinctPrompts distinctPrompt = DistinctPrompts.distinctPrompts()
                                                                    .withPromptIdentifier(prompt.getPromptIdentifier())
                                                                    .withPromptReference(prompt.getPromptReference())
                                                                    .withDurationSequence(prompt.getDurationSequence())
                                                                    .withLabel(prompt.getLabel())
                                                                    .withValue(prompt.getValue())
                                                                    .build();
                                                            distinctPrompts.put(prompt.getPromptReference(), distinctPrompt);
                                                        }
                                                    }
                                            )
                                    )));
        }

        return distinctPrompts;
    }
}
