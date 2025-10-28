package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DISQUALIFICATION_PERIOD;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.PERIOD_00;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.PERIOD_LIFETIME;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDDL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDDTL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDOTEL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDPL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDPTEL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRAL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRNL;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.DDRVL;

import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DisqualificationPeriodHelper {

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    static final int MAX_VALUE = 99;

    private static final List<String> LIFETIME_DISQUALIFICATION_RESULTS = asList(
            DDDTL.id, DDOTEL.id, DDPL.id, DDPTEL.id, DDRAL.id, DDRNL.id, DDRVL.id, DDDL.id
    );

    private DisqualificationPeriodHelper() {
    }

    public static String getDisqualificationPeriod(DefendantCaseOffences offence, final String orderDate) {
        final AtomicReference<String> disqualificationPeriod = new AtomicReference<>(EMPTY);
        final AtomicBoolean hasLifetimeDisqualification = new AtomicBoolean(FALSE);

        if (isNotEmpty(offence.getResults())) {
            offence.getResults().stream().forEach(result -> {
                if (isNotEmpty(result.getPrompts())) {
                    result.getPrompts().stream().forEach(prompt -> {
                        if (DISQUALIFICATION_PERIOD.equalsIgnoreCase(prompt.getPromptReference())) {
                            disqualificationPeriod.set(prompt.getValue());
                        }
                    });
                }
                if (isNotEmpty(result.getResultIdentifier()) &&
                        LIFETIME_DISQUALIFICATION_RESULTS.stream()
                                .anyMatch(result.getResultIdentifier()::equalsIgnoreCase)) {
                    hasLifetimeDisqualification.set(TRUE);
                }
            });
        }

        return calculateDisqualificationPeriod(disqualificationPeriod, hasLifetimeDisqualification, orderDate);
    }

    private static String calculateDisqualificationPeriod(final AtomicReference<String> disqualificationPeriod, final AtomicBoolean hasLifetimeDisqualification, final String orderDate) {
        if (isNotEmpty(disqualificationPeriod.get())) {
            return formatPeriodToNumeric(disqualificationPeriod.get(), orderDate);
        } else if (hasLifetimeDisqualification.get()) {
            return PERIOD_LIFETIME;
        } else {
            return EMPTY;
        }
    }

    public static String formatPeriodToNumeric(final String unformatted, final String orderDate) {
        String year = PERIOD_00;
        String month = PERIOD_00;
        String day = PERIOD_00;
        String week = PERIOD_00;

        if (isNotEmpty(unformatted)) {
            final String[] parts = unformatted.trim().replaceAll(" +", " ").split(" ");
            for (int i = 1; i < parts.length; i++) {
                final String part = parts[i - 1];
                if (parts[i].toUpperCase().contains("YEAR")) {
                    year = part;
                } else if (parts[i].toUpperCase().contains("MONTH")) {
                    month = part;
                } else if (parts[i].toUpperCase().contains("DAY")) {
                    day = part;
                } else if (parts[i].toUpperCase().contains("WEEK")) {
                    week = part;
                }
            }
        }
        return getFormattedDisqualificationPeriod(year, month, week, day, orderDate);
    }

    private static String getFormattedDisqualificationPeriod(final String year, final String month, final String week, final String day, final String orderDate) {
        final int weekMultiplier = Integer.parseInt(week) > 0 ? Integer.parseInt(week) * 7 : 0;
        final int totalDays = Integer.parseInt(day) + weekMultiplier;

        if (Integer.parseInt(year) <= MAX_VALUE && Integer.parseInt(month) <= MAX_VALUE && totalDays <= MAX_VALUE) {
            return String.format("%02d%02d%02d", Integer.parseInt(year), Integer.parseInt(month), totalDays);
        } else if (year.equalsIgnoreCase(PERIOD_00) && month.equalsIgnoreCase(PERIOD_00) &&
                day.equalsIgnoreCase(PERIOD_00) && week.equalsIgnoreCase(PERIOD_00)) {
            return EMPTY;
        } else {
            final Period period = getPeriodDuration(year, month, week, day, orderDate);
            return String.format("%02d%02d%02d", period.getYears(), period.getMonths(), period.getDays());
        }
    }

    private static Period getPeriodDuration(final String years, final String months, final String weeks, final String days, final String orderDate) {
        final LocalDate now = parseDate(orderDate);
        final LocalDate futureDate = now
                .plusYears(Integer.parseInt(years))
                .plusMonths(Integer.parseInt(months))
                .plusWeeks(Integer.parseInt(weeks))
                .plusDays(Integer.parseInt(days));
        return Period.between(now, futureDate);
    }

    private static LocalDate parseDate(final String orderDate) {
        try {
            return LocalDate.parse(orderDate, formatter);
        } catch (NumberFormatException e) {
            return LocalDate.now();
        }
    }
}