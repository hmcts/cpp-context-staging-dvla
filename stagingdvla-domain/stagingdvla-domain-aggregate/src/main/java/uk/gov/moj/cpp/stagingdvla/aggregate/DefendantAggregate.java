package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ResultType.SUMRCC;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.DriverNotifiedEngine.transformDriverNotified;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryCancelled;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotifiedNextRetryScheduled;
import uk.gov.justice.cpp.stagingdvla.event.SjpCaseReferred;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

@SuppressWarnings("squid:S1602")
public class DefendantAggregate implements Aggregate {
    private static final long serialVersionUID = 4L;
    private static final Logger LOGGER = getLogger(DefendantAggregate.class);

    private DriverNotified previousDriverNotified;
    private boolean isWaitingRetryTrigger = false;
    private int retrySequence = 0;
    private final Map<String, DriverNotified> previousDriverNotifiedByCase = new HashMap<>();
    private final Map<UUID, List<String>> sjpCaseReferredApplicationTypes = new HashMap<>();

    public Stream<Object> notifyDriver(final String orderDate,
                                       final CourtCentre orderingCourt,
                                       final String amendmentDate,
                                       final Nowdefendant defendant,
                                       final List<Cases> currentCases,
                                       final UUID hearingId,
                                       final List<CourtApplications> courtApplications,
                                       final UUID masterDefendantId) {

        final List<SjpCaseReferred> sjpCaseReferredEvents = getSjpCaseReferredEvents(currentCases, courtApplications);
        // Create a new event for each incoming cases
        final List<DriverNotified> driverNotifiedEvents = transformDriverNotified(
                this.previousDriverNotifiedByCase,
                orderDate,
                orderingCourt,
                amendmentDate,
                defendant,
                currentCases,
                hearingId,
                courtApplications,
                sjpCaseReferredApplicationTypes);

        if (driverNotifiedEvents.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("D20 not generated since there are no appeal/endorsable offences/change from previous offences for hearingId {}", hearingId);
            }
            if (sjpCaseReferredEvents.isEmpty()) {
                return null;
            } else {
                final Stream.Builder<Object> streamBuilder = Stream.builder();
                sjpCaseReferredEvents.forEach(streamBuilder::add);
                return apply(streamBuilder.build());
            }

        }

        final List<DriverNotified> transformedDriverNotifiedEvents = driverNotifiedEvents.stream()
                .map(e -> getDriverNotifiedEvent(e, masterDefendantId, 0))
                .collect(Collectors.toList());

        final Stream.Builder<Object> streamBuilder =
                streamingEvents(transformedDriverNotifiedEvents, masterDefendantId);




        return apply(streamBuilder.build());
    }

    /**
     * Builds {@link SjpCaseReferred} events for cases that have been referred from SJP to CC
     * <p>
     * The referral from SJP to CC occurs in two distinct stages:
     * <ol>
     *   <li>
     *     Initially, a case receives a {@code SUMRCC} result indicating referral to
     *     CC. At this point, no court application information is available.
     *     An event is raised containing only the {@code caseId}.
     *   </li>
     *   <li>
     *     Subsequently, court applications (e.g. reopen applications) are received
     *     for the same case. A second event is raised for the same {@code caseId},
     *     this time including the associated court application type identifiers.
     *   </li>
     * </ol>
     *
     * This method is required because once a case has moved to CC,
     * there is no other reliable way to determine that the case originated from
     * SJP or that it inherited court applications (such as reopen applications)
     * related to the original SJP case.
     *
     * <p>
     * Event generation rules:
     * <ul>
     *   <li>
     *     If no court application types have been recorded yet, an
     *     {@link SjpCaseReferred} event is emitted for each case that contains a
     *     {@code SUMRCC} result, populated with {@code caseId} only.
     *   </li>
     *   <li>
     *     If court applications have been recorded and all application type values
     *     are present, a second {@link SjpCaseReferred} event is emitted for the
     *     relevant case(s), including both {@code caseId} and application type IDs.
     *   </li>
     * </ul>
     */
    private List<SjpCaseReferred> getSjpCaseReferredEvents(final List<Cases> currentCases, final List<CourtApplications> courtApplications) {
        final List<SjpCaseReferred> sjpCaseReferredEvents = new ArrayList<>();
        if (sjpCaseReferredApplicationTypes.isEmpty()) {
            currentCases.stream()
                    .filter(currentCase -> currentCase.getDefendantCaseOffences().stream()
                            .anyMatch(defendantCaseOffence -> nonNull(defendantCaseOffence.getResults()) && defendantCaseOffence.getResults().stream()
                                    .anyMatch(result -> SUMRCC.id.equals(result.getResultIdentifier()))))
                    .forEach(currentCase -> sjpCaseReferredEvents.add(SjpCaseReferred.sjpCaseReferred().withCaseId(currentCase.getCaseId()).build()));
        } else if (sjpCaseReferredApplicationTypes.values().stream().allMatch(Objects::isNull)) {
            currentCases.forEach(currentCase -> {
                if (sjpCaseReferredApplicationTypes.containsKey(currentCase.getCaseId()) && isNotEmpty(courtApplications)) {
                    sjpCaseReferredEvents.add(SjpCaseReferred.sjpCaseReferred()
                            .withCaseId(currentCase.getCaseId())
                            .withApplicationTypeIds(courtApplications.stream()
                                    .map(CourtApplications::getApplicationTypeId)
                                    .toList())
                            .build());
                }
            });
        }

        return sjpCaseReferredEvents;
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

    @Override
    public Object apply(final Object event) {

        return match(event).with(

                when(DriverNotified.class).apply(e -> {
                    // Last event for this defendant.
                    previousDriverNotified = e;

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
                when(SjpCaseReferred.class).apply(e ->
                    sjpCaseReferredApplicationTypes.put(e.getCaseId(), e.getApplicationTypeIds())
                ),
                otherwiseDoNothing());
    }
}
