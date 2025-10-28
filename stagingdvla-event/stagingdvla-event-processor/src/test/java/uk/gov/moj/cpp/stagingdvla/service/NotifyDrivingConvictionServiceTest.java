package uk.gov.moj.cpp.stagingdvla.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.stagingdvla.notify.azure.DvlaApimConfig;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConviction;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConvictionResponse;
import uk.gov.moj.cpp.stagingdvla.notify.util.FileUtils;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotifyDrivingConvictionServiceTest {
    @InjectMocks
    private NotifyDrivingConvictionService notifyDrivingConvictionService;

    @Mock
    private DvlaApimConfig dvlaApimConfig;

    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Response apiResponse;

    @Mock
    private NotifyDrivingConvictionResponse notifyDrivingConvictionResponse;

    @Mock
    private ObjectMapper objMapper;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    // data
    private DriverNotified driverNotified;


    @BeforeEach
    public void setup() throws IOException, IllegalAccessException {
        when(dvlaApimConfig.getDvlaEnquiryApimUrl()).thenReturn("http://dvlaEnquiryApimUrl");
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json", "A11Y10M99W99D");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
    }

    @Captor
    private ArgumentCaptor<String> envelopeArgumentCaptor;

    @Test
    public void shouldNotifyOnNewEndorsementOk() throws IOException, URISyntaxException {
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");
        final NotifyDrivingConvictionResponse notifyDrivingConvictionResponse = prepareNotifyDrivingConvictionResponse(responseJson);

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);

        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));
        assertNull(convictionResponse.getErrors());

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence(), is(nullValue()));
    }

    @Test
    public void shouldNotifyOnNewEndorsementOkWith04Weeks() throws IOException, URISyntaxException {
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json", "A04W");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));
        assertNull(convictionResponse.getErrors());

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getWeeks(), is(4));
    }

    @Test
    public void shouldNotifyOnNewEndorsementOkWithMixedTrailingZeros() throws IOException, URISyntaxException {
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json", "A4M06W");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getMonths(), is(4));
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getWeeks(), is(nullValue()));
        assertNull(convictionResponse.getErrors());
    }

    @Test
    public void shouldNotifyOnNewEndorsementOkWithWeeksAndMonthsWithAllTrailingZeros() throws IOException, URISyntaxException {
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json", "A08M08W");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getMonths(), is(8));
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getWeeks(), is(nullValue()));
        assertNull(convictionResponse.getErrors());
    }

    @Test
    public void shouldNotifyOnNewEndorsementOkWithWeeksAndMonthsWithNoTrailingZeros() throws IOException, URISyntaxException {
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json",
                "A8M8W");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getMonths(), is(8));
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getWeeks(), is(nullValue()));
        assertNull(convictionResponse.getErrors());
    }

    @Test
    public void shouldNotifyOnNewEndorsementOkWithWeeksAndMonthsWithNoTrailingZerosJ000() throws IOException, URISyntaxException {
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json", "J000");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getOtherSentenceType(), is("Absolute Discharge"));
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getWeeks(), is(nullValue()));
        assertNull(convictionResponse.getErrors());
    }

    @Test
    public void shouldNotifyOnNewEndorsementOkWithWeeksAndMonthsWithNoTrailingZerosM000A0W() throws IOException, URISyntaxException {
        final JsonObject driverNotifiedPayload = buildDriverNotifiedJsonObject("stagingdvla.driver-notified.dvla-api.json", "M000A04W");
        driverNotified = jsonObjectToObjectConverter.convert(driverNotifiedPayload, DriverNotified.class);
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-ok.json");

        when(apiResponse.getStatus()).thenReturn(SC_OK);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);
        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_OK));

        verify(restEasyClientService).post(any(), envelopeArgumentCaptor.capture(), any());
        NotifyDrivingConviction notifyDrivingConviction = getNotifyDrivingConviction(envelopeArgumentCaptor.getValue());
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getOtherSentenceType(), is("Community Order"));
        assertThat(notifyDrivingConviction.getOffences().get(0).getOtherSentence().getWeeks(), is(nullValue()));
        assertNull(convictionResponse.getErrors());
    }

    @Test
    public void shouldNotifyOnNewEndorsementError() throws IOException, URISyntaxException {
        String responseJson = FileUtils.readFile("dvla.notify-driving-conviction-response-error.json");
        final NotifyDrivingConvictionResponse notifyDrivingConvictionResponse = prepareNotifyDrivingConvictionResponse(responseJson);

        when(apiResponse.readEntity(String.class)).thenReturn(responseJson);
        when(apiResponse.getStatus()).thenReturn(SC_BAD_REQUEST);
        when(restEasyClientService.post(any(), any(), any())).thenReturn(apiResponse);

        NotifyDrivingConvictionResponse convictionResponse = notifyDrivingConvictionService.notifyDrivingConviction(driverNotified);

        assertThat(convictionResponse.getStatus(), is(HttpStatus.SC_BAD_REQUEST));
        assertNotNull(convictionResponse.getErrors());
        assertThat(convictionResponse.getErrors().contains("END003"), is(true));
        assertThat(convictionResponse.getErrors().contains("END004"), is(true));
    }

    private NotifyDrivingConvictionResponse prepareNotifyDrivingConvictionResponse(final String fileContent) throws IOException {
        final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
        final NotifyDrivingConvictionResponse notifyDrivingConvictionResponse = objectMapper.reader()
                .forType(NotifyDrivingConvictionResponse.class)
                .readValue(fileContent);
        return notifyDrivingConvictionResponse;
    }

    private JsonObject buildDriverNotifiedJsonObject(String filename, String otherSentence) throws IOException {
        String inputPayload = Resources.toString(getResource(filename), defaultCharset());
        inputPayload = inputPayload.replace("IDENTIFIER_ID", randomUUID().toString())
                .replace("OTHER_SENTENCE", otherSentence);
        ;
        return stringToJsonObjectConverter.convert(inputPayload);
    }

    private NotifyDrivingConviction getNotifyDrivingConviction(String result) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        NotifyDrivingConviction notifyDrivingConviction = mapper.readValue(result, NotifyDrivingConviction.class);
        return notifyDrivingConviction;
    }
}