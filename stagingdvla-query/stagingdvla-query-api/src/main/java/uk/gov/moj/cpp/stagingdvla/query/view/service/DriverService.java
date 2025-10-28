package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_EXPECTATION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.substring;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverImageQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverSummaryQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverImageResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverSummaryResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Endorsement;
import uk.gov.moj.cpp.stagingdvla.query.view.response.Error;

import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DriverService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverService.class);
    private static final String FIND_BY_DRIVER_NUMBER_URL = "/drivers/retrieve";
    private static final String FIND_BY_DRIVER_DETAILS_URL = "/drivers/find";
    private static final String FIND_DRIVER_IMAGE_URL = "/images/retrieve";
    private static final String CODE = "code";
    private static final String DETAIL = "detail";
    private static final String ERRORS = "errors";
    private static final String STATUS = "status";
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private static final String AZURE_LOGS = "Azure Function {} invoked. Received response: {}";
    private static final UnaryOperator<String> driverNumberFunc = rawDriverNumber -> substring(rawDriverNumber, 0, 16);

    @Inject
    @Value(key = "dvlaEnquiryApimUrl", defaultValue = "http://localhost:8080/dvla/enquiry/v1")
    private String dvlaEnquiryApimUrl;
    //used existing subscription key which is common
    @Inject
    @Value(key = "mireportdata.caseFilter.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;
    @Inject
    private RestEasyClientService restEasyClientService;

    /**
     * Find Driver by DriverNumber.
     *
     * @param queryParameters including driverNumber of the driver to find.
     * @return a matching {@link DriverResponse} or  Null, when not found.
     */
    public DriverResponse findByDriverNumber(final DriverQueryParameters queryParameters) {
        DriverResponse driverResponse = new DriverResponse.Builder().build();
        try {
            final String findByDriverNumberUrl = dvlaEnquiryApimUrl.concat(FIND_BY_DRIVER_NUMBER_URL);
            final String payload = Json.createObjectBuilder().add("drivingLicenceNumber", driverNumberFunc.apply(queryParameters.getDriverNumber())).build().toString();
            final Response response = restEasyClientService.post(findByDriverNumberUrl, payload, subscriptionKey);
            LOGGER.info(AZURE_LOGS, findByDriverNumberUrl, response.getStatus());
            driverResponse = transformDriverResponse(response);
        } catch (final IOException ex) {
            LOGGER.error("Exception occurred in findByDriverNumber with: {} ", ex.getMessage(), ex);
        }
        return driverResponse;
    }


    /**
     * Find Driver(s) by Driver details.
     *
     * @param queryParameters including driver details (lastName, firstNames, dateOfBirth, postcode)
     *                        of the driver(s) to find.
     * @return matching driver(s') summaries as a list inside {@link DriverSummaryResponse}
     */
    public DriverSummaryResponse findByDriverDetails(final DriverSummaryQueryParameters queryParameters) {
        final DriverSummaryResponse.Builder builder = new DriverSummaryResponse.Builder();
        final Map<String, Object> params = queryParameters.getParams();
        try {
            if (params.size() > 2) {
                Response response = getClientServiceResponse(params);
                final DriverSummaryResponse driverSummaryResponse = transformDriverSummaryResponse(response);
                if (retryWithoutPostcode(driverSummaryResponse, queryParameters.getPostcode(), params)) {
                    params.remove("postcode");
                    response = getClientServiceResponse(params);
                    return transformDriverSummaryResponse(response);
                }
                return driverSummaryResponse;
            } else {
                builder.error(
                        new Error.Builder().detail("please enter at least three parameters").status(Integer.toString(SC_EXPECTATION_FAILED)).build()
                );
            }
        } catch (final IOException ex) {
            LOGGER.error("Exception occurred in findByDriverDetails with: {} ", ex.getMessage(), ex);
        }
        return builder.build();
    }


    public DriverImageResponse findDriverImage(final DriverImageQueryParameters queryParameters) {
        DriverImageResponse driverImageResponse = new DriverImageResponse.Builder().build();
        try {
            final String findDriverImageUrl = dvlaEnquiryApimUrl.concat(FIND_DRIVER_IMAGE_URL);
            final JsonObjectBuilder builder = Json.createObjectBuilder().add("drivingLicenceNumber", driverNumberFunc.apply(queryParameters.getDriverNumber()));
            if(queryParameters.getRequiredImage() != null){
                builder.add("requiredImage", queryParameters.getRequiredImage());
            }
            final String payload = builder.build().toString();
            final Response response = restEasyClientService.post(findDriverImageUrl, payload, subscriptionKey);
            LOGGER.info(AZURE_LOGS, findDriverImageUrl, response.getStatus());
            driverImageResponse = transformDriverImageResponse(response);
        } catch (final IOException ex) {
            LOGGER.error("Exception occurred in findDriverImage with: {} ", ex.getMessage(), ex);
        }
        return driverImageResponse;
    }

    private Response getClientServiceResponse(Map<String, Object> params){
        final String findByDriverDetailsUrl = dvlaEnquiryApimUrl.concat(FIND_BY_DRIVER_DETAILS_URL);
        final String jsonPayload = prepareJsonPayload(params);
        final Response response = restEasyClientService.post(findByDriverDetailsUrl, jsonPayload, subscriptionKey);
        LOGGER.info(AZURE_LOGS, findByDriverDetailsUrl, response.getStatus());
        return response;
    }

    private DriverResponse transformDriverResponse(final Response response) throws IOException {
        final String responseAsString = response.readEntity(String.class);
        final int statusCode = response.getStatus();
        if (statusCode == SC_BAD_REQUEST || statusCode == SC_NOT_FOUND || statusCode == SC_INTERNAL_SERVER_ERROR || statusCode == SC_FORBIDDEN) {
            return getErrorDriverResponse(responseAsString);
        }

        final DriverResponse driverResponse = objectMapper.reader()
                .forType(DriverResponse.class)
                .readValue(responseAsString);

        driverResponse.getEndorsements()
                .sort(Comparator.comparing(Endorsement::getSentenceDate, Comparator.nullsFirst(Comparator.reverseOrder()))
                        .thenComparing(Endorsement::getConvictionDate, Comparator.nullsFirst(Comparator.reverseOrder())));

        return driverResponse;
    }

    private DriverImageResponse transformDriverImageResponse(final Response response) throws IOException {
        final String responseAsString = response.readEntity(String.class);
        final int statusCode = response.getStatus();
        if (statusCode == SC_BAD_REQUEST || statusCode == SC_NOT_FOUND || statusCode == SC_INTERNAL_SERVER_ERROR) {
            return getErrorDriverImageResponse(responseAsString);
        }
        return objectMapper.reader()
                .forType(DriverImageResponse.class)
                .readValue(responseAsString);
    }

    private DriverSummaryResponse transformDriverSummaryResponse(final Response response) throws IOException {
        final String responseAsString = response.readEntity(String.class);
        final int statusCode = response.getStatus();
        if (statusCode == SC_BAD_REQUEST || statusCode == SC_NOT_FOUND || statusCode == SC_INTERNAL_SERVER_ERROR) {
            return getErrorDriverSummaryResponse(responseAsString);
        }
        final DriverSummaryResponse driverSummaryResponse = objectMapper.reader()
                .forType(DriverSummaryResponse.class)
                .readValue(responseAsString);
        driverSummaryResponse.setCount(response.getHeaderString("x-total-count"));
        return driverSummaryResponse;
    }

    private DriverResponse getErrorDriverResponse(final String responseAsString) {
        return new DriverResponse.Builder().error(getErrorMessage(responseAsString)).build();
    }

    private DriverSummaryResponse getErrorDriverSummaryResponse(final String responseAsString) {
        return new DriverSummaryResponse.Builder().error(getErrorMessage(responseAsString)).build();
    }

    private DriverImageResponse getErrorDriverImageResponse(final String responseAsString) {
        return new DriverImageResponse.Builder().error(getErrorMessage(responseAsString)).build();
    }

    private Error getErrorMessage(final String responseAsString) {
        final Error.Builder builder = new Error.Builder();
        try (final JsonReader jsonReader = Json.createReader(new StringReader(responseAsString))) {
            final JsonObject jsonObject = jsonReader.readObject();
            if (jsonObject.containsKey(ERRORS)) {
                final JsonObject errorJson = jsonObject.getJsonArray(ERRORS).getJsonObject(0);
                if (errorJson.containsKey(CODE)) {
                    builder.title(errorJson.getString(CODE));
                }
                if (errorJson.containsKey(DETAIL)) {
                    builder.detail(errorJson.getString(DETAIL));
                }
                if (errorJson.containsKey(STATUS)) {
                    builder.status(errorJson.getString(STATUS));
                }
            }
        }
        return builder.build();
    }

    private String prepareJsonPayload(final Map<String, Object> params) {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        final JsonObjectBuilder criteria = Json.createObjectBuilder();
        final JsonObjectBuilder optionsBuilder = Json.createObjectBuilder();

        params.entrySet().forEach(entry -> {
            if ("exactFirstNamesMatch".equals(entry.getKey())) {
                if ("true".equalsIgnoreCase(entry.getValue().toString())) {
                    optionsBuilder.add("firstNamesMatchType", "exact");
                    builder.add("options", optionsBuilder.build());
                }
            } else {
                criteria.add(entry.getKey(), entry.getValue().toString());
            }
        });
        builder.add("criteria", criteria);
        return builder.build().toString();
    }

    private boolean retryWithoutPostcode(final DriverSummaryResponse driverSummaryResponse, final String postCode, final Map<String, Object> params) {
        return (isNull(driverSummaryResponse) || isNull(driverSummaryResponse.getResults())
                || nonNull(driverSummaryResponse.getError())
                || driverSummaryResponse.getResults().isEmpty())
                && nonNull(postCode) && params.size() > 4;
    }
}
