package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ALCOHOL_DRUG_MAX_LEVEL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.AOF;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.ACP;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.APPRO;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DATE_DISQUALIFICATION_ENDS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DEFAULT_DVLA_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DVLACODE_FOR_OFFENCE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.REMOVE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.UPDATE_MERGE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.EndorsementStatus.UPDATE_NOMERGE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.NOTIONAL_PENALTY_POINTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.NO_DURATION_VALUE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.PENALTY_POINTS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType;
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
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRI;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DSPA;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DSPAS;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.ERR;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.G;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.REMUB;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.RFSD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SV;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.TEXT;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.TT99;

import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;
import uk.gov.justice.cpp.stagingdvla.event.Results;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

@SuppressWarnings({"squid:S1118", "squid:S1602", "squid:S1612", "squid:S1188", "squid:S3776"})
public class OffenceUtil {
    private static final Logger LOGGER = getLogger(OffenceUtil.class);

    private static final String INACTIVE = "INACTIVE";
    private static final String ST_DEC = "STATUTORY DECLARATION";
    private static final String NEXT_HEARING_GROUP = "NEXT HEARING";
    private static final List<String> APPEAL_RESULTS = asList(
            AACA.id, AACD.id, AASA.id, AASD.id, ACSD.id, APA.id, ASV.id, AW.id, DDRE.id
    );

    private static final List<String> COV_G_RESULTS = asList(COV.id, G.id);

    public static String getConvictingCourtCode(final DefendantCaseOffences currentOffence,
                                                final DefendantCaseOffences previousOffence) {
        if (nonNull(previousOffence) && isNotEmpty(previousOffence.getConvictingCourtCode())) {
            return previousOffence.getConvictingCourtCode();
        } else {
            return currentOffence.getConvictingCourtCode();
        }
    }

    public static EndorsementStatus getEndorsementStatus(final boolean isAmendment,
                                                         final DefendantCaseOffences currentOffence,
                                                         final List<CourtApplications> courtApplications) {
        if (isAmendment) {
            return isNull(currentOffence) ? REMOVE : UPDATE_NOMERGE;
        } else if (hasAppealResult(courtApplications)) {
            return getEndorsementStatus(currentOffence, courtApplications);
        } else if (hasResultType(courtApplications, DSPAS)) {
            return UPDATE_MERGE;
        } else {
            return isNull(currentOffence) ? REMOVE : UPDATE_MERGE;
        }
    }

    private static EndorsementStatus getEndorsementStatus(final DefendantCaseOffences currentOffence, final List<CourtApplications> courtApplications) {
        if (hasAppealAgainstAllowed(courtApplications)) {
            return REMOVE;
        } else if (hasResultType(currentOffence, SV)) {
            if (hasAnyOtherEndorsement(currentOffence, SV)) {
                return UPDATE_NOMERGE;
            } else if (hasAppealResult(courtApplications)) {
                return UPDATE_MERGE;
            } else {
                return REMOVE;
            }
        } else {
            if (nonNull(currentOffence)) {
                return UPDATE_MERGE;
            } else if (hasAppealResult(courtApplications)) {
                return UPDATE_MERGE;
            } else {
                return REMOVE;
            }
        }
    }

    public static String getDvlaCode(DefendantCaseOffences offence) {
        final AtomicReference<String> dvlaCode = new AtomicReference<>(DEFAULT_DVLA_CODE);

        if (nonNull(offence)) {
            if (isNotEmpty(offence.getDvlaCode())) {
                dvlaCode.set(offence.getDvlaCode());
            } else if (isNotEmpty(offence.getResults())) {
                final Prompts prompt = offence.getResults().stream()
                        .map(Results::getPrompts)
                        .filter(CollectionUtils::isNotEmpty)
                        .flatMap(Collection::stream)
                        .filter(p -> isNotEmpty(p.getPromptReference())
                                && isNotEmpty(p.getValue())
                                && DVLACODE_FOR_OFFENCE.equalsIgnoreCase(p.getPromptReference()))
                        .findFirst()
                        .orElse(null);

                if (nonNull(prompt)) {
                    dvlaCode.set(prompt.getValue());
                }
            }
        }
        return dvlaCode.get();
    }

    public static String getAlcoholReadingAmount(DefendantCaseOffences offence) {
        if (isNull(offence) || isNull(offence.getAlcoholReadingAmount())) {
            return null;
        } else if (offence.getAlcoholReadingAmount().length() <= 3) {
            return getNumericValue(offence.getAlcoholReadingAmount(), 3);
        } else {
            return ALCOHOL_DRUG_MAX_LEVEL;
        }
    }

    public static String getFine(DefendantCaseOffences offence) {
        final AtomicReference<String> fine = new AtomicReference<>(EMPTY);

        if (nonNull(offence) && isNotEmpty(offence.getResults())) {
            offence.getResults().stream().filter(result -> isNotEmpty(result.getPrompts()))
                    .forEach(result -> result.getPrompts().forEach(prompt -> {
                        if (AOF.equalsIgnoreCase(prompt.getPromptReference())) {
                            fine.set(prompt.getValue());
                        }
                    }));
        }

        return fine.get();
    }

    public static String getPenaltyPoints(DefendantCaseOffences offence) {
        final AtomicReference<String> penaltyPoints = new AtomicReference<>(EMPTY);

        if (nonNull(offence) && isNotEmpty(offence.getResults())) {
            offence.getResults().stream().filter(result -> isNotEmpty(result.getPrompts()))
                    .forEach(result -> result.getPrompts().forEach(prompt -> {
                        if ((NOTIONAL_PENALTY_POINTS.equalsIgnoreCase(prompt.getPromptReference()) && TT99.equalsIgnoreCase(result.getPointsDisqualificationCode()))
                                || PENALTY_POINTS.equalsIgnoreCase(prompt.getPromptReference())) {
                            penaltyPoints.set(prompt.getValue());
                        }
                    }));
        }

        return penaltyPoints.get();
    }

    private static String getNumericValue(final String value, final int digits) {
        String newValue = isNumeric(value) ? value : EMPTY;

        for (int i = 0; i < (digits - value.length()); i++) {
            newValue = "0".concat(newValue);
        }

        return newValue;
    }

    public static String getOtherSentence(final List<Results> results) {
        final AtomicReference<String> otherSentence = new AtomicReference<>(null);
        final AtomicReference<String> dvlaCode = new AtomicReference<>(null);

        results.stream()
                .filter(result -> isNotEmpty(result.getDvlaCode()))
                .forEach(result -> {
                    dvlaCode.set(result.getDvlaCode());
                    if (isNotEmpty(result.getPrompts())) {
                        result.getPrompts().forEach(prompt -> {
                            if (getDurationSequence(prompt.getDurationSequence()) == 1 && isEmpty(otherSentence.get())) {
                                otherSentence.set(dvlaCode.get().concat(calculateDuration(prompt.getValue())));
                            }
                        });
                    }
                });

        if (isNotEmpty(otherSentence.get())) {
            return otherSentence.get();
        } else if (isNotEmpty(dvlaCode.get())) {
            return dvlaCode.get().concat(NO_DURATION_VALUE);
        } else {
            return null;
        }
    }

    public static String getSuspendedSentence(final List<Results> results) {
        final AtomicReference<String> suspendedSentence = new AtomicReference<>(null);

        results.stream()
                .filter(result -> isNotEmpty(result.getDvlaCode()) && "C".equals(result.getDvlaCode()) && isNotEmpty(result.getPrompts()))
                .forEach(result -> {
                    result.getPrompts().forEach(prompt -> {
                        if (getDurationSequence(prompt.getDurationSequence()) == 2 && isEmpty(suspendedSentence.get())) {
                            suspendedSentence.set(calculateDuration(prompt.getValue()));
                        }
                    });
                });

        return suspendedSentence.get();
    }

    private static int getDurationSequence(BigDecimal durationSequence) {
        if (nonNull(durationSequence)) {
            return durationSequence.intValue();
        } else {
            return -1;
        }
    }

    private static String calculateDuration(final String value) {
        if (isNotEmpty(value) && value.contains(" ")) {
            final String[] parts = value.trim().replaceAll(" +", " ").split(" ");
            if (parts.length > 1) {
                return getNumericValue(parts[0], 2) + (parts[1].toUpperCase().charAt(0));
            }
        }

        return NO_DURATION_VALUE;
    }

    public static String getDttpDtetp(DefendantCaseOffences offence) {
        final AtomicReference<String> dttpDtetp = new AtomicReference<>(EMPTY);

        if (nonNull(offence) && isNotEmpty(offence.getResults())) {
            offence.getResults().forEach(result -> {
                if (nonNull(result.getDrivingTestStipulation())
                        && result.getDrivingTestStipulation() > 0) {
                    dttpDtetp.set(result.getDrivingTestStipulation().toString());
                }
            });
        }

        return dttpDtetp.get();
    }

    public static String getInterimImposedFinalSentence(DefendantCaseOffences offence) {
        if (nonNull(offence) && isNotEmpty(offence.getResults())) {
            if (offence.getResults().stream().anyMatch(result -> DDRI.id.equalsIgnoreCase(result.getResultIdentifier()))) {
                return "1";
            } else if (offence.getResults().stream().anyMatch(result ->
                    isNotEmpty(result.getPrompts()) &&
                            result.getPrompts().stream().anyMatch(prompt -> STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION.equalsIgnoreCase(prompt.getPromptReference())))) {
                return "2";
            }
        }

        return EMPTY;
    }

    public static String getSentencingCourtCode(final DefendantCaseOffences offence, final List<CourtApplications> courtApplications, final DefendantCaseOffences previousOffence, final String amendmentDate, final String orderDate, final String orderingCourtCode) {
        if (isNotEmpty(amendmentDate) && hasResultType(offence, DDRI)) {
            return null;
        } else if (nonNull(previousOffence) && isNotEmpty(previousOffence.getSentencingCourtCode())) {
            return previousOffence.getSentencingCourtCode();
        } else if (nonNull(offence) && isSentenced(offence, courtApplications, orderDate)) {
            return orderingCourtCode;
        } else {
            return null;
        }
    }

    public static String getSentenceDate(final DefendantCaseOffences offence, final List<CourtApplications> courtApplications, final DefendantCaseOffences previousOffence, final String amendmentDate, final String orderDate) {
        if (isNotEmpty(amendmentDate) && hasResultType(offence, DDRI)) {
            return null;
        } else if (nonNull(previousOffence) && isNotEmpty(previousOffence.getSentenceDate())) {
            return previousOffence.getSentenceDate();
        } else if (isSentenced(offence, courtApplications, orderDate)) {
            return orderDate;
        } else {
            return null;
        }
    }

    private static boolean isSentenced(final DefendantCaseOffences offence, final List<CourtApplications> courtApplications, final String orderDate) {
        if (hasAppealResult(courtApplications) || hasResultType(offence, DDRI)) {
            return false;
        } else if (nonNull(offence) && isNotEmpty(orderDate) && isNotEmpty(offence.getConvictionDate())) {
            return !orderDate.equalsIgnoreCase(offence.getConvictionDate());
        } else {
            return false;
        }
    }

    public static String getDateFromWhichDisqRemoved(DefendantCaseOffences offence) {
        final AtomicReference<String> dateDisqualificationEnds = new AtomicReference<>(EMPTY);

        if (nonNull(offence) && isNotEmpty(offence.getResults())) {
            offence.getResults().stream().filter(result -> isNotEmpty(result.getPrompts()))
                    .forEach(result -> result.getPrompts().forEach(prompt -> {
                        if (DATE_DISQUALIFICATION_ENDS.equalsIgnoreCase(prompt.getPromptReference())) {
                            dateDisqualificationEnds.set(prompt.getValue());
                        }
                    }));
        }

        return dateDisqualificationEnds.get();
    }

    public static String getDateDisqSuspendedPendingAppeal(final DefendantCaseOffences offence, final List<CourtApplications> courtApplications, final DefendantCaseOffences previousOffence, final String amendmentDate, final String orderDate) {
        if (hasResultType(offence, DSPA) || hasResultType(offence, DSPAS)) {
            return orderDate;
        } else if (isEmpty(amendmentDate) && isNotEmpty(courtApplications) && nonNull(previousOffence)) {
            return previousOffence.getDateDisqSuspendedPendingAppeal();
        } else {
            return null;
        }
    }

    public static String getDateDisqReimposedFollowingAppeal(final List<CourtApplications> courtApplications, final String orderDate) {
        if (hasResultType(courtApplications, DDRE)) {
            return orderDate;
        } else {
            return null;
        }
    }

    private static boolean hasAppealAgainstAllowed(final List<CourtApplications> courtApplications) {
        return hasResultType(courtApplications, AACA) || hasResultType(courtApplications, AASA);
    }

    public static boolean hasAppealResult(List<CourtApplications> courtApplications) {
        return isNotEmpty(courtApplications)
                && courtApplications.stream()
                .anyMatch(courtApplication -> courtApplication.getResults().stream()
                        .anyMatch(result -> APPEAL_RESULTS.stream().anyMatch(result.getResultIdentifier()::equalsIgnoreCase)));
    }

    public static boolean hasResultType(final DefendantCaseOffences offence, final ResultType resultType) {
        return nonNull(offence) && isNotEmpty(offence.getResults()) &&
                offence.getResults().stream().anyMatch(result -> resultType.id.equalsIgnoreCase(result.getResultIdentifier()));
    }

    public static boolean hasResultType(final List<CourtApplications> courtApplications, final ResultType resultType) {
        return isNotEmpty(courtApplications) &&
                courtApplications.stream().anyMatch(courtApplication -> isNotEmpty(courtApplication.getResults()) &&
                        courtApplication.getResults().stream().anyMatch(result -> resultType.id.equalsIgnoreCase(result.getResultIdentifier())));
    }


    public static boolean hasAnyResultType(final List<Results> results, final List<ResultType> resultTypes) {
        if (isNotEmpty(results) && isNotEmpty(resultTypes)) {
            return results.stream().anyMatch(result -> resultTypes.stream()
                    .anyMatch(resultType -> resultType.id.equalsIgnoreCase(result.getResultIdentifier())));
        } else {
            return false;
        }
    }

    public static boolean hasAnyResultType(final Results result, final List<ResultType> resultTypes) {
        if (nonNull(result) && isNotEmpty(resultTypes)) {
            return resultTypes.stream().anyMatch(resultType -> resultType.id.equalsIgnoreCase(result.getResultIdentifier()));
        } else {
            return false;
        }
    }

    public static boolean hasAnyOtherEndorsement(final DefendantCaseOffences offence, final ResultType resultType) {
        return nonNull(offence) && isNotEmpty(offence.getResults())
                && offence.getResults().stream().anyMatch(result -> !resultType.id.equalsIgnoreCase(result.getResultIdentifier()) && result.getD20());
    }

    public static boolean hasD20Endorsement(final List<Results> results) {
        return isNotEmpty(results) && results.stream()
                .anyMatch(r -> nonNull(r.getD20()) && r.getD20());
    }

    public static boolean hasPointsDisqualificationCode(final List<Results> results) {
        return isNotEmpty(results) && results.stream()
                .anyMatch(r -> isNotEmpty(r.getPointsDisqualificationCode()));
    }

    /**
     * This method indicates whether any endorsed interim offence result was removed after
     * amending. Example scenario:
     *
     * Previous Notification
     * Case Offence Results
     * Adj  - No Endorsement
     * SS30(Interim Disq) - Endorsed and has d20
     *
     * Delete interim disqualification and keep Adj
     * Case Offence Results
     * Adj  - No Endorsement
     *
     * D20 removal should be generated and notification sent to DVLA
     *
     * @param prevCase
     * @param currCase
     * @param courtApplications
     * @return true if there is any previously endorsed offense result removed
     * and false otherwise
     */

    public static boolean hasAnyD20Removed(final Cases prevCase,
                                           final Cases currCase,
                                           final List<CourtApplications> courtApplications) {

        if (check20Removal(prevCase, currCase, courtApplications)) {
            if (isStdecGranted(courtApplications) || isCaseReopen(courtApplications)) {
                LOGGER.info("[Case Id:{}], this result is an STDEC Granted or case reopen", currCase.getCaseId());

                return true;
            } else if( isAdjournmentOrError(currCase, courtApplications)) {
                LOGGER.info("[Case Id:{}], this result is an adjournment", currCase.getCaseId());

                return false;
            } else if (CollectionUtils.isEmpty(currCase.getDefendantCaseOffences()) ) {
                LOGGER.info("[Case Id:{}] CurrCase.getDefendantCaseOffences empty, checking presence of previous d20 endorsements", currCase.getCaseId());

                return prevCase.getDefendantCaseOffences().
                        stream().
                        anyMatch(prevOffence -> hasD20Endorsement(prevOffence.getResults()));
            } else {
                LOGGER.info("[Case Id:{}], searching for D20 removals", currCase.getCaseId());

                return prevCase.getDefendantCaseOffences().
                    stream().
                    anyMatch(prevOffence -> {
                        final DefendantCaseOffences currOffence =
                                currCase.getDefendantCaseOffences().
                                        stream().
                                        filter(offence ->
                                                equalsIgnoreCase(prevOffence.getMainOffenceCode(),
                                                        offence.getMainOffenceCode())).
                                        findFirst().
                                        orElse(null);

                        return isNull(currOffence)
                                ? hasD20Endorsement(prevOffence.getResults())
                                : hasOffenceD20Removed(currOffence, prevOffence);
                    });
            }
        }

        LOGGER.info("[Case Id:{}], D20 removal check not needed", currCase.getCaseId());

        return false;
    }

    private static boolean isCaseReopen(final List<CourtApplications> courtApplications) {
        if(CollectionUtils.isNotEmpty(courtApplications))
            return courtApplications.stream()
                    .anyMatch(ca -> APPRO.appType.equalsIgnoreCase(ca.getApplicationType()) || APPRO.id.equals(ca.getApplicationTypeId()));

        return false;
    }

    private static boolean isAdjournmentOrError(final Cases currCase,
                                         final List<CourtApplications> courtApplications) {
        if (CollectionUtils.isEmpty(currCase.getDefendantCaseOffences())
                && CollectionUtils.isEmpty(courtApplications)) {
            LOGGER.info("[Case Id:{}] Both case defence offence and court applications are empty.", currCase.getCaseId());

            return false;
        }

        final List<Results> results = getResults(currCase, courtApplications);

        if (results.isEmpty()) {
            LOGGER.info("[Case Id:{}] Results from both case defence offence and court applications are empty.", currCase.getCaseId());

            return false;
        }

        final boolean isAdjournment = isResultAnAdjournment(results);

        LOGGER.info("Checked adjournment and found isAdjournment is {}", isAdjournment);

        return  isAdjournment || isError(results);
    }

    /**
     * If any result has a Next hearing prompt then we consider that result as adjournment.
     * So, if the case result contains only adjournment result and its prompt results then
     * don't generate removal D20, since it is just an adjournment.
     *
     * @param results
     * @return boolean indicating whether the result is adjournment result or not
     */
    public static boolean isResultAnAdjournment(final List<Results> results) {
        final Optional<Results> adjournmentResult = getAdjournmentResult(results);

        if (adjournmentResult.isPresent()) {
            final String adjournmentResultId = adjournmentResult.get().getResultIdentifier();
            final Set<String> adjournmentResultPrompts = getAdjournmentResultPrompts(results, adjournmentResultId);

            return results.stream()
                    .map(Results::getResultIdentifier)
                    .allMatch(id -> adjournmentResultId.equals(id)
                            || TEXT.id.equals(id)
                            || REMUB.id.equals(id)
                            || (!adjournmentResultPrompts.isEmpty() && adjournmentResultPrompts.contains(id)));
        }

        return false;
    }

    public boolean isApplicationsRefused(final List<CourtApplications> courtApplications) {
        return CollectionUtils.isNotEmpty(courtApplications) && courtApplications.stream()
                .allMatch(c-> CollectionUtils.isNotEmpty(c.getResults()) &&
                        c.getResults().stream().allMatch(r-> RFSD.id.equals(r.getResultIdentifier())));
    }

    private static Set<String> getAdjournmentResultPrompts(final List<Results> results, final String adjournmentResultId) {
        return results.stream()
                .filter(result -> adjournmentResultId.equals(result.getResultIdentifier()) && isNotEmpty(result.getPrompts()))
                .flatMap(result -> result.getPrompts().stream().map(Prompts::getPromptIdentifier))
                .collect(Collectors.toSet());
    }

    private static Optional<Results> getAdjournmentResult(final List<Results> results) {
        final Optional<Results> parentResult = results.stream()
                .filter(r -> isNotEmpty(r.getPrompts()))
                .filter(result -> results.stream()
                        .anyMatch(nh -> NEXT_HEARING_GROUP.equalsIgnoreCase(nh.getResultDefinitionGroup())
                                && nh.getResultIdentifier().equals(
                                result.getPrompts().stream()
                                        .map(Prompts::getPromptIdentifier)
                                        .filter(Objects::nonNull)
                                        .findFirst().orElse(EMPTY))))
                .findFirst();

        if (parentResult.isPresent()) {
            return parentResult;
        } else {
            return results.stream()
                    .filter(nh -> NEXT_HEARING_GROUP.equalsIgnoreCase(nh.getResultDefinitionGroup()))
                    .findFirst();
        }
    }

    private static boolean isError(final List<Results> results) {
        return results.stream()
                .allMatch(r -> ERR.id.equals(r.getResultIdentifier()));
    }

    private static List<Results> getResults(final Cases currCase,
                                            final List<CourtApplications> courtApplications) {
        List<Results> results = new ArrayList<>();

        results.addAll(getCaseResults(currCase));

        results.addAll(getApplicationResults(courtApplications));

        return results;
    }

    public static List<Results> getCaseResults(final Cases currCase) {
        if (CollectionUtils.isNotEmpty(currCase.getDefendantCaseOffences())) {
            return currCase.getDefendantCaseOffences().stream()
                    .filter(offence -> CollectionUtils.isNotEmpty(offence.getResults()))
                    .flatMap(offence -> offence.getResults().stream())
                    .toList();
        }

        return Collections.emptyList();
    }

    public static List<Results> getApplicationResults(final List<CourtApplications> courtApplications) {
        if (CollectionUtils.isNotEmpty(courtApplications)) {
            return courtApplications.stream()
                    .filter(ca -> CollectionUtils.isNotEmpty(ca.getResults()))
                    .flatMap(ca -> ca.getResults().stream()).toList();
        }

        return Collections.emptyList();
    }

    private static boolean isStdecGranted(final List<CourtApplications> courtApplications) {
        if(CollectionUtils.isNotEmpty(courtApplications))
            return courtApplications.stream()
                    .filter(ca -> CollectionUtils.isNotEmpty(ca.getResults()))
                    .filter(OffenceUtil::isStDec)
                    .anyMatch(ca -> isGranted(ca.getResults()));

        return false;
    }

    private static boolean isGranted(final List<Results> results) {
        return results.stream().map(Results::getResultIdentifier).anyMatch(G.id::equals);
    }

    private static boolean isStDec(CourtApplications courtApplications) {
        return Objects.nonNull(courtApplications.getApplicationType()) &&
                courtApplications.getApplicationType().toUpperCase().contains(ST_DEC);
    }

    private static boolean check20Removal(final Cases prevCase, final Cases currCase, final List<CourtApplications> courtApplications) {
        final boolean ignoreSearch = ignoreD20RemovalSearch(currCase, courtApplications);

        return !ignoreSearch &&
                nonNull(prevCase) &&
                nonNull(prevCase.getDefendantCaseOffences());
    }

    private static boolean ignoreD20RemovalSearch(final Cases currCase, final List<CourtApplications> courtApplications) {
        return isNotEmpty(courtApplications) &&
                courtApplications.stream()
                .anyMatch(courtApplication -> isFinalisedCase(currCase, courtApplication) ||
                                (nonNull(courtApplication.getResults()) &&
                                        (isRefused(courtApplication) ||
                                        isCovGranted(courtApplication))));
    }

    private static boolean isFinalisedCase(final Cases currCase, final CourtApplications courtApplication) {
        return (ACP.id.equals(courtApplication.getApplicationTypeId()) || ACP.appType.equalsIgnoreCase(courtApplication.getApplicationType())) &&
                INACTIVE.equals(currCase.getCaseStatus());
    }

    public static boolean isRefused(final CourtApplications courtApplication) {
        return courtApplication.getResults().stream()
                .anyMatch(result -> RFSD.id.equals(result.getResultIdentifier()));
    }

    private static boolean isCovGranted(final CourtApplications courtApplication) {
        final Set<String> allResultIdentifiers = courtApplication.getResults()
                .stream()
                .map(Results::getResultIdentifier)
                .collect(Collectors.toSet());

        return allResultIdentifiers.containsAll(COV_G_RESULTS);
    }

    public static boolean hasAnyResultOrPromptModified(final Cases prevCase, final Cases currCase) {
        boolean hasResultOrPromptModified = false;

        if (nonNull(currCase) && nonNull(prevCase) && nonNull(prevCase.getDefendantCaseOffences())) {
            hasResultOrPromptModified = prevCase.getDefendantCaseOffences().
                    stream().
                    filter(prevOffence -> prevOffence.getMainOffenceCode().equals(prevOffence.getCode())).// get only original offences
                    anyMatch(prevOffence -> {
                        final DefendantCaseOffences currOffence =
                                currCase.getDefendantCaseOffences().
                                        stream().
                                        filter(offence ->
                                                equalsIgnoreCase(prevOffence.getMainOffenceCode(),
                                                        offence.getMainOffenceCode())).
                                        findFirst().
                                        orElse(null);

                        return hasAnyResultOrPromptChanged(currOffence, prevOffence);
                    });
        }

        return hasResultOrPromptModified;
    }

    private static boolean hasAnyResultOrPromptChanged(final DefendantCaseOffences currOffence,
                                                       final DefendantCaseOffences prevOffence) {
        return compareResultsAndPrompts(currOffence, prevOffence);
    }

    private static boolean hasOffenceD20Removed(final DefendantCaseOffences currOffence,
                                                final DefendantCaseOffences prevOffence) {
        final boolean d20Removed;
        if (isNotEmpty(prevOffence.getResults())) {
            if (CollectionUtils.isEmpty(currOffence.getResults())) {
                d20Removed = hasD20Endorsement(prevOffence.getResults());
            } else {
                final Stream<Results> removedResults =
                        prevOffence.
                                getResults().
                                stream().
                                filter(prevResult -> currOffence.
                                        getResults().
                                        stream().
                                        noneMatch(currResult ->
                                                equalsIgnoreCase(currResult.getResultIdentifier(),
                                                        prevResult.getResultIdentifier())));
                d20Removed = removedResults.anyMatch(
                        removedResult -> nonNull(removedResult.getD20()) &&
                                removedResult.getD20());
            }
        } else {
            d20Removed = false;
        }

        return d20Removed;
    }

    private static List<Results> compareResults(DefendantCaseOffences currOffence, DefendantCaseOffences prevOffence) {
        if (nonNull(currOffence) && nonNull(prevOffence)) {
            return prevOffence.
                    getResults().
                    stream().
                    filter(prevResult -> currOffence.
                            getResults().
                            stream().
                            noneMatch(currResult ->
                                    equalsIgnoreCase(currResult.getResultIdentifier(),
                                            prevResult.getResultIdentifier()))).collect(Collectors.toList());
        }
        return emptyList();
    }

    private static boolean compareResultsAndPrompts(final DefendantCaseOffences currOffence, final DefendantCaseOffences prevOffence) {

        final List<Prompts> promptsList = new ArrayList<>();
        final List<Results> removedResults = compareResults(currOffence, prevOffence);

        if (nonNull(currOffence) && nonNull(prevOffence) && removedResults.isEmpty()) {
            prevOffence.getResults().forEach(prevResult -> currOffence.getResults().forEach(currResult ->
            {
                if (prevResult.getResultIdentifier().equalsIgnoreCase(currResult.getResultIdentifier()) && nonNull(prevResult.getPrompts()) && nonNull(currResult.getPrompts())) {
                    prevResult.getPrompts().stream()
                            .filter(prevPrompt -> currResult.getPrompts().stream()
                                    .noneMatch(currPrompt -> currPrompt.getPromptReference().equalsIgnoreCase(prevPrompt.getPromptReference()) && currPrompt.getValue().equalsIgnoreCase(prevPrompt.getValue())))
                            .forEach(promptsList::add);

                }
            }));
        }
        return !removedResults.isEmpty() || !promptsList.isEmpty();
    }
}