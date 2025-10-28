package uk.gov.moj.cpp.stagingdvla.handler;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.moj.cpp.stagingdvla.handler.util.EventStreamAppender.appendEventsToStream;

import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.cpp.stagingdvla.command.handler.DriverNotification;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.Prompts;
import uk.gov.justice.cpp.stagingdvla.event.Results;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.DefendantAggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S3457"})
@ServiceComponent(COMMAND_HANDLER)
public class DriverNotificationHandler {

    protected static final String STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION = "stagingdvla.command.handler.driver-notification";
    private static final Logger LOGGER = getLogger(DriverNotificationHandler.class);
    @Inject
    private EventSource eventSource;

    @Inject
    private AggregateService aggregateService;

    @Handles(STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION)
    public void handleDriverNotification(final Envelope<DriverNotification> envelope) throws EventStreamException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received request {} {}", STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION, envelope.metadata().asJsonObject());
        }

        final DriverNotification driverNotification = envelope.payload();

        LOGGER.info("Request received for hearing id: {}", driverNotification.getOrderingHearingId());

        final EventStream eventStream = eventSource.getStreamById(driverNotification.getMasterDefendantId());
        final DefendantAggregate defendantAggregate = aggregateService.get(eventStream, DefendantAggregate.class);

        final Stream<Object> events = defendantAggregate
                .notifyDriver(
                        driverNotification.getNowContent().getOrderDate(),
                        driverNotification.getOrderingCourt(),
                        driverNotification.getNowContent().getAmendmentDate(),
                        driverNotification.getNowContent().getDefendant(),
                        getCases(driverNotification.getNowContent().getCases()),
                        driverNotification.getOrderingHearingId(),
                        getCourtApplications(driverNotification.getNowContent().getCourtApplications()),
                        driverNotification.getMasterDefendantId());

        if (nonNull(events)) {
            appendEventsToStream(envelope, eventStream, events);
        }

        LOGGER.info("Request processed for hearing id: {}", driverNotification.getOrderingHearingId());
    }

    private List<Cases> getCases(final List<uk.gov.justice.cpp.stagingdvla.command.handler.Cases> cases) {
        final List<Cases> casesList = new ArrayList<>();
        cases.forEach(aCase -> casesList.add(Cases.cases()
                .withReference(aCase.getReference())
                .withCaseId(aCase.getCaseId())
                .withCaseStatus(aCase.getCaseStatus())
                .withInitiationCode(Optional.ofNullable(aCase.getInitiationCode()).orElse(StringUtils.EMPTY))
                .withDefendantCaseOffences(getDefendantCaseOffences(aCase.getDefendantCaseOffences()))
                .build()));
        return casesList;
    }

    private List<CourtApplications> getCourtApplications(final List<uk.gov.justice.cpp.stagingdvla.command.handler.CourtApplications> courtApplications) {
        final List<CourtApplications> courtApplicationsList = new ArrayList<>();
        if (isNotEmpty(courtApplications)) {
            courtApplications.forEach(aCourtApplication -> courtApplicationsList.add(CourtApplications.courtApplications()
                    .withId(aCourtApplication.getId())
                    .withApplicationType(aCourtApplication.getApplicationType())
                    .withApplicationTypeId(aCourtApplication.getApplicationTypeId())
                    .withApplicationStatus(aCourtApplication.getApplicationStatus())
                    .withApplicationReference(aCourtApplication.getApplicationReference())
                    .withApplicationReceivedDate(aCourtApplication.getApplicationReceivedDate())
                    .withApplicationCode(aCourtApplication.getApplicationCode())
                    .withResults(transformResults(aCourtApplication.getResults()))
                    .build()));
        }
        return courtApplicationsList;
    }

    private List<DefendantCaseOffences> getDefendantCaseOffences(List<uk.gov.justice.cpp.stagingdvla.command.handler.DefendantCaseOffences> defendantCaseOffences) {
        final List<DefendantCaseOffences> defendantCaseOffencesList = new ArrayList<>();
        defendantCaseOffences.forEach(
                defendantCaseOffence -> defendantCaseOffencesList.add(transformOffence(defendantCaseOffence)));
        return defendantCaseOffencesList;
    }

    private DefendantCaseOffences transformOffence(final uk.gov.justice.cpp.stagingdvla.command.handler.DefendantCaseOffences defendantCaseOffence) {
        return DefendantCaseOffences.defendantCaseOffences()
                .withMainOffenceCode(defendantCaseOffence.getCode())
                .withCode(defendantCaseOffence.getCode())
                .withTitle(defendantCaseOffence.getTitle())
                .withCivilOffence(defendantCaseOffence.getCivilOffence())
                .withStartDate(defendantCaseOffence.getStartDate())
                .withEndDate(defendantCaseOffence.getEndDate())
                .withConvictionDate(defendantCaseOffence.getConvictionDate())
                .withConvictingCourtCode(defendantCaseOffence.getConvictingCourtCode())
                .withConvictionStatus(defendantCaseOffence.getConvictionStatus())
                .withPlea(defendantCaseOffence.getPlea())
                .withVehicleRegistration(defendantCaseOffence.getVehicleRegistration())
                .withDvlaCode(defendantCaseOffence.getDvlaCode())
                .withModeOfTrial(defendantCaseOffence.getModeOfTrial())
                .withAllocationDecision(defendantCaseOffence.getAllocationDecision())
                .withWording(defendantCaseOffence.getWording())
                .withAlcoholReadingAmount(toString(defendantCaseOffence.getAlcoholReadingAmount()))
                .withAlcoholReadingMethodCode(defendantCaseOffence.getAlcoholReadingMethodCode())
                .withAlcoholReadingMethodDescription(defendantCaseOffence.getAlcoholReadingMethodDescription())
                .withEndorsableFlag(defendantCaseOffence.getEndorsableFlag())
                .withResults(transformResults(defendantCaseOffence.getResults()))
                .build();
    }

    private List<Results> transformResults(final List<uk.gov.justice.cpp.stagingdvla.command.handler.Results> results) {
        final List<Results> resultsList = new ArrayList<>();
        if (isNotEmpty(results)) {
            results.forEach(aResult -> resultsList.add(Results.results()
                    .withLabel(aResult.getLabel())
                    .withResultIdentifier(aResult.getResultIdentifier())
                    .withNowRequirementText(aResult.getNowRequirementText())
                    .withPublishedForNows(aResult.getPublishedForNows())
                    .withPrompts(transformPrompts(aResult.getPrompts()))
                    .withResultWording(aResult.getResultWording())
                    .withResultDefinitionGroup(aResult.getResultDefinitionGroup())
                    .withSequence(aResult.getSequence())
                    .withD20(aResult.getD20())
                    .withPointsDisqualificationCode(aResult.getPointsDisqualificationCode())
                    .withDrivingTestStipulation(aResult.getDrivingTestStipulation())
                    .withDvlaCode(aResult.getDvlaCode())
                    .build()));
        }
        return resultsList;
    }

    private List<Prompts> transformPrompts(final List<uk.gov.justice.cpp.stagingdvla.command.handler.Prompts> prompts) {
        final List<Prompts> promptsList = new ArrayList<>();
        if (isNotEmpty(prompts)) {
            prompts.forEach(aPrompt -> promptsList.add(Prompts.prompts()
                    .withPromptIdentifier(aPrompt.getPromptIdentifier())
                    .withPromptReference(aPrompt.getPromptReference())
                    .withDurationSequence(aPrompt.getDurationSequence())
                    .withLabel(aPrompt.getLabel())
                    .withValue(aPrompt.getValue())
                    .build()));
        }
        return promptsList;
    }

    private String toString(final Integer value) {
        if (nonNull(value)) {
            return value.toString();
        } else {
            return null;
        }
    }
}
