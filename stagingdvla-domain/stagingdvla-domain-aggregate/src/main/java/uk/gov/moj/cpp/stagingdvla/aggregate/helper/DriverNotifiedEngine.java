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
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.NO_RESULT_PREV_ENDORSED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.NO_UPDATE_PREV_ENDORSED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.NO_UPDATE_PREV_NOT_ENDORSED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.OATS_PREV_ENDORSED;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.REMOVE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.SPECIAL_REASON;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.UPDATE_MERGE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.POINTS_DISQUALIFICATION_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ADJ;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.RFSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.DisqualificationPeriodHelper.getDisqualificationPeriod;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.MergeUtil.getDistinctPrompts;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.MergeUtil.mergeOffence;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.LICENCE_PRODUCED_IN_COURT_RESULTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.getAlcoholReadingAmount;
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
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealRefusedResult;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealResult;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasAppealResultOrGranted;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasD20Endorsement;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasPointsDisqualificationCode;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.hasResultType;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isApplicationNotGranted;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isCaseReopen;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isStdecGranted;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.ApplicationTypes;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DistinctPrompts;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.NotificationType;
import uk.gov.justice.cpp.stagingdvla.event.Previous;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
            final String orderDate,
            final CourtCentre orderingCourt,
            final String amendmentDate,
            final Nowdefendant defendant,
            final List<Cases> currentCases,
            final UUID hearingId,
            final List<CourtApplications> courtApplications,
            final Map<String, Map<UUID,DriverNotified>> previousDriverNotifiedByCaseAndHearing,
            final Map<String, List<ApplicationTypes>> sjpCaseToCcReferredApplications,
            final Boolean isReshare) {

        final List<DriverNotified> driverNotifiedList = currentCases
                .stream()
                .map(currentCase -> processCase(previousDriverNotifiedByCase.get(currentCase.getReference()),
                        orderDate,
                        orderingCourt,
                        amendmentDate,
                        defendant,
                        currentCase,
                        hearingId,
                        courtApplications,
                        previousDriverNotifiedByCaseAndHearing.get(currentCase.getReference()),
                        sjpCaseToCcReferredApplications.get(currentCase.getReference()),
                        isReshare
                ))
                .filter(Objects::nonNull)
                .collect(toList());

        LOGGER.info("{} event(s) created for hearing id: {}", driverNotifiedList.size(), hearingId);

        return driverNotifiedList;
    }

    private static DriverNotified processCase(final DriverNotified previousDriverNotified,
                                              final String orderDate,
                                              final CourtCentre orderingCourt,
                                              final String amendmentDate,
                                              final Nowdefendant defendant,
                                              final Cases currentCase,
                                              final UUID hearingId,
                                              final List<CourtApplications> courtApplications,
                                              final Map<UUID,DriverNotified> previousDriverNotifiedByHearing,
                                              final List<ApplicationTypes> sjpCaseToCcReferredApplications,
                                              final Boolean isReshare) {

        LOGGER.info("Processing case: {}", currentCase.getReference());

        if (isStDecApplicationResharedAndNotGrantedResult(courtApplications, isReshare)) {
            return getLatestDriverNotifiedFromPreviousHearing(previousDriverNotified,orderDate,hearingId, currentCase,courtApplications,previousDriverNotifiedByHearing);
        }

        // Get previous case using reference number
        final Cases previousCase = nonNull(previousDriverNotified) ? previousDriverNotified.getCases()
                .stream()
                .filter((aCase -> isNotEmpty(currentCase.getReference()) && currentCase.getReference().equalsIgnoreCase(aCase.getReference())))
                .findFirst()
                .orElse(null) : null;

        LOGGER.info("Previous event found for case: {}", nonNull(previousCase));

        final boolean hasD20Removed = hasAnyD20Removed(previousCase, currentCase, courtApplications, sjpCaseToCcReferredApplications);

        LOGGER.info("Any D20 removed for case: {}", hasD20Removed);

        // Hearing adjourned, previous offences will be not be changed
        if (isEmpty(amendmentDate) && isEmpty(courtApplications)) {
            // if case not amended or not appeal results.
            // check for subsequent hearing
            removeNonMatchingOffencesFromPrevious(currentCase, previousDriverNotified);
        }

        removeNonEndorsableOffences(previousCase, currentCase, courtApplications, asList(SV.id), sjpCaseToCcReferredApplications);

        boolean hasResultOrPromptModified = false;

        if (isNotEmpty(amendmentDate)) {
            hasResultOrPromptModified = hasAnyResultOrPromptModified(previousCase, currentCase);
        }

        if (hasD20Removed ||
                isD20ShouldBeGenerated(currentCase,
                        amendmentDate, previousDriverNotified, courtApplications, hasResultOrPromptModified)) {

            final List<Cases> cases = getUpdatedCases(currentCase);
            final List<String> nonEndorsableOffenceCodes = removeNonEndorsableOffences(previousCase, currentCase, courtApplications, sjpCaseToCcReferredApplications);
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
                boolean sendNotification = setEndorsementsAndNotificationType(builder, amendmentDate, orderDate, orderingCourtCode, previousDriverNotified, cases, courtApplications, nonEndorsableOffenceCodes, sjpCaseToCcReferredApplications);
                if (!sendNotification) {
                    return null;
                }
            } else {
                builder.withNotificationType(NotificationType.NEW);
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

    private static DriverNotified getLatestDriverNotifiedFromPreviousHearing(final DriverNotified previousDriverNotified,
                                                                      final String orderDate,
                                                                      final UUID hearingId,
                                                                      final Cases currentCase,
                                                                      final List<CourtApplications> courtApplications,
                                                                      final Map<UUID, DriverNotified> previousDriverNotifiedByHearing) {
        final DriverNotified latestDriverNotified = previousDriverNotifiedByHearing.values().stream()
                .filter(event -> !event.getOrderingHearingId().equals(hearingId))
                .max(Comparator.comparing(DriverNotified::getOrderDate))
                .orElse(null);
        if (isNull(latestDriverNotified)) {
            return null;
        } else if (NotificationType.REMOVE.equals(latestDriverNotified.getNotificationType()) && NotificationType.REMOVE.equals(previousDriverNotified.getNotificationType())) {
            return null;
        }
        return DriverNotified.driverNotified()
                .withValuesFrom(latestDriverNotified)
                .withOrderingHearingId(hearingId)
                .withNotificationType(latestDriverNotified.getNotificationType().equals(NotificationType.NEW) ? NotificationType.UPDATE : latestDriverNotified.getNotificationType())
                .withNotificationWasPreviouslySent(true)
                .withCaseApplicationReferences(singletonList(currentCase.getReference()))
                .withCourtApplications(courtApplications)
                .withOrderDate(orderDate)
                .withIsResetToPreviousEvent(true)
                .withPrevious(getPrevious(previousDriverNotified))
                .build();
    }

    private static boolean isStDecApplicationResharedAndNotGrantedResult(final List<CourtApplications> courtApplications, final Boolean isReshare) {
        return isApplicationNotGranted(courtApplications) && courtApplications.stream().anyMatch(OffenceUtil::isStDec) &&
                (hasResultType(courtApplications, ADJ) || hasResultType(courtApplications, RFSD))
                && Boolean.TRUE.equals(isReshare);
    }

    private static List<Cases> getUpdatedCases(final Cases cases) {
        final List<Cases> updatedCases = new ArrayList<>();
        updatedCases.add(Cases.cases()
                .withValuesFrom(cases)
                .withHasSV(hasSV(cases))
                .build());
        return updatedCases;
    }

    private static boolean hasSV(final List<Cases> cases) {
        return cases.stream().anyMatch(DriverNotifiedEngine::hasSV);
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
                final DefendantCaseOffences previousOffence = getMatchingOffence(previousCase, currentOffence);
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

    private static DefendantCaseOffences getMatchingOffence(final Cases aCase, final DefendantCaseOffences offenceToMatch) {
        return nonNull(aCase) && isNotEmpty(aCase.getDefendantCaseOffences()) ?
                aCase.getDefendantCaseOffences().stream()
                        .filter(offence -> offenceToMatch.getCode().equalsIgnoreCase(offence.getCode()))
                        .findFirst()
                        .orElse(null)
                : null;
    }

    private static DefendantCaseOffences updateOffence(DefendantCaseOffences currentOffence, final List<CourtApplications> courtApplications, final DefendantCaseOffences previousOffence, final String amendmentDate, final String orderDate, final String orderingCourtCode) {
        final DefendantCaseOffences.Builder builder = DefendantCaseOffences.defendantCaseOffences()
                .withValuesFrom(currentOffence);

        return builder.withDvlaCode(getDvlaCode(currentOffence))
                .withConvictingCourtCode(getConvictingCourtCode(currentOffence, previousOffence))
                .withResults(currentOffence.getResults())
                .withAlcoholReadingAmount(getAlcoholReadingAmount(currentOffence))
                .withFine(getFine(currentOffence.getResults()))
                .withPenaltyPoints(getPenaltyPoints(currentOffence.getResults()))
                .withDisqualificationPeriod(getDisqualificationPeriod(currentOffence.getResults(), orderDate))
                .withOtherSentence(getOtherSentence(currentOffence.getResults()))
                .withSuspendedSentence(getSuspendedSentence(currentOffence.getResults()))
                .withDttpDtetp(getDttpDtetp(currentOffence.getResults()))
                .withInterimImposedFinalSentence(getInterimImposedFinalSentence(currentOffence.getResults()))
                .withSentencingCourtCode(getSentencingCourtCode(currentOffence, previousOffence, amendmentDate, orderDate, orderingCourtCode, courtApplications))
                .withSentenceDate(getSentenceDate(currentOffence, previousOffence, amendmentDate, orderDate,courtApplications))
                .withDateFromWhichDisqRemoved(getDateFromWhichDisqRemoved(currentOffence.getResults()))
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
                                .noneMatch(currentOffence -> equalsIgnoreCase(previousOffence.getMainOffenceCode(), currentOffence.getMainOffenceCode())));
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
                        if (hasAnyResultType(result, LICENCE_PRODUCED_IN_COURT_RESULTS)) {
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

    private static boolean setEndorsementsAndNotificationType(final DriverNotified.Builder builder,
                                                              final String amendmentDate, final String orderDate, final String orderingCourtCode,
                                                              final DriverNotified previousDriverNotified,
                                                              final List<Cases> cases, final List<CourtApplications> courtApplications,
                                                              final List<String> nonEndorsableOffenceCodes,
                                                              final List<ApplicationTypes> sjpCaseToCcReferredApplications) {
        if (hasAppealRefusedResult(courtApplications)
                && !(hasResultType(courtApplications, DDRE) || hasSV(cases))) {
            return false;
        }

        final List<String> removedEndorsements = new ArrayList<>();
        final List<String> updatedEndorsements = new ArrayList<>();
        final List<String> oatsOffences = new ArrayList<>();
        final List<String> noUpdateOffences = new ArrayList<>();
        final List<String> emptyResultOffences = new ArrayList<>();
        final List<String> specialReasonOffences = new ArrayList<>();
        final boolean isCaseHasReopenedApplication = isCaseReopen(courtApplications, sjpCaseToCcReferredApplications);

        previousDriverNotified.getCases().forEach(previousCase -> {
            final Cases currentCase = cases.stream()
                    .filter(aCase -> previousCase.getCaseId().equals(aCase.getCaseId())).findFirst().orElse(null);
            previousCase.getDefendantCaseOffences().forEach(previousOffence -> {
                final DefendantCaseOffences currentOffence = getMatchingOffence(currentCase, previousOffence);
                final EndorsementStatus endorsementStatus = getEndorsementStatus(isNotEmpty(amendmentDate),
                        currentOffence, previousOffence, courtApplications, nonEndorsableOffenceCodes, sjpCaseToCcReferredApplications);
                final String dvlaCode = getDvlaCode(previousOffence);

                switch (endorsementStatus) {
                    case REMOVE -> removedEndorsements.add(dvlaCode);
                    case UPDATE_MERGE, UPDATE_NOMERGE -> updatedEndorsements.add(dvlaCode);
                    case OATS_PREV_ENDORSED -> oatsOffences.add(dvlaCode);
                    case NO_UPDATE_PREV_ENDORSED -> noUpdateOffences.add(dvlaCode);
                    case NO_RESULT_PREV_ENDORSED -> emptyResultOffences.add(dvlaCode);
                    case SPECIAL_REASON -> specialReasonOffences.add(dvlaCode);
                    default -> {
                        // Do nothing for all other cases
                    }
                }


                if (REMOVE.equals(endorsementStatus)) {
                    if (hasAppealResultOrGranted(courtApplications) || isCaseHasReopenedApplication) {
                        removeOffence(currentOffence, currentCase);
                    } else {
                        removeConvictionDataFromOffence(currentOffence, currentCase);
                    }
                } else {
                    if (UPDATE_MERGE.equals(endorsementStatus) || OATS_PREV_ENDORSED.equals(endorsementStatus)
                            || NO_UPDATE_PREV_ENDORSED.equals(endorsementStatus) || NO_RESULT_PREV_ENDORSED.equals(endorsementStatus)) {
                        mergeOffences(currentCase, currentOffence, previousOffence, courtApplications, orderDate, orderingCourtCode, hasAppealResultOrGranted(courtApplications), isCaseHasReopenedApplication, isStdecGranted(courtApplications));
                    } else if (SPECIAL_REASON.equals(endorsementStatus) || NO_UPDATE_PREV_NOT_ENDORSED.equals(endorsementStatus)) {
                        removeOffence(currentOffence, currentCase);
                    }
                }
            });
        });

        if (hasAppealResultOrGranted(courtApplications) || isCaseHasReopenedApplication) {
            updatedEndorsements.addAll(checkOffencesThatDoesNotExistInPrevious(cases, previousDriverNotified));
        }

        if (isNotEmpty(removedEndorsements) || isNotEmpty(updatedEndorsements) || isNotEmpty(specialReasonOffences)) {
            updatedEndorsements.addAll(noUpdateOffences);
            updatedEndorsements.addAll(emptyResultOffences);
            removedEndorsements.addAll(specialReasonOffences);
            assignEndorsements(builder, courtApplications, removedEndorsements, updatedEndorsements, oatsOffences, sjpCaseToCcReferredApplications);
            return true;
        }
        return false;
    }

    private static void removeOffence(final DefendantCaseOffences currentOffence, final Cases currentCase) {
        if (nonNull(currentCase) && nonNull(currentOffence)) {
            currentCase.getDefendantCaseOffences().remove(currentOffence);
        }
    }

    private static void removeConvictionDataFromOffence(final DefendantCaseOffences currentOffence, final Cases currentCase) {
        if (nonNull(currentCase) && nonNull(currentOffence)) {
            final DefendantCaseOffences defendantCaseOffence = DefendantCaseOffences.defendantCaseOffences()
                    .withValuesFrom(currentOffence)
                    .withConvictionDate(null)
                    .withConvictingCourtCode(null)
                    .build();
            currentCase.getDefendantCaseOffences().remove(currentOffence);
            currentCase.getDefendantCaseOffences().add(defendantCaseOffence);
        }
    }

    private static List<String> checkOffencesThatDoesNotExistInPrevious(final List<Cases> cases, final DriverNotified previousDriverNotified) {
        final List<String> newEndorsements = new ArrayList<>();

        cases.stream()
                .filter(aCase -> isNotEmpty(aCase.getDefendantCaseOffences()))
                .forEach(aCase -> {
                    List<DefendantCaseOffences> offencesToProcess = aCase.getDefendantCaseOffences().stream()
                            .filter(offence -> isNotEmpty(offence.getCode()) && isNotEmpty(offence.getDvlaCode()) && !POINTS_DISQUALIFICATION_CODE.equals(offence.getDvlaCode()))
                            .filter(offence -> isNull(previousDriverNotified)
                                    || isEmpty(previousDriverNotified.getCases())
                                    || previousDriverNotified.getCases().stream()
                                    .filter(prevCase -> isNotEmpty(prevCase.getDefendantCaseOffences()))
                                    .flatMap(prevCase -> prevCase.getDefendantCaseOffences().stream())
                                    .noneMatch(prevOffence -> offence.getCode().equalsIgnoreCase(prevOffence.getCode())))
                            .toList();

                    if (isNotEmpty(offencesToProcess)) {
                        offencesToProcess.forEach(offence -> {
                            if (hasD20Endorsement(offence)) {
                                newEndorsements.add(offence.getDvlaCode());
                            } else {
                                aCase.getDefendantCaseOffences().remove(offence);
                            }
                        });
                    }
                });

        return newEndorsements;
    }

    private static void assignEndorsements(final DriverNotified.Builder builder, final List<CourtApplications> courtApplications, final List<String> removedEndorsements, final List<String> updatedEndorsements, final List<String> oatsOffences, final List<ApplicationTypes> sjpCaseToCcReferredApplications) {
        if (isNotEmpty(removedEndorsements)) {
            builder.withRemovedEndorsements(removedEndorsements);
        }

        if (isNotEmpty(updatedEndorsements)) {
            builder.withUpdatedEndorsements(updatedEndorsements);
        }

        if (isNotEmpty(oatsOffences)) {
            builder.withOatsEndorsements(oatsOffences);
        }

        if (hasAppealResultOrGranted(courtApplications) || isCaseReopen(courtApplications, sjpCaseToCcReferredApplications) || isStdecGranted(courtApplications)) {
            builder.withNotificationType(isNotEmpty(updatedEndorsements) || isNotEmpty(oatsOffences)
                    ? NotificationType.UPDATE : NotificationType.REMOVE);

        } else {
            builder.withNotificationType(isNotEmpty(removedEndorsements)
                    ? NotificationType.REMOVE : NotificationType.UPDATE);
        }
    }

    private static void mergeOffences(final Cases currentCase, final DefendantCaseOffences currentOffence,
                                      final DefendantCaseOffences previousOffence, final List<CourtApplications> courtApplications,
                                      final String orderDate, final String orderingCourtCode, final boolean hasAppealResultOrGranted,
                                      final boolean isCaseReopened, final boolean isStatDec) {
        if (isNull(currentOffence)) {
            currentCase.getDefendantCaseOffences().add(DefendantCaseOffences.defendantCaseOffences()
                    .withValuesFrom(previousOffence)
                    .withDateDisqReimposedFollowingAppeal(getDateDisqReimposedFollowingAppeal(courtApplications, orderDate))
                    .build());
        } else if (nonNull(previousOffence)) {
            final DefendantCaseOffences mergedOffence = mergeOffence(currentOffence, previousOffence, orderDate, orderingCourtCode, hasAppealResultOrGranted, isCaseReopened, isStatDec);
            currentCase.getDefendantCaseOffences().remove(currentOffence);
            currentCase.getDefendantCaseOffences().add(mergedOffence);
        }
    }

    private static boolean isD20ShouldBeGenerated(final Cases cases, final String amendmentDate,
                                                  final DriverNotified previousDriverNotified, final List<CourtApplications> courtApplications, boolean hasAnyResultModified) {
        final AtomicBoolean generateD20 = new AtomicBoolean(true);

        if (nonNull(previousDriverNotified) && (isNotEmpty((previousDriverNotified.getCases())))) {
            if (isNotEmpty(courtApplications)) {
                evaluateCourtApplicationStatus(cases, courtApplications, generateD20, hasAnyResultModified);
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

    private static void evaluateCourtApplicationStatus(Cases cases, List<CourtApplications> courtApplications, AtomicBoolean generateD20, boolean hasAnyResultModified) {
        if (isEmpty(cases.getDefendantCaseOffences())
                && !hasAppealResult(courtApplications)
                && !hasAnyResultModified) {
            generateD20.set(false);
        }
    }

    private static List<String> removeNonEndorsableOffences(final Cases previousCase, final Cases currentCase, final List<CourtApplications> courtApplications, final List<ApplicationTypes> sjpCaseToCcReferredApplications) {
        return removeNonEndorsableOffences(previousCase, currentCase, courtApplications, null, sjpCaseToCcReferredApplications);
    }

    private static List<String> removeNonEndorsableOffences(final Cases previousCase, final Cases currentCase, final List<CourtApplications> courtApplications, final List<String> resultTypes, final List<ApplicationTypes> sjpCaseToCcReferredApplications) {
        final List<String> removedOffences = new ArrayList<>();
        if (nonNull(currentCase) && isNotEmpty(currentCase.getDefendantCaseOffences())) {
            final Iterator<DefendantCaseOffences> caseOffencesIterator = currentCase.getDefendantCaseOffences().iterator();
            while (caseOffencesIterator.hasNext()) {
                final DefendantCaseOffences offence = caseOffencesIterator.next();
                // remove only if: offence does not have any result provided in resultTypes and do not have D20 endorsement.
                if (!(nonNull(previousCase) && (hasAppealResultOrGranted(courtApplications) || isCaseReopen(courtApplications, sjpCaseToCcReferredApplications)))
                        && !(hasAnyResultType(offence.getResults(), resultTypes) || hasD20Endorsement(offence))) {
                    removedOffences.add(offence.getDvlaCode());
                    caseOffencesIterator.remove();
                }
            }
        }
        return removedOffences;
    }
}
