package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Objects.nonNull;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.DriverNotifiedEngine.transformDriverNotified;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryCancelled;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryScheduled;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;

@SuppressWarnings("squid:S1602")
public class DefendantAggregate implements Aggregate {
    private static final long serialVersionUID = 3L;
    private static final Logger LOGGER = getLogger(DefendantAggregate.class);
    private static final String APPLICATION_REFUSED_RESULT = "d3902789-4cc8-4753-a15f-7e26dd39f6ae";
    private static final String NEXT_HEARING_RESULT_ID = "f00359b5-7303-403b-b59e-0b1a1daa89bc";
    private static final String NEXT_HEARING_IN_CROWN_RESULT_ID = "fbed768b-ee95-4434-87c8-e81cbc8d24c8";
    private static final String NEXT_HEARING_IN_MAGISTRATE_RESULT_ID = "70c98fa6-804d-11e8-adc0-fa7ae01bbebc";
    private static final String REMUB_RESULT_ID = "d076bd4a-17d5-4720-899a-1c6f96e3b35f";

    private DriverNotified previousDriverNotified;
    private DriverNotified previousPreviousDriverNotified;
    private boolean isWaitingRetryTrigger = false;
    private int retrySequence = 0;
    private final Map<String, DriverNotified> previousDriverNotifiedByCase = new HashMap<>();

    public Stream<Object> notifyDriver(final String orderDate,
                                       final CourtCentre orderingCourt,
                                       final String amendmentDate,
                                       final Nowdefendant defendant,
                                       final List<Cases> currentCases,
                                       final UUID hearingId,
                                       final List<CourtApplications> courtApplications,
                                       final UUID masterDefendantId) {

        // Create a new event for each incoming cases

        if((areCasesResultsEmpty(currentCases) || areCasesResultsAdjourn(currentCases)) && nonNull(this.previousPreviousDriverNotified) && (
                areApplicationsRefused(courtApplications) || areApplicationsAdjourn(courtApplications)
                )) {
            return Stream.of(DriverNotified.driverNotified()
                    .withValuesFrom(this.previousPreviousDriverNotified)
                    .withIsCopiedFromPrevious(true)
                    .build());
        }
        final List<DriverNotified> driverNotifiedEvents = transformDriverNotified(
                this.previousDriverNotifiedByCase,
                orderDate,
                orderingCourt,
                amendmentDate,
                defendant,
                currentCases,
                hearingId,
                courtApplications);

        if (driverNotifiedEvents.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("D20 not generated since there are no appeal/endorsable offences/change from previous offences for hearingId {}", hearingId);
            }
            return null;
        }

        final List<DriverNotified> transformedDriverNotifiedEvents = driverNotifiedEvents.stream()
                .map(e -> getDriverNotifiedEvent(e, masterDefendantId, 0))
                .collect(Collectors.toList());

        final Stream.Builder<Object> streamBuilder =
                streamingEvents(transformedDriverNotifiedEvents, masterDefendantId);

        return apply(streamBuilder.build());
    }

    private Stream.Builder<Object> streamingEvents(final List<DriverNotified> driverNotifiedEvents, final UUID masterDefendantId) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        driverNotifiedEvents.stream().forEach(
                e -> {
                    if (this.isWaitingRetryTrigger) {
                        streamBuilder.add(getNextRetryCancelledEvent(previousDriverNotified.getIdentifier(), masterDefendantId));
                    }
                    streamBuilder.add(e);
                }
        );

        return streamBuilder;
    }

    public Stream<Object> scheduleNextRetryForDriverNotified(final UUID convictionId, final UUID masterDefendantId) {
        return apply(Stream.of(getNextRetryScheduledEvent(convictionId, masterDefendantId)));
    }

    public Stream<Object> triggerNextRetryForDriverNotified(final UUID convictionId, final UUID masterDefendantId) {
        if (!this.isWaitingRetryTrigger) {
            return Stream.empty();
        } else if (isRetryMatchingWithPreviousDriverNotified(convictionId)) {
            return apply(Stream.of(getDriverNotifiedEvent(this.previousDriverNotified, masterDefendantId, this.retrySequence + 1)));
        } else {
            return apply(Stream.of(getNextRetryCancelledEvent(convictionId, masterDefendantId)));
        }
    }

    private boolean isRetryMatchingWithPreviousDriverNotified(final UUID convictionId) {
        return nonNull(this.previousDriverNotified)
                && nonNull(this.previousDriverNotified.getIdentifier())
                && this.previousDriverNotified.getIdentifier().equals(convictionId);
    }

    private DriverNotified getDriverNotifiedEvent(final DriverNotified driverNotified, final UUID masterDefendantId, final int retrySeq) {
        return DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withMasterDefendantId(masterDefendantId)
                .withRetrySequence(retrySeq)
                .build();
    }

    private DriverNotifiedNextRetryScheduled getNextRetryScheduledEvent(final UUID convictionId, final UUID masterDefendantId) {
        return DriverNotifiedNextRetryScheduled.driverNotifiedNextRetryScheduled()
                .withConvictionId(convictionId)
                .withMasterDefendantId(masterDefendantId)
                .build();
    }

    private DriverNotifiedNextRetryCancelled getNextRetryCancelledEvent(final UUID convictionId, final UUID masterDefendantId) {
        return DriverNotifiedNextRetryCancelled.driverNotifiedNextRetryCancelled()
                .withConvictionId(convictionId)
                .withMasterDefendantId(masterDefendantId)
                .build();
    }

    private boolean areCasesResultsEmpty(final List<Cases> cases) {
        return CollectionUtils.isEmpty(cases) || cases.stream().allMatch(c->c.getDefendantCaseOffences().isEmpty());
    }

    private boolean areCasesResultsAdjourn(final List<Cases> cases) {
        return cases.stream().allMatch(c->c.getDefendantCaseOffences().stream().allMatch(
                cdf->cdf.getResults().stream().allMatch(
                        r->List.of(NEXT_HEARING_RESULT_ID, NEXT_HEARING_IN_CROWN_RESULT_ID, NEXT_HEARING_IN_MAGISTRATE_RESULT_ID, REMUB_RESULT_ID).contains(r.getResultIdentifier()))
        ));
    }

    private boolean areApplicationsRefused(final List<CourtApplications> courtApplications) {
        return CollectionUtils.isNotEmpty(courtApplications) && courtApplications.stream()
                .allMatch(c-> CollectionUtils.isNotEmpty(c.getResults()) &&
                        c.getResults().stream().allMatch(r-> APPLICATION_REFUSED_RESULT.equals(r.getResultIdentifier())));
    }

    private boolean areApplicationsAdjourn(final List<CourtApplications> courtApplications) {
        return CollectionUtils.isNotEmpty(courtApplications) && courtApplications.stream()
                .allMatch(c-> CollectionUtils.isNotEmpty(c.getResults()) &&
                        c.getResults().stream().allMatch(r-> List.of(NEXT_HEARING_RESULT_ID, NEXT_HEARING_IN_CROWN_RESULT_ID, NEXT_HEARING_IN_MAGISTRATE_RESULT_ID).contains(r.getResultIdentifier())));

    }


    @Override
    public Object apply(final Object event) {

        return match(event).with(

                when(DriverNotified.class).apply(e -> {
                    // Last event for this defendant.
                    if (Boolean.TRUE.equals(e.getIsCopiedFromPrevious())) {
                        previousDriverNotified = previousPreviousDriverNotified;
                        previousPreviousDriverNotified = null;
                    } else {
                        previousPreviousDriverNotified = previousDriverNotified;
                        previousDriverNotified = e;
                    }

                    // For each case, get the latest DriverNotifiedEvent. This is required for comparing if
                    // results has been updated
                    if (nonNull(e.getCases())) {
                        e.getCases().forEach(c -> previousDriverNotifiedByCase.put(c.getReference(), e));
                    }

                    isWaitingRetryTrigger = false;
                    if (nonNull(e.getRetrySequence())) {
                        retrySequence = e.getRetrySequence();
                    } else {
                        retrySequence = 0;
                    }
                }),
                when(DriverNotifiedNextRetryScheduled.class).apply(e -> {
                    isWaitingRetryTrigger = true;
                }),
                when(DriverNotifiedNextRetryCancelled.class).apply(e -> {
                    isWaitingRetryTrigger = false;
                    retrySequence = 0;
                }),
                otherwiseDoNothing());
    }
}
