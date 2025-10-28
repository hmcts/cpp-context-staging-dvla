package uk.gov.moj.cpp.stagingdvla.notify.util;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isAlpha;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DEFAULT_DVLA_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DEFENDANT_DRIVING_LICENCE_NUMBER;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.LICENCE_ISSUE_NUMBER;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.NO_DURATION_VALUE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.PERIOD_LIFETIME;

import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DistinctPrompts;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.Address;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.Conviction;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.Disqualification;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConviction;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.Offence;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.OtherSentence;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.PrisonSentenceSuspended;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S3776"})
public class DrivingConvictionTransformUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrivingConvictionTransformUtil.class.getName());

    public static final int MAX_LENGTH_DEFENDANT_TITLE = 58;
    public static final int MAX_LENGTH_DEFENDANT_FIRST_NAME = 38;
    public static final int MAX_LENGTH_DEFENDANT_LAST_NAME = 43;
    public static final int MAX_LENGTH_ADDRESS_LINE = 30;
    public static final int MAX_LENGTH_POST_CODE = 8;

    public static final String NONE_OR_UNKNOWN = "0: None or unknown";

    public static final DateTimeFormatter cpDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter dvlaDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    protected static final List<String> alcoholMethodCodes = Arrays.asList("A", "B", "U");
    public static final String ALCOHOL = "Alcohol";

    public static final Pattern VALID_SINGLE_CHAR_SENTENCE_MIXED_PERIOD_PATTERN = Pattern.compile(
            "^[APCE](?:(?:0?[1-9]\\d?Y|0?[1-9]\\d?M|0?[1-9]\\d?W|0?[1-9]\\d?D|0?[1-9]\\d?H)){1,2}$"
    );

    //any string that starts with A, P, C, or E and can be followed by any characters or be empty.
    public static final String SINGLE_CHARACTER_SENTENCE_TYPE_REGEX = "^[APCE].*";

    public static final Pattern VALID_MULTIPLE_CHAR_SENTENCE_MIXED_PERIOD_PATTERN = Pattern.compile(
            "^(?:[JM]000(?:[APCE](?:0?[1-9]\\d?[YMWDH])){0,2}|[APCE](?:0?[1-9]\\d?[YMWDH]){1,2})$"
    );

    public static final String MULTIPLE_CHARACTER_SENTENCE_TYPE_REGEX = "^(J000|M000).*";

    public static final String NO_SEPARATE_PENALTY_REGEX = "^I000$";

    public static NotifyDrivingConviction transformToNotifyDrivingConviction(final DriverNotified driverNotified) {
        LOGGER.info("transforming to NotifyDrivingConviction from DriverNotified with identifier: {}",
                driverNotified.getIdentifier());

        final NotifyDrivingConviction notifyDrivingConviction = NotifyDrivingConviction.notifyDrivingConviction()
                .withConviction(getConviction(driverNotified))
                .withOffences(getOffences(driverNotified))
                .build();

        LOGGER.info("transformed to NotifyDrivingConviction from DriverNotified with identifier: {}",
                notifyDrivingConviction.getConviction().getIdentifier());

        return notifyDrivingConviction;
    }

    public static EndorsementType getEndorsementType(final DriverNotified driverNotified) {
        if (isNotEmpty(driverNotified.getRemovedEndorsements())) {
            return EndorsementType.REMOVE;
        } else if (isNotEmpty(driverNotified.getUpdatedEndorsements())) {
            return EndorsementType.UPDATE;
        }
        return EndorsementType.NEW;
    }

    private static Conviction getConviction(final DriverNotified driverNotified) {
        return Conviction.conviction()
                .withIdentifier(nonNull(driverNotified.getIdentifier()) ?
                        driverNotified.getIdentifier().toString() : randomUUID().toString())
                .withConvictingCourtCode(getConvictingCourtCode(driverNotified))
                .withConvictionDate(getConvictionDate(driverNotified))
                .withDrivingLicenceNumber(getDrivingLicenceNumber(driverNotified))
                .withLicenceProducedInCourt(getLicenceProducedInCourt(driverNotified))
                .withLicenceIssueNumber(getLicenceIssueNumber(driverNotified))
                .withGender(getGender(driverNotified))
                .withDateOfBirth(getDateOfBirth(driverNotified))
                .withTitle(getTitle(driverNotified))
                .withLastName(getLastName(driverNotified))
                .withFirstNames(getFirstNames(driverNotified.getDefendant()))
                .withAddress(getAddress(driverNotified))
                .build();
    }

    private static String getConvictingCourtCode(final DriverNotified driverNotified) {
        final DefendantCaseOffences selectedOffence = driverNotified.getCases().stream()
                .filter(aCase -> isNotEmpty(aCase.getDefendantCaseOffences()))
                .flatMap(aCase -> aCase.getDefendantCaseOffences().stream())
                .collect(Collectors.toList()).stream()
                .filter(offence -> isNotEmpty(offence.getConvictingCourtCode()))
                .findFirst().orElse(null);

        if (nonNull(selectedOffence)) {
            return selectedOffence.getConvictingCourtCode();
        }
        return null;
    }

    public static boolean hasMultipleConvictingCourts(final DriverNotified driverNotified) {
        String firstValue = EMPTY;
        for (final Cases aCase : driverNotified.getCases()) {
            for (final DefendantCaseOffences offence : aCase.getDefendantCaseOffences()) {
                if (isNotEmpty(offence.getConvictingCourtCode())
                        && isEmpty(firstValue)) {
                    firstValue = offence.getConvictingCourtCode();
                }
                if (isNotEmpty(offence.getConvictingCourtCode())
                        && !firstValue.equalsIgnoreCase(offence.getConvictingCourtCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getConvictionDate(final DriverNotified driverNotified) {
        final DefendantCaseOffences selectedOffence = driverNotified.getCases().stream()
                .filter(aCase -> isNotEmpty(aCase.getDefendantCaseOffences()))
                .flatMap(aCase -> aCase.getDefendantCaseOffences().stream())
                .collect(Collectors.toList()).stream()
                .filter(offence -> isNotEmpty(offence.getConvictionDate()))
                .findFirst().orElse(null);

        if (nonNull(selectedOffence)) {
            return formatDate(selectedOffence.getConvictionDate());
        }
        return null;
    }

    public static boolean hasMultipleConvictionDates(final DriverNotified driverNotified) {
        String firstValue = EMPTY;
        for (final Cases aCase : driverNotified.getCases()) {
            for (final DefendantCaseOffences offence : aCase.getDefendantCaseOffences()) {
                if (isNotEmpty(offence.getConvictionDate())
                        && isEmpty(firstValue)) {
                    firstValue = offence.getConvictionDate();
                }
                if (isNotEmpty(offence.getConvictionDate())
                        && !firstValue.equalsIgnoreCase(offence.getConvictionDate())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getDrivingLicenceNumber(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDistinctPrompts())) {
            final DistinctPrompts prompt = driverNotified.getDistinctPrompts().stream()
                    .filter(p -> DEFENDANT_DRIVING_LICENCE_NUMBER.equalsIgnoreCase(p.getPromptReference()))
                    .findFirst().orElse(null);
            return nonNull(prompt) ? prompt.getValue() : null;
        }
        return null;
    }

    private static String getLicenceIssueNumber(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDistinctPrompts())) {
            final DistinctPrompts prompt = driverNotified.getDistinctPrompts().stream()
                    .filter(o -> LICENCE_ISSUE_NUMBER.equalsIgnoreCase(o.getPromptReference()))
                    .findFirst().orElse(null);
            return nonNull(prompt) ? prompt.getValue() : null;
        }
        return null;
    }

    private static String getLicenceProducedInCourt(final DriverNotified driverNotified) {
        if (isNotEmpty(driverNotified.getLicenceProducedInCourt())) {
            return driverNotified.getLicenceProducedInCourt();
        }
        return NONE_OR_UNKNOWN;
    }

    private static String getGender(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDefendant())
                && isNotEmpty(driverNotified.getDefendant().getGender())
                && Arrays.stream(Gender.values()).anyMatch(gender -> gender.genderName.equals(driverNotified.getDefendant().getGender()))) {
            return driverNotified.getDefendant().getGender();
        }
        return null;
    }

    private static String getDateOfBirth(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDefendant())) {
            final String dateOfBirth = formatDate(driverNotified.getDefendant().getDateOfBirth());
            return isNotEmpty(dateOfBirth) ? dateOfBirth : EMPTY;
        }
        return EMPTY;
    }

    private static String getTitle(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDefendant())) {
            return truncateTo(driverNotified.getDefendant().getTitle(), MAX_LENGTH_DEFENDANT_TITLE);
        }
        return null;
    }

    private static String getLastName(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDefendant())) {
            return truncateTo(driverNotified.getDefendant().getLastName(), MAX_LENGTH_DEFENDANT_LAST_NAME);
        }
        return null;
    }

    private static String getFirstNames(final Nowdefendant defendant) {
        if (nonNull(defendant) && isNotEmpty(defendant.getFirstName())) {
            final String firstNames = defendant.getFirstName()
                    .concat(isNotEmpty(defendant.getMiddleName()) ?
                            " ".concat(defendant.getMiddleName()) : EMPTY);
            return truncateTo(firstNames, MAX_LENGTH_DEFENDANT_FIRST_NAME);
        }
        return null;
    }

    private static Address getAddress(final DriverNotified driverNotified) {
        if (nonNull(driverNotified.getDefendant()) && nonNull(driverNotified.getDefendant().getAddress())) {
            final Nowaddress nowaddress = driverNotified.getDefendant().getAddress();
            final String line1 = truncateTo(nowaddress.getLine1(), MAX_LENGTH_ADDRESS_LINE);
            return Address.address()
                    .withLine1(nonNull(line1) ? line1 : EMPTY)
                    .withLine2(truncateTo(nowaddress.getLine2(), MAX_LENGTH_ADDRESS_LINE))
                    .withLine3(truncateTo(nowaddress.getLine3(), MAX_LENGTH_ADDRESS_LINE))
                    .withLine4(getAddressLine4(nowaddress))
                    .withPostcode(truncateTo(nowaddress.getPostCode(), MAX_LENGTH_POST_CODE))
                    .build();
        }
        return null;
    }

    private static String getAddressLine4(final Nowaddress nowaddress) {
        final StringBuilder builder = new StringBuilder(EMPTY);
        if (isNotEmpty(nowaddress.getLine4()) && isNotEmpty(nowaddress.getLine5())) {
            builder.append(nowaddress.getLine4()).append(" ").append(nowaddress.getLine5());
        } else if (isNotEmpty(nowaddress.getLine4())) {
            builder.append(nowaddress.getLine4());
        } else if (isNotEmpty(nowaddress.getLine5())) {
            builder.append(nowaddress.getLine5());
        }
        return truncateTo(builder.toString(), MAX_LENGTH_ADDRESS_LINE);
    }

    private static List<Offence> getOffences(final DriverNotified driverNotified) {
        final List<Offence> offences = new ArrayList<>();

        driverNotified.getCases().forEach(aCase -> aCase.getDefendantCaseOffences()
                .forEach(offence -> offences.add(Offence.offence()
                                .withOffenceCode(isNotEmpty(offence.getDvlaCode()) ? offence.getDvlaCode() : DEFAULT_DVLA_CODE)
                                .withDateOfOffence(getDateOfOffence(offence))
                                .withFine(getFine(offence))
                                .withPenaltyPoints(getPenaltyPoints(offence))
                                .withIntoxicantType(getIntoxicantType(offence))
                                .withTestingMethod(getTestingMethod(offence))
                                .withTestingResultLevel(getTestingResultLevel(offence))
                                .withDisqualification(getDisqualification(offence))
                                .withOtherSentence(getOtherSentence(offence))
                                .withPrisonSentenceSuspended(getPrisonSentenceSuspended(offence))
                                .withSentenceState(getSentenceState(offence))
                                .withSentencingCourtCode(getSentenceCourtCode(offence))
                                .withSentenceDate(formatDate(offence.getSentenceDate()))
                                .withDisqualificationRemovalDate(formatDate(offence.getDateFromWhichDisqRemoved()))
                                .withDisqualificationSuspendedPendingAppealDate(formatDate(offence.getDateDisqSuspendedPendingAppeal()))
                                .build()
                        )
                )
        );
        return offences;
    }

    private static String getDateOfOffence(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getEndDate())) {
            return formatDate(offence.getEndDate());
        } else if (isNotEmpty(offence.getStartDate())) {
            return formatDate(offence.getStartDate());
        }
        return null;
    }

    private static Integer getFine(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getFine())) {
            final String fine = offence.getFine().replaceAll("[^0-9]+", "");
            if (isNumeric(fine)) {
                return Integer.valueOf(fine);
            }
        }
        return null;
    }

    private static Integer getPenaltyPoints(final DefendantCaseOffences offence) {
        if (isNumeric(offence.getPenaltyPoints())) {
            return Integer.valueOf(offence.getPenaltyPoints());
        }
        return null;
    }

    private static String getIntoxicantType(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getAlcoholReadingMethodCode()) &&
                alcoholMethodCodes.contains(offence.getAlcoholReadingMethodCode().toUpperCase(Locale.ROOT))) {
            return ALCOHOL;
        } else if (isNotEmpty(offence.getAlcoholReadingMethodDescription())) {
            return offence.getAlcoholReadingMethodDescription();
        }
        return null;
    }

    private static String getTestingMethod(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getAlcoholReadingMethodCode())
                && alcoholMethodCodes.contains(offence.getAlcoholReadingMethodCode().toUpperCase(Locale.ROOT))
                && isNotEmpty(offence.getAlcoholReadingMethodDescription())) {
            return offence.getAlcoholReadingMethodDescription();
        }
        return null;
    }

    private static BigDecimal getTestingResultLevel(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getAlcoholReadingAmount())) {
            return new BigDecimal(offence.getAlcoholReadingAmount());
        }
        return null;
    }

    private static Disqualification getDisqualification(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getDisqualificationPeriod()) || isNotEmpty(offence.getDttpDtetp())) {
            return Disqualification.disqualification()
                    .withExtendedType(getExtendedType(offence))
                    .withForLife(PERIOD_LIFETIME.equalsIgnoreCase(offence.getDisqualificationPeriod()))
                    .withYears(getDisqualificationPeriod(offence, PeriodType.YEAR))
                    .withMonths(getDisqualificationPeriod(offence, PeriodType.MONTH))
                    .withDays(getDisqualificationPeriod(offence, PeriodType.DAY))
                    .build();
        }
        return null;
    }

    private static String getExtendedType(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getDttpDtetp())) {
            final ExtendedType extendedType = ExtendedType.findByCpValue(offence.getDttpDtetp());
            return nonNull(extendedType) ? extendedType.dvlaValue : null;
        }
        return null;
    }

    @SuppressWarnings("squid:S2629")
    private static final Integer getDisqualificationPeriod(final DefendantCaseOffences offence, final PeriodType periodType) {
        if (isNotEmpty(offence.getDisqualificationPeriod()) && !PERIOD_LIFETIME.equalsIgnoreCase(offence.getDisqualificationPeriod())) {
            if (offence.getDisqualificationPeriod().length() == 6) {
                final String period = offence.getDisqualificationPeriod().substring(periodType.startIndex, periodType.endIndex);
                if (isNumeric(period)) {
                    return Integer.valueOf(period);
                }
            } else if (offence.getDisqualificationPeriod().length() > 6) { // SNI-1477 Temporary solution, till we have proper CCT implemented to fix DVLA to accept more than 2 digit
                throw new IllegalStateException("Cannot have offence disqualification period more than 2 digit, as DVLA API cannot accept more than 2 digits");
            }
        }
        return null;
    }

    public static String getHighestMixedDurationPeriod(String mixedPeriod) {
        String sentenceType = mixedPeriod.substring(0, 1);
        String givenMixedPeriod = mixedPeriod.substring(1);

        int yearIndex = givenMixedPeriod.indexOf("Y");
        int monthIndex = givenMixedPeriod.indexOf("M");
        int weekIndex = givenMixedPeriod.indexOf("W");
        int dayIndex = givenMixedPeriod.indexOf("D");
        int hoursIndex = givenMixedPeriod.indexOf("H");

        int lowestIndex = Integer.MAX_VALUE;
        String highestPeriod = null;

        if (yearIndex != -1) {
            lowestIndex = yearIndex;
            highestPeriod = "Y";
        }
        if (monthIndex != -1 && monthIndex < lowestIndex) {
            lowestIndex = monthIndex;
            highestPeriod = "M";
        }
        if (weekIndex != -1 && weekIndex < lowestIndex) {
            lowestIndex = weekIndex;
            highestPeriod = "W";
        }
        if (dayIndex != -1 && dayIndex < lowestIndex) {
            lowestIndex = dayIndex;
            highestPeriod = "D";
        }
        if (hoursIndex != -1 && hoursIndex < lowestIndex) {
            lowestIndex = hoursIndex;
            highestPeriod = "H";
        }

        if (highestPeriod == null) {
            return null;
        }

        int startIndex = 0;
        String numericValue = givenMixedPeriod.substring(startIndex, lowestIndex);

        if (numericValue.equals("0")) {
            return null;
        } else {
            String value = sentenceType + numericValue + highestPeriod;
            if (value.length() == 3) {
                String otherSentenceType = value.substring(0, 1);
                String otherSentencePeriodNumeric = "0" + value.charAt(1);
                String otherSentencePeriodType = value.substring(2, 3);
                value = otherSentenceType + otherSentencePeriodNumeric + otherSentencePeriodType;
                return value;
            }
            return value;
        }
    }

    private static OtherSentence mapOtherSentence(String highestPeriod) {

        final OtherSentenceType otherSentenceType = OtherSentenceType.findByType(highestPeriod.substring(0, 1));

        OtherSentence.Builder otherSentenceBuilder = OtherSentence.otherSentence();

        if (Objects.nonNull(otherSentenceType)) {
            if (Objects.equals(otherSentenceType.value, OtherSentenceType.I.value)) {
                otherSentenceBuilder.withOtherSentenceType(otherSentenceType.value);
                return otherSentenceBuilder.build();
            }

            otherSentenceBuilder.withOtherSentenceType(otherSentenceType.value);
            otherSentenceBuilder.withYears(getOtherSentencePeriod(otherSentenceType, OtherSentencePeriodType.YEAR.name(), highestPeriod));
            otherSentenceBuilder.withMonths(getOtherSentencePeriod(otherSentenceType, OtherSentencePeriodType.MONTH.name(), highestPeriod));
            otherSentenceBuilder.withWeeks(getOtherSentencePeriod(otherSentenceType, OtherSentencePeriodType.WEEKS.name(), highestPeriod));
            otherSentenceBuilder.withDays(getOtherSentencePeriod(otherSentenceType, OtherSentencePeriodType.DAY.name(), highestPeriod));
            otherSentenceBuilder.withHours(getOtherSentencePeriod(otherSentenceType, OtherSentencePeriodType.HOUR.name(), highestPeriod));

            return otherSentenceBuilder.build();
        }
        return null;
    }

    private static OtherSentence getOtherSentence(final DefendantCaseOffences offence) {

        OtherSentence otherSentence = null;
        if (isNotEmpty(offence.getOtherSentence())) {
            Pattern regularSentenceTypePattern = Pattern.compile(SINGLE_CHARACTER_SENTENCE_TYPE_REGEX);
            Pattern specialCharacterSentenceTypePattern = Pattern.compile(MULTIPLE_CHARACTER_SENTENCE_TYPE_REGEX);
            Pattern noSpecialPenaltySentenceTypeMatch = Pattern.compile(NO_SEPARATE_PENALTY_REGEX);

            Matcher matchRegularSentenceType = regularSentenceTypePattern.matcher(offence.getOtherSentence());
            Matcher matchSpecialCharacterSentenceType = specialCharacterSentenceTypePattern.matcher(offence.getOtherSentence());
            Matcher matchNoSpecialPenaltySentenceType = noSpecialPenaltySentenceTypeMatch.matcher(offence.getOtherSentence());


            if (matchRegularSentenceType.matches()) {
                boolean isSingleCharacterPatternMatch = VALID_SINGLE_CHAR_SENTENCE_MIXED_PERIOD_PATTERN.matcher(offence.getOtherSentence()).matches();
                if (isSingleCharacterPatternMatch) {
                    String highestPeriod = getHighestMixedDurationPeriod(offence.getOtherSentence());
                    if (Objects.nonNull(highestPeriod)) {
                        otherSentence = mapOtherSentence(highestPeriod);
                    }
                }
            }

            if (matchSpecialCharacterSentenceType.matches()) {
                boolean isMultipleCharacterPatternMatch = VALID_MULTIPLE_CHAR_SENTENCE_MIXED_PERIOD_PATTERN.matcher(offence.getOtherSentence()).matches();
                if (isMultipleCharacterPatternMatch) {
                    String res  = offence.getOtherSentence().startsWith("J") ? OtherSentenceType.J.value
                            : offence.getOtherSentence().startsWith("M") ? OtherSentenceType.M.value : "";
                    otherSentence = OtherSentence.otherSentence().withOtherSentenceType(res).build();
                }
            }

            if (matchNoSpecialPenaltySentenceType.matches()) {
                otherSentence = mapOtherSentence(offence.getOtherSentence());
            }
        }
        return otherSentence;
    }

    private static Integer getOtherSentencePeriod(final OtherSentenceType otherSentenceType,
                                                  final String periodType, final String otherSentence) {

        if (otherSentenceType.isACEP
                && otherSentence.length() == 4
                && isNumeric(otherSentence.substring(1, 3))
                && isAlpha(otherSentence.substring(3))) {

            return getPeriodValue(periodType, otherSentence.substring(1, 4));
        }
        return null;
    }

    private static PrisonSentenceSuspended getPrisonSentenceSuspended(final DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getSuspendedSentence())
                && !NO_DURATION_VALUE.equalsIgnoreCase(offence.getSuspendedSentence())
                && offence.getSuspendedSentence().length() == 3
                && isNumeric(offence.getSuspendedSentence().substring(0, 2))
                && isAlpha(offence.getSuspendedSentence().substring(2))) {
            return PrisonSentenceSuspended.prisonSentenceSuspended()
                    .withYears(getPeriodValue(OtherSentencePeriodType.YEAR.name(), offence.getSuspendedSentence()))
                    .withMonths(getPeriodValue(OtherSentencePeriodType.MONTH.name(), offence.getSuspendedSentence()))
                    .withDays(getPeriodValue(OtherSentencePeriodType.DAY.name(), offence.getSuspendedSentence()))
                    .build();
        }
        return null;
    }

    private static Integer getPeriodValue(final String periodType, final String periodInString) {
        if (isNotEmpty(periodInString) && periodInString.length() == 3) {
            final String period = periodInString.substring(0, 2);
            final char type = periodInString.charAt(2);

            if (isNumeric(period) && periodType.charAt(0) == type) {
                return Integer.valueOf(period);
            }
        }
        return null;
    }

    private static String getSentenceState(DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getInterimImposedFinalSentence())) {
            final SentenceState sentenceState = SentenceState.findByCpValue(offence.getInterimImposedFinalSentence());
            return nonNull(sentenceState) ? sentenceState.dvlaValue : null;
        }
        return null;
    }

    private static String getSentenceCourtCode(DefendantCaseOffences offence) {
        if (isNotEmpty(offence.getSentencingCourtCode())) {
            return offence.getSentencingCourtCode();
        }
        return null;
    }

    private static String truncateTo(final String value, final int maxLength) {
        if (isEmpty(value)) {
            return null;
        } else if (value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }

    private static String formatDate(final String cpDate) {
        if (isEmpty(cpDate)) {
            return null;
        } else {
            try {
                return dvlaDateFormat.format(cpDateFormat.parse(cpDate));
            } catch (DateTimeParseException e) {
                LOGGER.debug("DateTimeParseException while parsing date", e);
            }
        }
        return cpDate;
    }

    public enum EndorsementType {
        REMOVE, UPDATE, NEW
    }

    public enum Gender {
        M("MALE"), F("FEMALE");

        private final String genderName;

        Gender(final String genderName) {
            this.genderName = genderName;
        }
    }

    public enum PeriodType {
        YEAR(0, 2), MONTH(2, 4), DAY(4, 6), HOUR(6, 8);

        private final int startIndex;
        private final int endIndex;

        PeriodType(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    public enum OtherSentencePeriodType {
        YEAR, MONTH, WEEKS, DAY, HOUR
    }

    public enum OtherSentenceType {
        A("Imprisonment", true),
        C("Suspended Prison Sentence", true),
        E("Conditional Discharge", true),
        J("Absolute Discharge", false),
        M("Community Order", false),
        P("Youth Custody Sentence", true),
        I("No Separate Penalty", false);

        private final String value;
        private final Boolean isACEP;

        OtherSentenceType(String value, Boolean isACEP) {
            this.value = value;
            this.isACEP = isACEP;
        }

        public static OtherSentenceType findByType(String typeStr) {
            for (final OtherSentenceType type : values()) {
                if (type.name().equals(typeStr)) {
                    return type;
                }
            }
            return null;
        }
    }

    public enum ExtendedType {
        DTTP("1", "DTTP"), DTETP("4", "DTETP");

        private final String cpValue;
        private final String dvlaValue;

        ExtendedType(final String cpValue, final String dvlaValue) {
            this.cpValue = cpValue;
            this.dvlaValue = dvlaValue;
        }

        public static ExtendedType findByCpValue(final String cpValue) {
            for (final ExtendedType extendedType : values()) {
                if (extendedType.cpValue.equalsIgnoreCase(cpValue)) {
                    return extendedType;
                }
            }
            return null;
        }
    }

    public enum SentenceState {
        INTERIM("1", "Interim"), FINAL("2", "Final");

        private final String cpValue;
        private final String dvlaValue;

        SentenceState(final String cpValue, final String dvlaValue) {
            this.cpValue = cpValue;
            this.dvlaValue = dvlaValue;
        }

        public static SentenceState findByCpValue(final String cpValue) {
            for (final SentenceState sentenceState : values()) {
                if (sentenceState.cpValue.equalsIgnoreCase(cpValue)) {
                    return sentenceState;
                }
            }
            return null;
        }
    }
}
