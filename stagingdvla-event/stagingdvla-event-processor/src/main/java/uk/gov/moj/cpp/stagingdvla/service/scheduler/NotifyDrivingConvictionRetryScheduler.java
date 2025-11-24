package uk.gov.moj.cpp.stagingdvla.service.scheduler;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.domain.DrivingConvictionRetry;
import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;
import uk.gov.moj.cpp.stagingdvla.query.view.StagingdvlaQueryView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Startup
public class NotifyDrivingConvictionRetryScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyDrivingConvictionRetryScheduler.class);

    private static final int INITIAL_RETRY_ITEM_COUNT = 1;

    @Inject
    private DvlaApimConfig dvlaApimConfig;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StagingdvlaQueryView stagingdvlaQueryView;

    public enum TimerStatus {
        STOPPED, RETRYING, RUNNING
    }

    public enum DvlaResponseType {
        SUCCESS, FAIL
    }

    private AtomicReference<TimerStatus> currentStatus;
    private int retryIntervalInMinutes;
    private int maxRecordCount;

    @Resource
    private TimerService timerService;

    @PostConstruct
    private void init() {
        currentStatus = new AtomicReference<>(TimerStatus.STOPPED);
        retryIntervalInMinutes = Integer.valueOf(dvlaApimConfig.getDrivingConvictionRetryIntervalInMinutes());
        maxRecordCount = Integer.valueOf(dvlaApimConfig.getDrivingConvictionRetryMaxRecordCount());
        startTimer();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NotifyDrivingConvictionRetryScheduler initialized. Timer started.");
        }
    }

    public void dvlaResponseReceived(final DvlaResponseType dvlaResponseType) {
        if (TimerStatus.RETRYING.equals(currentStatus.get())) {
            currentStatus.set(TimerStatus.RUNNING);
            if (DvlaResponseType.SUCCESS.equals(dvlaResponseType)) {
                retryDrivingConviction(this.maxRecordCount);
            }
        }
    }

    private void startTimer() {
        stopTimers();
        final Timer timer = timerService.createSingleActionTimer(getTimerIntervalInMillis(), new TimerConfig("RETRY", true));
        currentStatus.set(TimerStatus.RUNNING);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NotifyDrivingConvictionRetryScheduler timer started with nextTimeout: {}", timer.getNextTimeout());
        }
    }

    private void stopTimers() {
        if (isNotEmpty(timerService.getTimers())) {
            timerService.getTimers().forEach(Timer::cancel);
        }
        currentStatus.set(TimerStatus.STOPPED);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NotifyDrivingConvictionRetryScheduler timers stopped");
        }
    }

    @Timeout
    public void expireTimer() {
        startTimer();
        currentStatus.set(TimerStatus.RETRYING);
        retryDrivingConviction(INITIAL_RETRY_ITEM_COUNT);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("NotifyDrivingConvictionRetryScheduler timer expired");
        }
    }

    @PreDestroy
    private void cleanup() {
        stopTimers();
    }

    private void retryDrivingConviction(final int maxCount) {
        final List<DrivingConvictionRetry> drivingConvictionRetries = getDrivingConvictionRetries(maxCount);

        if (isNotEmpty(drivingConvictionRetries)) {
            drivingConvictionRetries.forEach(this::triggerNextRetryForDriverNotified);
        }
    }

    private List<DrivingConvictionRetry> getDrivingConvictionRetries(final int maxCount) {
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName("stagingdvla.query.driving-conviction-retry")
                        .build(),
                createObjectBuilder().add("maxCount", maxCount).build());

        final Envelope<JsonObject> drivingConvictionRetries = stagingdvlaQueryView
                .findDrivingConvictionRetries(requestEnvelope);

        return convertToList(drivingConvictionRetries.payload().getJsonArray("drivingConvictionRetries"),
                DrivingConvictionRetry.class);

    }

    private void triggerNextRetryForDriverNotified(final DrivingConvictionRetry drivingConvictionRetry) {
        sender.sendAsAdmin(Envelope.envelopeFrom(
                metadataFrom(metadataBuilder().withId(randomUUID())
                        .withName("stagingdvla.command.handler.trigger-next-retry-for-driver-notified")
                        .build())
                ,  createObjectBuilder()
                        .add("convictionId", drivingConvictionRetry.getConvictionId().toString())
                        .add("masterDefendantId", drivingConvictionRetry.getMasterDefendantId().toString())
                        .build()));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("stagingdvla.command.handler.trigger-next-retry-for-driver-notified sent for convictionId: {}, masterDefendantId: {}",
                    drivingConvictionRetry.getConvictionId(), drivingConvictionRetry.getMasterDefendantId());
        }
    }

    private <T> List<T> convertToList(final JsonArray jsonArray, final Class<T> clazz) {
        final List<T> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(this.jsonObjectToObjectConverter.convert(jsonArray.getJsonObject(i), clazz));
        }
        return list;
    }

    private long getTimerIntervalInMillis() {
        return Long.valueOf(retryIntervalInMinutes * Long.valueOf(60) * Long.valueOf(1000));
    }
}
