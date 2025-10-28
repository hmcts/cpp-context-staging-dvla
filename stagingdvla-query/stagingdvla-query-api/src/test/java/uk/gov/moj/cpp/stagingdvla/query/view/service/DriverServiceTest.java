package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_EXPECTATION_FAILED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.stagingdvla.query.utils.FileUtils;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverImageQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DriverSummaryQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverImageResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DriverSummaryResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DriverServiceTest {

    private static final String FIRST_NAMES = "Walter";
    private static final String LAST_NAME = "Harris";
    private static final String GENDER = "Male";
    private static final String POST_CODE = "SW1 4DK";
    private static final String DATE_OF_BIRTH = "1971-06-22";
    private static final String DRIVER_NUMBER = UUID.randomUUID().toString();
    private static final String LONG_DRIVER_NUMBER = "12345678901234567890";
    private static final String TRIMMED_DRIVER_NUMBER = "1234567890123456";
    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final String driverResponseFileName = "driverResponse.json";
    private static final String driverResponseSentenceDateFileName = "driverResponseSentenceDate.json";
    private static final String driverResponseSortOrder = "driverResponseSortOrder.json";
    private static final String driverSummaryResponseFileName = "driverSummaryResponse.json";
    private static final String driverResponseTokenValidityToTokenFileName = "driverResponseTokenValidityToToken.json";
    private static final String driverResponseNotFound = "driverResponseNotFound.json";
    private static final String driverImageResponseNotFound = "driverImageResponseNotFound.json";
    private static final String driverResponseBedRequest = "driverResponseBedRequest.json";
    private static final String driverResponseInternalServerError = "driverResponseInternalServerError.json";
    private static final String driverResponseForbidden = "driverResponseForbidden.json";
    public static final String TOTAL = "3";
    private static final String driverImageResponseFileName = "driverImageResponse.json";
    private static final String driverImageSignatureResponseFileName = "driverImageSignatureResponse.json";
    private static final String REQUIRED_IMAGE = "signature";

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @InjectMocks
    private DriverService driverService;

    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Response response;

    @Mock
    private ObjectMapper objMapper;

    @Captor
    private ArgumentCaptor<String> argumentCaptor;


    @BeforeEach
    public void setup() throws IllegalAccessException {
        FieldUtils.writeField(driverService, "dvlaEnquiryApimUrl", "dvlaEnquiryApimUrl", true);
    }

    @Test
    public void shouldReturnRecordWhenFindByDriverNumber() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseFileName);
        final DriverResponse driverResponse = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverResponse driverResponse1 = driverService.findByDriverNumber(queryParameters);
        assertThat(driverResponse1.getDriverRedirect(), is(driverResponse.getDriverRedirect()));
        assertThat(driverResponse1.getDriver().getFirstNames(), is(driverResponse.getDriver().getFirstNames()));
    }

    @Test
    public void shouldReturnRecordWhenFindByDriverNumberWithLongDriverNumber() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(LONG_DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseFileName);
        final DriverResponse driverResponse = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);

        driverService.findByDriverNumber(queryParameters);

        verify(restEasyClientService).post(any(), argumentCaptor.capture(), any());

        assertThat(argumentCaptor.getValue(), isJson(allOf(
                withJsonPath("$.drivingLicenceNumber", is(TRIMMED_DRIVER_NUMBER))
        )));
    }

    @Test
    void shouldReturnRecordWhenFindByDriverNumberSortedBySentenceDate() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseSentenceDateFileName);
        final DriverResponse driverResponse = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        when(objMapper.readValue(json, DriverResponse.class)).thenReturn(driverResponse);
        DriverResponse driverResponse1 = driverService.findByDriverNumber(queryParameters);
        assertEquals(5, driverResponse1.getEndorsements().size());
        assertNull(driverResponse1.getEndorsements().get(0).getSentenceDate());
        assertEquals(driverResponse1.getEndorsements().get(1).getSentenceDate(), LocalDate.of(2018, 05, 01));
        assertEquals(driverResponse1.getEndorsements().get(2).getSentenceDate(), LocalDate.of(2016, 04, 01));
        assertEquals(driverResponse1.getEndorsements().get(3).getSentenceDate(), LocalDate.of(2006, 04, 01));
        assertEquals(driverResponse1.getEndorsements().get(4).getSentenceDate(), LocalDate.of(2003, 04, 01));
    }

    @Test
    void shouldReturnRecordWithCorrectSortOrderSni_5910() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseSortOrder);
        final DriverResponse driverResponse = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverResponse driverResponse1 = driverService.findByDriverNumber(queryParameters);
        assertEquals(9, driverResponse1.getEndorsements().size());
        assertThat(driverResponse1.getEndorsements().get(0).getConvictionCourtCode(), is("CCC-6"));
        assertThat(driverResponse1.getEndorsements().get(1).getConvictionCourtCode(), is("CCC-9"));
        assertThat(driverResponse1.getEndorsements().get(2).getConvictionCourtCode(), is("CCC-8"));
        assertThat(driverResponse1.getEndorsements().get(3).getConvictionCourtCode(), is("CCC-7"));
        assertThat(driverResponse1.getEndorsements().get(4).getConvictionCourtCode(), is("CCC-4"));
        assertThat(driverResponse1.getEndorsements().get(5).getConvictionCourtCode(), is("CCC-3"));
        assertThat(driverResponse1.getEndorsements().get(6).getConvictionCourtCode(), is("CCC-2"));
        assertThat(driverResponse1.getEndorsements().get(7).getConvictionCourtCode(), is("CCC-5"));
        assertThat(driverResponse1.getEndorsements().get(8).getConvictionCourtCode(), is("CCC-1"));
    }
    @Test
    public void shouldFindByDriverNumberOkWithTokenValidityToToken() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseTokenValidityToTokenFileName);
        final DriverResponse driverResponse = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverResponse driverResponse1 = driverService.findByDriverNumber(queryParameters);
        assertThat(driverResponse1.getDriverRedirect(), is(driverResponse.getDriverRedirect()));
        assertThat(driverResponse1.getDriver().getFirstNames(), is(driverResponse.getDriver().getFirstNames()));
        assertThat(driverResponse1.getTokenValidity().getTokenValidFromDate(),equalTo(driverResponse.getTokenValidity().getTokenValidFromDate()));
        assertThat(driverResponse1.getTokenValidity().getTokenValidToDate(),equalTo(driverResponse.getTokenValidity().getTokenValidToDate()));
        assertThat(driverResponse1.getTokenValidity().getTokenIssueNumber(),is(driverResponse.getTokenValidity().getTokenIssueNumber()));
    }

    @Test
    public void shouldFindByDriverDetailsOk() throws IOException, URISyntaxException {
        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .firstNames(FIRST_NAMES)
                .lastName(LAST_NAME)
                .gender(GENDER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverSummaryResponseFileName);
        final DriverSummaryResponse driverSummary = prepareDriverSummaryResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(response.getHeaderString("x-total-count")).thenReturn(TOTAL);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverSummaryResponse driverSummaryResponse = driverService.findByDriverDetails(queryParameters);
        assertThat(driverSummaryResponse.getResults().size(), is(driverSummary.getResults().size()));
        assertThat(driverSummaryResponse.getCount(), is(TOTAL));

        verify(restEasyClientService).post(any(), argumentCaptor.capture(), any());

        assertThat(argumentCaptor.getValue(), isJson(allOf(
                withJsonPath("$.criteria.firstNames", is(FIRST_NAMES)),
                withJsonPath("$.criteria.lastName", is(LAST_NAME)),
                withJsonPath("$.criteria.gender", is(GENDER)),
                withoutJsonPath("$.options.firstNamesMatchType"),
                withoutJsonPath("$.options.lastNameMatchType")
        )));
    }

    @Test
    public void shouldFindByDriverDetailsWithExactFirstNamesMatch() throws IOException, URISyntaxException {
        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .firstNames(FIRST_NAMES)
                .lastName(LAST_NAME)
                .gender(GENDER)
                .caseId(CASE_ID)
                .exactFirstNamesMatch(true).build();
        String json = FileUtils.readFile(driverSummaryResponseFileName);
        final DriverSummaryResponse driverSummary = prepareDriverSummaryResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_OK);
        when(response.getHeaderString("x-total-count")).thenReturn(TOTAL);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverSummaryResponse driverSummaryResponse = driverService.findByDriverDetails(queryParameters);
        assertThat(driverSummaryResponse.getResults().size(), is(driverSummary.getResults().size()));
        assertThat(driverSummaryResponse.getCount(), is(TOTAL));

        verify(restEasyClientService).post(any(), argumentCaptor.capture(), any());

        assertThat(argumentCaptor.getValue(), isJson(allOf(
                withJsonPath("$.criteria.firstNames", is(FIRST_NAMES)),
                withJsonPath("$.criteria.lastName", is(LAST_NAME)),
                withJsonPath("$.criteria.gender", is(GENDER)),
                withJsonPath("$.options.firstNamesMatchType", is("exact")),
                withoutJsonPath("$.options.lastNameMatchType")
        )));
    }

    @Test
    public void shouldReturnExceptionWhenFindByDriverDetailsWithInvalidParameters() throws IOException, URISyntaxException {
        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .firstNames(FIRST_NAMES)
                .lastName(LAST_NAME)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverSummaryResponseFileName);
        final DriverSummaryResponse driverSummary = prepareDriverSummaryResponse(json);
        DriverSummaryResponse driverSummaryResponse = driverService.findByDriverDetails(queryParameters);
        assertThat(driverSummaryResponse.getError().getStatus(), is(Integer.toString(SC_EXPECTATION_FAILED)));
    }


    @Test
    public void shouldReturnNonFoundExceptionWhenFindByDriverNumber() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseNotFound);
        final DriverResponse driver = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_NOT_FOUND);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverResponse driverResponse = driverService.findByDriverNumber(queryParameters);
        assertNotNull(driverResponse.getError());
        assertThat(driverResponse.getError().getStatus(), is(Integer.toString(SC_NOT_FOUND)));
    }

    @Test
    public void shouldReturnBadRequestWhenFindByDriverNumber() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseBedRequest);
        final DriverResponse driver = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_BAD_REQUEST);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverResponse driverResponse = driverService.findByDriverNumber(queryParameters);
        assertNotNull(driverResponse.getError());
        assertThat(driverResponse.getError().getStatus(), is(Integer.toString(SC_BAD_REQUEST)));
    }

    @Test
    public void shouldReturnInternalServerErrorWhenFindByInvalidDriverNumber() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseInternalServerError);
        final DriverResponse driver = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_INTERNAL_SERVER_ERROR);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverResponse driverResponse = driverService.findByDriverNumber(queryParameters);
        assertNotNull(driverResponse.getError());
        assertThat(driverResponse.getError().getStatus(), is(Integer.toString(SC_INTERNAL_SERVER_ERROR)));
    }

    @Test
    public void shouldReturnForbiddenWhenFindByDriverNumber() throws IOException, URISyntaxException {
        final DriverQueryParameters queryParameters = new DriverQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .caseId(CASE_ID).build();
        String json = FileUtils.readFile(driverResponseForbidden);
        final DriverResponse driver = prepareDriverResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_FORBIDDEN);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        when(objMapper.readValue(json, DriverResponse.class)).thenReturn(driver);
        DriverResponse driverResponse = driverService.findByDriverNumber(queryParameters);
        assertNotNull(driverResponse.getError());
        assertThat(driverResponse.getError().getStatus(), is(Integer.toString(SC_FORBIDDEN)));
    }

    @Test
    public void shouldReturnNotFoundWhenFindByDriverDetailsWithOrWithoutPostCode() throws IOException, URISyntaxException {
        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .firstNames(FIRST_NAMES)
                .lastName(LAST_NAME)
                .postcode(POST_CODE)
                .dateOfBirth(DATE_OF_BIRTH).build();
        String json = FileUtils.readFile(driverResponseNotFound);
        final DriverSummaryResponse driverSummary = prepareDriverSummaryResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_BAD_REQUEST);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverSummaryResponse driverSummaryResponse = driverService.findByDriverDetails(queryParameters);
        assertNotNull(driverSummaryResponse.getError());
        assertThat(driverSummaryResponse.getError().getStatus(), is(Integer.toString(SC_NOT_FOUND)));
    }

    @Test
    public void shouldReturnNotFoundWhenFindByDriverDetailsWithNonMatchingPostCodeGenderDoB() throws IOException, URISyntaxException {
        final DriverSummaryQueryParameters queryParameters = new DriverSummaryQueryParameters.Builder()
                .gender(GENDER)
                .postcode(POST_CODE)
                .dateOfBirth(DATE_OF_BIRTH).build();
        String json = FileUtils.readFile(driverResponseNotFound);
        final DriverSummaryResponse driverSummary = prepareDriverSummaryResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_NOT_FOUND);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverSummaryResponse driverSummaryResponse = driverService.findByDriverDetails(queryParameters);
        assertNotNull(driverSummaryResponse.getError());
        assertThat(driverSummaryResponse.getError().getStatus(), is(Integer.toString(SC_NOT_FOUND)));
    }

    private DriverResponse prepareDriverResponse(final String fileContent) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        return objectMapper.reader()
                .forType(DriverResponse.class)
                .readValue(fileContent);
    }

    private DriverSummaryResponse prepareDriverSummaryResponse(final String fileContent) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        return objectMapper.reader()
                .forType(DriverSummaryResponse.class)
                .readValue(fileContent);
    }

    @Test
    public void shouldReturnDriverImageWhenFindByDriverNumber() throws IOException, URISyntaxException {
        final DriverImageQueryParameters queryParameters = new DriverImageQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .build();
        String json = FileUtils.readFile(driverImageResponseFileName);
        final DriverImageResponse driverImageResponse = prepareDriverImageResponse(json);
        ObjectMapper objectMapper = new ObjectMapper();
        String request = objectMapper.writeValueAsString(driverImageResponse);
        when(response.readEntity(String.class)).thenReturn(request);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverImageResponse driverImageResponse1 = driverService.findDriverImage(queryParameters);
        assertThat(driverImageResponse1.getPhotograph().getImage(), is(driverImageResponse.getPhotograph().getImage()));
        assertThat(driverImageResponse1.getPhotograph().getImageFormat(), is(driverImageResponse.getPhotograph().getImageFormat()));
        assertThat(driverImageResponse1.getSignature().getImage(), is(driverImageResponse.getSignature().getImage()));
        assertThat(driverImageResponse1.getSignature().getImageFormat(), is(driverImageResponse.getSignature().getImageFormat()));
    }

    @Test
    public void shouldReturnDriverImageWhenFindByDriverNumberWithLongDriverNumber() throws IOException, URISyntaxException {
        final DriverImageQueryParameters queryParameters = new DriverImageQueryParameters.Builder()
                .driverNumber(LONG_DRIVER_NUMBER)
                .build();
        String json = FileUtils.readFile(driverImageResponseFileName);
        final DriverImageResponse driverImageResponse = prepareDriverImageResponse(json);
        ObjectMapper objectMapper = new ObjectMapper();
        String request = objectMapper.writeValueAsString(driverImageResponse);
        when(response.readEntity(String.class)).thenReturn(request);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);

        driverService.findDriverImage(queryParameters);

        verify(restEasyClientService).post(any(), argumentCaptor.capture(), any());

        assertThat(argumentCaptor.getValue(), isJson(allOf(
                withJsonPath("$.drivingLicenceNumber", is(TRIMMED_DRIVER_NUMBER))
        )));
    }

    @Test
    public void shouldReturnNonFoundExceptionWhenFindDriverImageByDriverNumber() throws IOException, URISyntaxException {
        final DriverImageQueryParameters queryParameters = new DriverImageQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .build();
        String json = FileUtils.readFile(driverImageResponseNotFound);
        final DriverImageResponse driverImage = prepareDriverImageResponse(json);
        when(response.readEntity(String.class)).thenReturn(json);
        when(response.getStatus()).thenReturn(SC_NOT_FOUND);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverImageResponse driverImageResponse = driverService.findDriverImage(queryParameters);
        assertNotNull(driverImageResponse.getError());
        assertThat(driverImageResponse.getError().getStatus(), is(Integer.toString(SC_NOT_FOUND)));
    }

    @Test
    public void shouldReturnDriverImageWithOnlySignatureWhenFindByDriverNumber() throws IOException, URISyntaxException {
        final DriverImageQueryParameters queryParameters = new DriverImageQueryParameters.Builder()
                .driverNumber(DRIVER_NUMBER)
                .requiredImage(REQUIRED_IMAGE).build();
        String json = FileUtils.readFile(driverImageSignatureResponseFileName);
        final DriverImageResponse driverImageResponse = prepareDriverImageResponse(json);
        ObjectMapper objectMapper = new ObjectMapper();
        String request = objectMapper.writeValueAsString(driverImageResponse);
        when(response.readEntity(String.class)).thenReturn(request);
        when(response.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(response);
        DriverImageResponse driverImageResponse1 = driverService.findDriverImage(queryParameters);
        assertThat(driverImageResponse1.getSignature().getImage(), is(driverImageResponse.getSignature().getImage()));
        assertThat(driverImageResponse1.getSignature().getImageFormat(), is(driverImageResponse.getSignature().getImageFormat()));
    }

    private DriverImageResponse prepareDriverImageResponse(final String fileContent) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        return objectMapper.reader()
                .forType(DriverImageResponse.class)
                .readValue(fileContent);
    }

}