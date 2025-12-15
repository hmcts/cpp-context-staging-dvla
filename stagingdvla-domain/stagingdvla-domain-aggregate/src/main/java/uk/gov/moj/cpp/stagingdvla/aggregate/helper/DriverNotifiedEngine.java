package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.C_FOR_CROWN;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.REMOVE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.UPDATE_MERGE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.POINTS_DISQUALIFICATION_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.REMOVED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.LPIC1;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.LPIC2;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.LPIC3;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.LPIC4;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.LPIC5;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.UPDATED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.DisqualificationPeriodHelper.getDisqualificationPeriod;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.MergeUtil.getDistinctPrompts;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.MergeUtil.mergeOffence;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getAlcoholReadingAmount;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getApplicationResults;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getCaseResults;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getConvictingCourtCode;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getDateDisqReimposedFollowingAppeal;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getDateDisqSuspendedPendingAppeal;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getDateFromWhichDisqRemoved;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getDttpDtetp;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getDvlaCode;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getEndorsementStatus;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getFine;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getInterimImposedFinalSentence;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getOtherSentence;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getPenaltyPoints;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getSentenceDate;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getSentencingCourtCode;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getSuspendedSentence;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyD20Removed;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyResultOrPromptModified;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAnyResultType;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealResult;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasD20Endorsement;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasPointsDisqualificationCode;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasResultType;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isRefused;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isResultAnAdjournment;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DistinctPrompts;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.Previous;
import uk.gov.justice.cpp.stagingdvla.event.Results;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

@SuppressWarnings({"squid:S1118", "squid:S1602", "squid:S1612", "squid:S1188", "squid:S3776"})
public class DriverNotifiedEngine {

    private static final Logger LOGGER = getLogger(DriverNotifiedEngine.class);

    /**
     * 1. Process each case and create DriverNotified event.
     *
     * @param previousDriverNotifiedByCase
     * @param orderDate
     * @param orderingCourt
     * @param amendmentDate
     * @param defendant
     * @param currentCases
     * @param hearingId
     * @param courtApplications
     * @return
     */
    public static List<DriverNotified> transformDriverNotified(
            final Map<String, DriverNotified> previousDriverNotifiedByCase,
            final Map<String, DriverNotified> previousPreviousDriverNotifiedByCase,
            final String orderDate,
            final CourtCentre orderingCourt,
            final String amendmentDate,
            final Nowdefendant defendant,
            final List<Cases> currentCases,
            final UUID hearingId,
            final List<CourtApplications> courtApplications,
            final Boolean isReshare) {

        final List<DriverNotified> driverNotifiedList = currentCases
                .stream()
                .map(currentCase -> processCase(previousDriverNotifiedByCase.get(currentCase.getReference()),
                        previousPreviousDriverNotifiedByCase.get(currentCase.getReference()),
                        orderDate,
                        orderingCourt,
                        amendmentDate,
                        defendant,
                        currentCase,
                        hearingId,
                        courtApplications,
                        isReshare
                ))
                .filter(Objects::nonNull)
                .collect(toList());

        LOGGER.info("{} event(s) created for hearing id: {}", driverNotifiedList.size(), hearingId);

        return driverNotifiedList;
    }

    private static DriverNotified processCase(final DriverNotified previousDriverNotified,
                                              final DriverNotified previousPreviousDriverNotified,
                                              final String orderDate,
                                              final CourtCentre orderingCourt,
                                              final String amendmentDate,
                                              final Nowdefendant defendant,
                                              final Cases currentCase,
                                              final UUID hearingId,
                                              final List<CourtApplications> courtApplications,
                                              final Boolean isReshare) {

        LOGGER.info("Processing case: {}", currentCase.getReference());

        if(Boolean.TRUE.equals(isReshare) && hasToUsePreviousEvent(currentCase, courtApplications)){
            if (nonNull(previousPreviousDriverNotified)) {
                return DriverNotified.driverNotified()
                        .withValuesFrom(previousPreviousDriverNotified)
                        //endorsement
                        .withIsCopiedFromPrevious(true)
                        .build();
            } else {
                return null;
            }
        }

        // Get previous case using reference number
        final Cases previousCase = nonNull(previousDriverNotified) ? previousDriverNotified.getCases()
                .stream()
                .filter((aCase -> equalsIgnoreCase(currentCase.getReference(), aCase.getReference())))
                .findFirst()
                .orElse(null) : null;

        LOGGER.info("Previous event found for case: {}", nonNull(previousCase));

        final boolean hasD20Removed = hasAnyD20Removed(previousCase, currentCase, courtApplications);

        LOGGER.info("Any D20 removed for case: {}", hasD20Removed);

        // Hearing adjourned, previous offences will be not be changed
        if (isEmpty(amendmentDate) && isEmpty(courtApplications)) {
            // if case not amended or not appeal results.
            // check for subsequent hearing
            removeNonMatchingOffencesFromPrevious(currentCase, previousDriverNotified);
        }

        removeNonEndorsableOffences(currentCase, asList(SV));

        boolean hasResultOrPromptModified = false;

        if (isNotEmpty(amendmentDate)) {
            hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCase, currentCase);
        }

        if (hasD20Removed ||
                isD20ShouldBeGenerated(currentCase,
                        amendmentDate, previousDriverNotified, courtApplications, hasResultOrPromptModified)) {

            final List<Cases> cases = getUpdatedCases(currentCase);
            final List<String> removedOffenceCodes = removeNonEndorsableOffences(currentCase);
            final String orderingCourtCode = getCourtCode(orderingCourt);

            final DriverNotified.Builder builder = DriverNotified.driverNotified()
                    .withOrderDate(orderDate)
                    .withAmendmentDate(amendmentDate)
                    .withMaterialId(UUID.randomUUID())
                    .withOrderingHearingId(hearingId)
                    .withOrderingCourt(orderingCourt)
                    .withOrderingCourtCode(orderingCourtCode)
                    .withDefendant(defendant)
                    .withCaseApplicationReferences(singletonList(currentCase.getReference()))
                    .withCourtApplications(courtApplications);

            final List<Cases> previousCases = previousHasOffence(previousDriverNotified) ? previousDriverNotified.getCases() : null;
            updateOffences(cases, courtApplications, previousCases, amendmentDate, orderDate, orderingCourtCode);
            if (previousHasOffence(previousDriverNotified)) {
                builder.withPrevious(getPrevious(previousDriverNotified));
                builder.withNotificationWasPreviouslySent(true);

                final Map<String, List<String>> endorsementStatuses = getEndorsementStatuses(amendmentDate, orderDate, previousDriverNotified, cases, courtApplications);
                updateEndorsementStatusMapIfNeeded(removedOffenceCodes, endorsementStatuses);
                builder.withRemovedEndorsements(endorsementStatuses.get(REMOVED));
                builder.withUpdatedEndorsements(endorsementStatuses.get(UPDATED));
            }

            final Map<String, DistinctPrompts> distinctPrompts = getDistinctPrompts(cases, previousDriverNotified);
            builder.withDistinctPrompts(new ArrayList<>(distinctPrompts.values()));
            builder.withLicenceProducedInCourt(getLicenceProducedInCourt(cases, previousDriverNotified));
            builder.withCases(cases);
            builder.withIdentifier(randomUUID());

            return builder.build();
        }
        return null;
    }

    private static boolean hasToUsePreviousEvent( final Cases currentCase, final List<CourtApplications> courtApplications) {
        final List<Results> caseResults = getCaseResults(currentCase);
        final List<Results> applicationResults = getApplicationResults(courtApplications);
        final boolean isRefused = CollectionUtils.isNotEmpty(courtApplications) && courtApplications.stream().allMatch(OffenceUtil::isRefused);
        return (caseResults.isEmpty() || isResultAnAdjournment(caseResults)) && (
                isRefused || isResultAnAdjournment(applicationResults));
    }

    private static List<Cases> getUpdatedCases(final Cases cases) {
        final List<Cases> updatedCases = new ArrayList<>();
        updatedCases.add(Cases.cases()
                .withValuesFrom(cases)
                .withHasSV(hasSV(cases))
                .build());
        return updatedCases;
    }

    private static boolean hasSV(final Cases aCase) {
        return isNotEmpty(aCase.getDefendantCaseOffences())
                && aCase.getDefendantCaseOffences().stream().anyMatch(offence -> hasResultType(offence, SV));
    }

    private static void updateOffences(final List<Cases> cases, final List<CourtApplications> courtApplications, final List<Cases> previousCases, final String amendmentDate, final String orderDate, final String orderingCourtCode) {
        cases.stream().forEach(currentCase -> {
            final Cases previousCase = isNotEmpty(previousCases) ?
                    previousCases.stream().filter(aCase -> currentCase.getCaseId().equals(aCase.getCaseId())).findFirst().orElse(null) : null;
            updateOffences(currentCase, courtApplications, previousCase, amendmentDate, orderDate, orderingCourtCode);
        });
    }

    private static void updateOffences(final Cases currentCase, final List<CourtApplications> courtApplications, final Cases previousCase, final String amendmentDate, final String orderDate, final String orderingCourtCode) {
        final List<DefendantCaseOffences> updatedOffences = new ArrayList<>();
        if (isNotEmpty(currentCase.getDefendantCaseOffences())) {
            currentCase.getDefendantCaseOffences().forEach(currentOffence -> {
                final DefendantCaseOffences previousOffence = nonNull(previousCase) && isNotEmpty(previousCase.getDefendantCaseOffences()) ?
                        previousCase.getDefendantCaseOffences().stream().filter(aOffence -> currentOffence.getCode().equalsIgnoreCase(aOffence.getCode())).findFirst().orElse(null) : null;
                final DefendantCaseOffences updatedOffence = updateOffence(currentOffence, courtApplications, previousOffence, amendmentDate, orderDate, orderingCourtCode);
                if (hasPointsDisqualificationCode(updatedOffence.getResults())) {
                    updatedOffences.add(DefendantCaseOffences.defendantCaseOffences()
                            .withValuesFrom(updatedOffence)
                            .withDisqualificationPeriod(null)
                            .withDateDisqSuspendedPendingAppeal(null)
                            .withDttpDtetp(null)
                            .build());

                    updatedOffences.add(DefendantCaseOffences.defendantCaseOffences()
                            .withMainOffenceCode(updatedOffence.getCode())
                            .withCode(isNotEmpty(updatedOffence.getCode()) ?
                                    updatedOffence.getCode().concat(POINTS_DISQUALIFICATION_CODE) : POINTS_DISQUALIFICATION_CODE)
                            .withDvlaCode(POINTS_DISQUALIFICATION_CODE)
                            .withWording(updatedOffence.getWording())
                            .withConvictionDate(updatedOffence.getConvictionDate())
                            .withConvictingCourtCode(updatedOffence.getConvictingCourtCode())
                            .withDisqualificationPeriod(updatedOffence.getDisqualificationPeriod())
                            .withSentencingCourtCode(updatedOffence.getSentencingCourtCode())
                            .withSentenceDate(updatedOffence.getSentenceDate())
                            .withDateDisqSuspendedPendingAppeal(updatedOffence.getDateDisqSuspendedPendingAppeal())
                            .withDttpDtetp(updatedOffence.getDttpDtetp())
                            .build()
                    );
                } else {
                    updatedOffences.add(updatedOffence);
                }
            });
        }

        final List<DefendantCaseOffences> defendantCaseOffences = currentCase.getDefendantCaseOffences();
        if (nonNull(defendantCaseOffences)) {
            defendantCaseOffences.clear();
            defendantCaseOffences.addAll(updatedOffences);
        }
    }

    private static DefendantCaseOffences updateOffence(DefendantCaseOffences currentOffence, final List<CourtApplications> courtApplications, final DefendantCaseOffences previousOffence, final String amendmentDate, final String orderDate, final String orderingCourtCode) {
        final DefendantCaseOffences.Builder builder = DefendantCaseOffences.defendantCaseOffences()
                .withValuesFrom(currentOffence);

        return builder.withDvlaCode(getDvlaCode(currentOffence))
                .withConvictingCourtCode(getConvictingCourtCode(currentOffence, previousOffence))
                .withResults(currentOffence.getResults())
                .withAlcoholReadingAmount(getAlcoholReadingAmount(currentOffence))
                .withFine(getFine(currentOffence))
                .withPenaltyPoints(getPenaltyPoints(currentOffence))
                .withDisqualificationPeriod(getDisqualificationPeriod(currentOffence, orderDate))
                .withOtherSentence(getOtherSentence(currentOffence.getResults()))
                .withSuspendedSentence(getSuspendedSentence(currentOffence.getResults()))
                .withDttpDtetp(getDttpDtetp(currentOffence))
                .withInterimImposedFinalSentence(getInterimImposedFinalSentence(currentOffence))
                .withSentencingCourtCode(getSentencingCourtCode(currentOffence, courtApplications, previousOffence, amendmentDate, orderDate, orderingCourtCode))
                .withSentenceDate(getSentenceDate(currentOffence, courtApplications, previousOffence, amendmentDate, orderDate))
                .withDateFromWhichDisqRemoved(getDateFromWhichDisqRemoved(currentOffence))
                .withDateDisqSuspendedPendingAppeal(getDateDisqSuspendedPendingAppeal(currentOffence, courtApplications, previousOffence, amendmentDate, orderDate))
                .withDateDisqReimposedFollowingAppeal(getDateDisqReimposedFollowingAppeal(courtApplications, orderDate))
                .build();
    }

    private static void removeNonMatchingOffencesFromPrevious(final Cases currentCase, final DriverNotified previousDriverNotified) {
        if (nonNull(currentCase) && previousHasOffence(previousDriverNotified)) {
            previousDriverNotified.getCases().forEach(previousCase -> {
                if (isNotEmpty(previousCase.getDefendantCaseOffences())) {
                    if (nonNull(currentCase) && isNotEmpty(currentCase.getDefendantCaseOffences())) {
                        previousCase.getDefendantCaseOffences().removeIf(previousOffence -> currentCase.getDefendantCaseOffences().stream()
                                .noneMatch(currentOffence -> previousOffence.getMainOffenceCode().equalsIgnoreCase(currentOffence.getMainOffenceCode())));
                    } else {
                        previousCase.getDefendantCaseOffences().clear();
                    }
                }
            });
        }
    }

    private static boolean previousHasOffence(DriverNotified previousDriverNotified) {
        return nonNull(previousDriverNotified)
                && isNotEmpty(previousDriverNotified.getCases())
                && previousDriverNotified.getCases().stream().anyMatch(aCase -> isNotEmpty(aCase.getDefendantCaseOffences()));
    }

    private static Previous getPrevious(final DriverNotified previousDriverNotified) {
        return Previous.previous()
                .withIdentifier(previousDriverNotified.getIdentifier())
                .withMasterDefendantId(previousDriverNotified.getMasterDefendantId())
                .withRetrySequence(previousDriverNotified.getRetrySequence())
                .withOrderDate(previousDriverNotified.getOrderDate())
                .withAmendmentDate(previousDriverNotified.getAmendmentDate())
                .withConvictionDate(previousDriverNotified.getConvictionDate())
                .withMaterialId(previousDriverNotified.getMaterialId())
                .withOrderingHearingId(previousDriverNotified.getOrderingHearingId())
                .withOrderingCourt(previousDriverNotified.getOrderingCourt())
                .withLicenceProducedInCourt(previousDriverNotified.getLicenceProducedInCourt())
                .withDefendant(previousDriverNotified.getDefendant())
                .withCaseApplicationReferences(previousDriverNotified.getCaseApplicationReferences())
                .withCases(previousDriverNotified.getCases())
                .withNotificationWasPreviouslySent(previousDriverNotified.getNotificationWasPreviouslySent())
                .withDistinctPrompts(previousDriverNotified.getDistinctPrompts())
                .build();
    }

    private static String getCourtCode(final CourtCentre courtCentre) {
        String courtCode = null;
        if (nonNull(courtCentre)) {
            if (isNotEmpty(courtCentre.getCode())
                    && courtCentre.getCode().toUpperCase().startsWith(C_FOR_CROWN)) {
                courtCode = courtCentre.getCourtLocationCode();
            }
            if (isEmpty(courtCode) && nonNull(courtCentre.getLja())
                    && isNotEmpty(courtCentre.getLja().getLjaCode())) {
                courtCode = courtCentre.getLja().getLjaCode();
            }
        }
        return courtCode;
    }

    private static String getLicenceProducedInCourt(final List<Cases> cases, final DriverNotified previousDriverNotified) {
        final AtomicReference<String> licenceProducedInCourt = new AtomicReference<>(EMPTY);
        if (isNotEmpty(cases)) {
            cases.stream().filter(aCase -> isNotEmpty(aCase.getDefendantCaseOffences())).forEach(aCase -> {
                aCase.getDefendantCaseOffences().stream().filter(offence -> isNotEmpty(offence.getResults())).forEach(offence -> {
                    offence.getResults().forEach(result -> {
                        if (hasAnyResultType(result, asList(LPIC1, LPIC2, LPIC3, LPIC4, LPIC5))) {
                            licenceProducedInCourt.set(result.getLabel());
                        }
                    });
                });
            });
        }

        if (isNotEmpty(licenceProducedInCourt.get())) {
            return licenceProducedInCourt.get();
        } else if (nonNull(previousDriverNotified) && isNotEmpty(previousDriverNotified.getLicenceProducedInCourt())) {
            return previousDriverNotified.getLicenceProducedInCourt();
        } else {
            return null;
        }
    }

    private static Map<String, List<String>> getEndorsementStatuses(final String amendmentDate,
                                                                    final String orderDate,
                                                                    final DriverNotified previousDriverNotified,
                                                                    final List<Cases> cases,
                                                                    final List<CourtApplications> courtApplications) {
        final List<String> removedEndorsements = new ArrayList<>();
        final List<String> updatedEndorsements = new ArrayList<>();
        final Map<String, List<String>> endorsementStatusMap = new HashMap<>();

        previousDriverNotified.getCases().forEach(previousCase -> {
            final Cases currentCase = cases.stream()
                    .filter(aCase -> previousCase.getCaseId().equals(aCase.getCaseId())).findFirst().orElse(null);
            previousCase.getDefendantCaseOffences().forEach(previousOffence -> {
                final DefendantCaseOffences currentOffence = nonNull(currentCase) ? currentCase.getDefendantCaseOffences().stream()
                        .filter(aOffence -> previousOffence.getCode().equalsIgnoreCase(aOffence.getCode())).findFirst().orElse(null)
                        : null;
                final EndorsementStatus endorsementStatus = getEndorsementStatus(isNotEmpty(amendmentDate), currentOffence, courtApplications);
                if (REMOVE.equals(endorsementStatus)) {
                    removedEndorsements.add(getDvlaCode(previousOffence));
                    if (Objects.nonNull(currentOffence)) {
                        final DefendantCaseOffences defendantCaseOffence = DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(currentOffence)
                                .withConvictionDate(null)
                                .withConvictingCourtCode(null)
                                .build();
                        currentCase.getDefendantCaseOffences().remove(currentOffence);
                        currentCase.getDefendantCaseOffences().add(defendantCaseOffence);
                    }
                } else {
                    updatedEndorsements.add(getDvlaCode(previousOffence));
                    if (UPDATE_MERGE.equals(endorsementStatus)) {
                        mergeOffences(currentCase, currentOffence, previousOffence, courtApplications, orderDate);
                    }
                }
            });
        });

        endorsementStatusMap.put(REMOVED, removedEndorsements);
        endorsementStatusMap.put(UPDATED, updatedEndorsements);
        return endorsementStatusMap;
    }

    private static void mergeOffences(final Cases currentCase, final DefendantCaseOffences currentOffence, final DefendantCaseOffences previousOffence, final List<CourtApplications> courtApplications, final String orderDate) {
        if (isNull(currentOffence)) {
            currentCase.getDefendantCaseOffences().add(DefendantCaseOffences.defendantCaseOffences()
                    .withValuesFrom(previousOffence)
                    .withDateDisqReimposedFollowingAppeal(getDateDisqReimposedFollowingAppeal(courtApplications, orderDate))
                    .build());
        } else {
            final DefendantCaseOffences mergedOffence = mergeOffence(currentOffence, previousOffence);
            currentCase.getDefendantCaseOffences().remove(currentOffence);
            currentCase.getDefendantCaseOffences().add(mergedOffence);
        }
    }

    private static boolean isD20ShouldBeGenerated(final Cases cases, final String amendmentDate,
                                                  final DriverNotified previousDriverNotified, final List<CourtApplications> courtApplications, boolean hasAnyResultModified) {
        final AtomicBoolean generateD20 = new AtomicBoolean(true);

        if (nonNull(previousDriverNotified) && (isNotEmpty((previousDriverNotified.getCases())))) {
            if (isNotEmpty(courtApplications)) {
                evaluateCourtApplicationStatus(cases, courtApplications, generateD20);
            } else if (isEmpty(amendmentDate) && isEmpty(cases.getDefendantCaseOffences())) {
                generateD20.set(false);
            } else {
                generateD20.set(!isNotEmpty(amendmentDate) || !isNotEmpty(cases.getDefendantCaseOffences()) || hasAnyResultModified);
            }
        } else {
            generateD20.set(!isEmpty(cases.getDefendantCaseOffences()));
        }
        return generateD20.get();
    }

    private static void evaluateCourtApplicationStatus(Cases cases, List<CourtApplications> courtApplications, AtomicBoolean generateD20) {
        if (isEmpty(cases.getDefendantCaseOffences())
                && !hasAppealResult(courtApplications)) {
            generateD20.set(false);
        }
    }

    private static List<String> removeNonEndorsableOffences(final Cases cases) {
        return removeNonEndorsableOffences(cases, null);
    }

    private static List<String> removeNonEndorsableOffences(final Cases cases, final List<AggregateConstants.ResultType> resultTypes) {
        final List<String> removedOffences = new ArrayList<>();
        if (nonNull(cases) && isNotEmpty(cases.getDefendantCaseOffences())) {
            final Iterator<DefendantCaseOffences> caseOffencesIterator = cases.getDefendantCaseOffences().iterator();
            while (caseOffencesIterator.hasNext()) {
                final DefendantCaseOffences offence = caseOffencesIterator.next();
                // remove only if: offence does not have any result provided in resultTypes and do not have D20 endorsement.
                if (!(hasAnyResultType(offence.getResults(), resultTypes)
                        || hasD20Endorsement(offence.getResults()))) {
                    removedOffences.add(offence.getDvlaCode());
                    caseOffencesIterator.remove();
                }
            }
        }
        return removedOffences;
    }

    /**
     * Issue : Due to multiple offences have same offence code, system set the endorsement status as 'UPDATED' instead of REMOVED
     * when one or more offence's D20 is set to false. Defect Link : https://tools.hmcts.net/jira/browse/DD-27020
     * This function checks whether removedDVLAOffences codes included within the endorsementStatusMap.
     * If removed offence code is not present in the removed list but present in updated list then
     * the function removes that code from updated list and adds to the removed list.
     * @param removedDVLAOffences  - the list of non endorseable offences removed from the current case
     * @param endorsementStatusMap - the map contains both the removed/updated dvla offence codes
     */
    private static void updateEndorsementStatusMapIfNeeded(final List<String> removedDVLAOffences,
                                                           final Map<String, List<String>> endorsementStatusMap) {
        // If we do not have removed any offences then no need validate
        if (removedDVLAOffences.isEmpty()) {
            return;
        }

        final List<String> updatedDVLACodes = endorsementStatusMap.get(UPDATED);
        final List<String> removedDVLACodes = endorsementStatusMap.get(REMOVED);
        final List<String> codesToBeAddedToRemoveList = new ArrayList<>();
        removedDVLAOffences.forEach(dvlaCode -> {
            if (!removedDVLACodes.contains(dvlaCode)) {
                codesToBeAddedToRemoveList.add(dvlaCode);
                updatedDVLACodes.remove(dvlaCode);
            }
        });
        if (!codesToBeAddedToRemoveList.isEmpty()) {
            removedDVLACodes.addAll(codesToBeAddedToRemoveList);
        }
    }
}
