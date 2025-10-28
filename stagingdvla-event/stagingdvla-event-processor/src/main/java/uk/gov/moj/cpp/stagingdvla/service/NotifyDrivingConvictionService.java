package uk.gov.moj.cpp.stagingdvla.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConviction;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConvictionResponse;
import uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;


public class NotifyDrivingConvictionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifyDrivingConvictionService.class);

    private static final String URL_NOTIFY_DRIVING_CONVICTION = "/notify-driving-conviction";

    @Inject
    private DvlaApimConfig dvlaApimConfig;

    @Inject
    private RestEasyClientService restEasyClientService;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    public NotifyDrivingConvictionResponse notifyDrivingConviction(final DriverNotified driverNotified) {
        String otherSentencesFromInput = driverNotified.getCases().stream().flatMap(c ->
                c.getDefendantCaseOffences().stream().map(o -> o.getOtherSentence())).collect(Collectors.joining(","));
        final NotifyDrivingConviction request = DrivingConvictionTransformUtil.transformToNotifyDrivingConviction(driverNotified);
        Response response = null;
        try {
            final String notifyDrivingConvictionUrl = dvlaApimConfig.getDvlaEnquiryApimUrl().concat(URL_NOTIFY_DRIVING_CONVICTION);
            final String jsonPayload = convertToJson(request);
            response = restEasyClientService.post(notifyDrivingConvictionUrl, jsonPayload, dvlaApimConfig.getSubscriptionKey());
            LOGGER.info("Azure Function {} invoked. Received response: {}",
                    notifyDrivingConvictionUrl, response.getStatus());
            return getTransformedResponse(response);
        } catch (final IOException ex) {
            LOGGER.error("IOException occurred in notifyDrivingConviction with: {} ", ex.getMessage(), ex);
            return getEmptyResponse();
        } catch (final ProcessingException ex) {
            LOGGER.error("ProcessingException occurred in notifyDrivingConviction with: {} ", ex.getMessage(), ex);
            return getEmptyResponse();
        } finally {
            if (nonNull(response)) {
                try {
                    response.close();
                } catch (RuntimeException e) {
                    LOGGER.error("Exception occurred after notifyDrivingConviction while closing response object {} ", e.getMessage(), e);
                }
            }
        }
    }

    private String convertToJson(final NotifyDrivingConviction notifyDrivingConvictionRequest) throws JsonProcessingException {
        return objectMapper.writeValueAsString(notifyDrivingConvictionRequest);
    }

    private NotifyDrivingConvictionResponse getEmptyResponse() {
        return NotifyDrivingConvictionResponse.notifyDrivingConvictionResponse()
                .build();
    }

    private NotifyDrivingConvictionResponse getTransformedResponse(final Response response) {
        final int statusCode = response.getStatus();
        return NotifyDrivingConvictionResponse.notifyDrivingConvictionResponse()
                .withStatus(statusCode)
                .withErrors(getErrors(statusCode, response))
                .build();
    }

    private String getErrors(final int statusCode, final Response response) {
        return isErrorResponse(statusCode) ? response.readEntity(String.class) : null;
    }

    private boolean isErrorResponse(final int statusCode) {
        return (statusCode == SC_BAD_REQUEST || statusCode == SC_UNAUTHORIZED || statusCode == SC_INTERNAL_SERVER_ERROR);
    }
}
