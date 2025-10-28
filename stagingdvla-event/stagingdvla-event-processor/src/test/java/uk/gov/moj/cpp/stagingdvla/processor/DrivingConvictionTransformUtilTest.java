package uk.gov.moj.cpp.stagingdvla.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.DEFAULT_DVLA_CODE;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.NO_DURATION_VALUE;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.ALCOHOL;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.EndorsementType.NEW;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.EndorsementType.REMOVE;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.EndorsementType.UPDATE;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.NONE_OR_UNKNOWN;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.getEndorsementType;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.getHighestMixedDurationPeriod;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.hasMultipleConvictingCourts;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.hasMultipleConvictionDates;
import static uk.gov.moj.cpp.stagingdvla.notify.util.DrivingConvictionTransformUtil.transformToNotifyDrivingConviction;

import uk.gov.justice.core.courts.nowdocument.Nowaddress;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.NotifyDrivingConviction;
import uk.gov.moj.cpp.stagingdvla.notify.driving.conviction.Offence;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.stagingdvla.processor.helper.MixedDurationHelper;

@ExtendWith(MockitoExtension.class)
public class DrivingConvictionTransformUtilTest {

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private DriverNotified driverNotified;

    private final String identifier = randomUUID().toString();

    private static final Pattern VALID_SINGLE_CHARACTER_PATTERN = Pattern.compile(
            "^[APCE](?:(?:[1-9]\\d?Y|[1-9]\\d?M|[1-9]\\d?W|[1-9]\\d?D|[1-9]\\d?H)){1,2}$"
    );

    private static final Pattern VALID_MULTIPLE_CHARACTER_PATTERN = Pattern.compile(
            "^[JM]000(?:(?:[1-9]\\d?Y|[1-9]\\d?M|[1-9]\\d?W|[1-9]\\d?D|[1-9]\\d?H)){1,2}$"
    );

    @BeforeEach
    public void setup() throws IOException {
        driverNotified = jsonObjectToObjectConverter.convert(getDriverNotifiedJson(), DriverNotified.class);
    }

    @Test
    public void shouldTransformConvictionAndOffences() {
        final NotifyDrivingConviction notifyDrivingConviction = transformToNotifyDrivingConviction(driverNotified);

        assertNotNull(notifyDrivingConviction.getConviction());
        assertNotNull(notifyDrivingConviction.getOffences());
        assertThat(notifyDrivingConviction.getOffences().size(), is(1));
    }

    @Test
    public void shouldTransformIdentifier() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        assertThat(transformed1.getConviction().getIdentifier(), is(identifier));

        final DriverNotified withNullIdentifer = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withIdentifier(null)
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withNullIdentifer);
        assertNotNull(transformed2.getConviction().getIdentifier());
    }

    @Test
    public void shouldTransformConvictingCourtCodeAndDate() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        assertThat(transformed1.getConviction().getConvictingCourtCode(), is("COURT_CODE"));
        assertThat(transformed1.getConviction().getConvictionDate(), is("2021-03-10"));

        final DriverNotified withNoConvictingCourt = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withConvictingCourtCode(null)
                                .withConvictionDate("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withNoConvictingCourt);
        assertNull(transformed2.getConviction().getConvictingCourtCode());
        assertNull(transformed2.getConviction().getConvictionDate());

        final DriverNotified withNoConvictionDate = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withConvictingCourtCode("")
                                .withConvictionDate(null)
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withNoConvictionDate);
        assertNull(transformed3.getConviction().getConvictingCourtCode());
        assertNull(transformed3.getConviction().getConvictionDate());
    }

    @Test
    public void shouldTransformLicenceData() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        assertThat(transformed1.getConviction().getDrivingLicenceNumber(), is("MORGA753116SMJIJ"));
        assertThat(transformed1.getConviction().getLicenceIssueNumber(), is("35"));
        assertThat(transformed1.getConviction().getLicenceProducedInCourt(), is("2: Full DVLA produced"));

        final DriverNotified withEmptyData = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withLicenceProducedInCourt("")
                .withDistinctPrompts(null)
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withEmptyData);
        assertNull(transformed2.getConviction().getDrivingLicenceNumber());
        assertNull(transformed2.getConviction().getLicenceIssueNumber());
        assertThat(transformed2.getConviction().getLicenceProducedInCourt(), is(NONE_OR_UNKNOWN));
    }

    @Test
    public void shouldTransformDefendantData() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        assertThat(transformed1.getConviction().getGender(), is("MALE"));
        assertThat(transformed1.getConviction().getDateOfBirth(), is("1983-03-07"));
        assertThat(transformed1.getConviction().getTitle(), is("Mr"));
        assertThat(transformed1.getConviction().getLastName(), is("John"));
        assertThat(transformed1.getConviction().getFirstNames(), is("Adam Mark"));

        final DriverNotified withEmptyData = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withDefendant(Nowdefendant.nowdefendant()
                        .withGender("")
                        .withDateOfBirth("")
                        .withTitle("")
                        .withLastName("")
                        .withMiddleName("")
                        .withFirstName("")
                        .build())
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withEmptyData);
        assertNull(transformed2.getConviction().getGender());
        assertThat(transformed2.getConviction().getDateOfBirth(), is(EMPTY));
        assertNull(transformed2.getConviction().getTitle());
        assertNull(transformed2.getConviction().getLastName());
        assertNull(transformed2.getConviction().getFirstNames());

        final DriverNotified withNoDefendant = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withDefendant(null)
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withNoDefendant);
        assertNull(transformed3.getConviction().getGender());
        assertThat(transformed3.getConviction().getDateOfBirth(), is(EMPTY));
        assertNull(transformed3.getConviction().getTitle());
        assertNull(transformed3.getConviction().getLastName());
        assertNull(transformed3.getConviction().getFirstNames());

        final DriverNotified withIncompatibleData = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withDefendant(Nowdefendant.nowdefendant()
                        .withGender("NOT_SPECIFIED")
                        .withDateOfBirth("2021.11.11")
                        .build())
                .build();

        final NotifyDrivingConviction transformed4 = transformToNotifyDrivingConviction(withIncompatibleData);
        assertNull(transformed4.getConviction().getGender());
        assertThat(transformed4.getConviction().getDateOfBirth(), is("2021.11.11"));
    }

    @Test
    public void shouldTransformAddressData() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        assertThat(transformed1.getConviction().getAddress().getLine1(), is("Flat 1"));
        assertThat(transformed1.getConviction().getAddress().getLine2(), is("11 Palace Court"));
        assertThat(transformed1.getConviction().getAddress().getLine3(), is("London"));
        assertThat(transformed1.getConviction().getAddress().getLine4(), is("England UK"));
        assertThat(transformed1.getConviction().getAddress().getPostcode(), is("W2 1AB12"));

        final DriverNotified withNoLine5 = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withDefendant(Nowdefendant.nowdefendant()
                        .withAddress(Nowaddress.nowaddress()
                                .withValuesFrom(driverNotified.getDefendant().getAddress())
                                .withLine1(null)
                                .withLine2("")
                                .withLine3(null)
                                .withLine4("England")
                                .withLine5(null)
                                .withPostCode(null)
                                .build())
                        .build())
                .build();
        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withNoLine5);
        assertThat(transformed2.getConviction().getAddress().getLine4(), is("England"));

        final DriverNotified withEmptyDataExceptLine5 = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withDefendant(Nowdefendant.nowdefendant()
                        .withAddress(Nowaddress.nowaddress()
                                .withLine1("")
                                .withLine2(null)
                                .withLine3("")
                                .withLine4(null)
                                .withLine5("UK")
                                .withPostCode("")
                                .build())
                        .build())
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withEmptyDataExceptLine5);
        assertThat(transformed3.getConviction().getAddress().getLine1(), is(EMPTY));
        assertNull(transformed3.getConviction().getAddress().getLine2());
        assertNull(transformed3.getConviction().getAddress().getLine3());
        assertThat(transformed3.getConviction().getAddress().getLine4(), is("UK"));
        assertNull(transformed3.getConviction().getAddress().getPostcode());
    }

    @Test
    public void shouldTransformOffenceCodeAndDate() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getOffenceCode(), is("SP30"));
        assertThat(offence1.getDateOfOffence(), is("2021-03-15"));

        final DriverNotified withNoDvlaCode = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDvlaCode("")
                                .withEndDate("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withNoDvlaCode);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertThat(offence2.getOffenceCode(), is(DEFAULT_DVLA_CODE));
        assertThat(offence2.getDateOfOffence(), is("2021-01-19"));
    }

    @Test
    public void shouldTransformOffenceFineAndPenaltyPoints() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getFine(), is(15000));
        assertThat(offence1.getPenaltyPoints(), is(12));

        final DriverNotified withNoFineOrPenaltyPoints = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withFine("")
                                .withPenaltyPoints("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withNoFineOrPenaltyPoints);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertNull(offence2.getFine());
        assertNull(offence2.getPenaltyPoints());
    }

    @Test
    public void shouldTransformOffenceTestingData() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getIntoxicantType(), is(ALCOHOL));
        assertThat(offence1.getTestingMethod(), is("method description"));
        assertThat(offence1.getTestingResultLevel().doubleValue(), is(Double.valueOf("123.45")));

        final DriverNotified withDifferentMethodCode = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withAlcoholReadingMethodCode("X")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withDifferentMethodCode);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertThat(offence2.getIntoxicantType(), is("method description"));
        assertNull(offence2.getTestingMethod());
        assertThat(offence2.getTestingResultLevel().doubleValue(), is(Double.valueOf("123.45")));

        final DriverNotified withNoTestingData = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withAlcoholReadingMethodCode("")
                                .withAlcoholReadingMethodDescription("")
                                .withAlcoholReadingAmount("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withNoTestingData);
        final Offence offence3 = transformed3.getOffences().get(0);
        assertNull(offence3.getIntoxicantType());
        assertNull(offence3.getTestingMethod());
        assertNull(offence3.getTestingResultLevel());
    }

    @Test
    public void shouldTransformOffenceDisqualification() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getDisqualification().getForLife(), is(false));
        assertThat(offence1.getDisqualification().getYears(), is(11));
        assertThat(offence1.getDisqualification().getMonths(), is(0));
        assertThat(offence1.getDisqualification().getDays(), is(0));
        assertThat(offence1.getDisqualification().getExtendedType(), is("DTETP"));

        final DriverNotified withMonth = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDisqualificationPeriod("000800")
                                .withDttpDtetp("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withMonth);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertThat(offence2.getDisqualification().getForLife(), is(false));
        assertThat(offence2.getDisqualification().getYears(), is(0));
        assertThat(offence2.getDisqualification().getMonths(), is(8));
        assertThat(offence2.getDisqualification().getDays(), is(0));
        assertNull(offence2.getDisqualification().getExtendedType());

        final DriverNotified withDay = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDisqualificationPeriod("000045")
                                .withDttpDtetp("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withDay);
        final Offence offence3 = transformed3.getOffences().get(0);
        assertThat(offence3.getDisqualification().getForLife(), is(false));
        assertThat(offence3.getDisqualification().getYears(), is(0));
        assertThat(offence3.getDisqualification().getMonths(), is(0));
        assertThat(offence3.getDisqualification().getDays(), is(45));
        assertNull(offence3.getDisqualification().getExtendedType());

        final DriverNotified withLifetime = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDisqualificationPeriod("999999")
                                .withDttpDtetp(null)
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed4 = transformToNotifyDrivingConviction(withLifetime);
        final Offence offence4 = transformed4.getOffences().get(0);
        assertThat(offence4.getDisqualification().getForLife(), is(true));
        assertNull(offence4.getDisqualification().getYears());
        assertNull(offence4.getDisqualification().getMonths());
        assertNull(offence4.getDisqualification().getDays());
        assertNull(offence4.getDisqualification().getExtendedType());

        final DriverNotified withEmpty = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDisqualificationPeriod("")
                                .withDttpDtetp(null)
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed5 = transformToNotifyDrivingConviction(withEmpty);
        final Offence offence5 = transformed5.getOffences().get(0);
        assertNull(offence5.getDisqualification());
    }

    @Test
    public void shouldTransformOffenceOtherSentence() {

        final DriverNotified withEmpty = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withOtherSentence("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed6 = transformToNotifyDrivingConviction(withEmpty);
        final Offence offence6 = transformed6.getOffences().get(0);
        assertNull(offence6.getOtherSentence());

        final DriverNotified withInvalidType = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withOtherSentence("Z23X")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed7 = transformToNotifyDrivingConviction(withInvalidType);
        final Offence offence7 = transformed7.getOffences().get(0);
        assertNull(offence7.getOtherSentence());

        final DriverNotified withNoDuration = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withOtherSentence("I000")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed8 = transformToNotifyDrivingConviction(withNoDuration);
        final Offence offence8 = transformed8.getOffences().get(0);
        assertThat(offence8.getOtherSentence().getOtherSentenceType(), is("No Separate Penalty"));
        assertNull(offence8.getOtherSentence().getYears());
        assertNull(offence8.getOtherSentence().getMonths());
        assertNull(offence8.getOtherSentence().getDays());
        assertNull(offence8.getOtherSentence().getHours());
    }

    @Test
    public void shouldTransformOffencePrisonSentenceSuspended() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getPrisonSentenceSuspended().getYears(), is(12));
        assertNull(offence1.getPrisonSentenceSuspended().getMonths());
        assertNull(offence1.getPrisonSentenceSuspended().getDays());

        final DriverNotified withMonth = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withSuspendedSentence("15M")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withMonth);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertNull(offence2.getPrisonSentenceSuspended().getYears());
        assertThat(offence2.getPrisonSentenceSuspended().getMonths(), is(15));
        assertNull(offence2.getPrisonSentenceSuspended().getDays());

        final DriverNotified withDay = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withSuspendedSentence("66D")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withDay);
        final Offence offence3 = transformed3.getOffences().get(0);
        assertNull(offence3.getPrisonSentenceSuspended().getYears());
        assertNull(offence3.getPrisonSentenceSuspended().getMonths());
        assertThat(offence3.getPrisonSentenceSuspended().getDays(), is(66));

        final DriverNotified withNoDuration = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withSuspendedSentence(NO_DURATION_VALUE)
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed4 = transformToNotifyDrivingConviction(withNoDuration);
        final Offence offence4 = transformed4.getOffences().get(0);
        assertNull(offence4.getPrisonSentenceSuspended());

        final DriverNotified withInvalidValue = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withSuspendedSentence("123")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed5 = transformToNotifyDrivingConviction(withInvalidValue);
        final Offence offence5 = transformed5.getOffences().get(0);
        assertNull(offence5.getPrisonSentenceSuspended());

        final DriverNotified withEmpty = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withSuspendedSentence("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed6 = transformToNotifyDrivingConviction(withEmpty);
        final Offence offence6 = transformed6.getOffences().get(0);
        assertNull(offence6.getPrisonSentenceSuspended());
    }

    @Test
    public void shouldTransformOffenceSentenceData() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getSentenceState(), is("Final"));
        assertThat(offence1.getSentencingCourtCode(), is("SNCC"));
        assertThat(offence1.getSentenceDate(), is("2021-05-10"));

        final DriverNotified withEmptyValues = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withInterimImposedFinalSentence("")
                                .withSentencingCourtCode("")
                                .withSentenceDate("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withEmptyValues);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertNull(offence2.getSentenceState());
        assertNull(offence2.getSentencingCourtCode());
        assertNull(offence2.getSentenceDate());

        final DriverNotified withNullValues = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withInterimImposedFinalSentence(null)
                                .withSentencingCourtCode(null)
                                .withSentenceDate(null)
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withNullValues);
        final Offence offence3 = transformed3.getOffences().get(0);
        assertNull(offence3.getSentenceState());
        assertNull(offence3.getSentencingCourtCode());
        assertNull(offence3.getSentenceDate());
    }

    @Test
    public void shouldTransformOffenceDisqDates() {
        final NotifyDrivingConviction transformed1 = transformToNotifyDrivingConviction(driverNotified);
        final Offence offence1 = transformed1.getOffences().get(0);
        assertThat(offence1.getDisqualificationRemovalDate(), is("2021-07-15"));
        assertThat(offence1.getDisqualificationSuspendedPendingAppealDate(), is("2021-03-20"));

        final DriverNotified withEmptyValues = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDateFromWhichDisqRemoved("")
                                .withDateDisqSuspendedPendingAppeal("")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed2 = transformToNotifyDrivingConviction(withEmptyValues);
        final Offence offence2 = transformed2.getOffences().get(0);
        assertNull(offence2.getDisqualificationRemovalDate());
        assertNull(offence2.getDisqualificationSuspendedPendingAppealDate());

        final DriverNotified withInvalidValues = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDateFromWhichDisqRemoved("20210505")
                                .withDateDisqSuspendedPendingAppeal("10082021")
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformed3 = transformToNotifyDrivingConviction(withInvalidValues);
        final Offence offence3 = transformed3.getOffences().get(0);
        assertThat(offence3.getDisqualificationRemovalDate(), is("20210505"));
        assertThat(offence3.getDisqualificationSuspendedPendingAppealDate(), is("10082021"));
    }

    @Test
    public void shouldGetEndorsementType() {
        final DriverNotified driverNotified1 = DriverNotified.driverNotified()
                .withUpdatedEndorsements(asList("NE98"))
                .withRemovedEndorsements(asList("NE98"))
                .build();
        assertThat(getEndorsementType(driverNotified1), is(REMOVE));

        final DriverNotified driverNotified2 = DriverNotified.driverNotified()
                .withUpdatedEndorsements(asList("NE98"))
                .withRemovedEndorsements(null)
                .build();
        assertThat(getEndorsementType(driverNotified2), is(UPDATE));

        final DriverNotified driverNotified3 = DriverNotified.driverNotified()
                .withRemovedEndorsements(asList("NE98"))
                .build();
        assertThat(getEndorsementType(driverNotified3), is(REMOVE));

        final DriverNotified driverNotified4 = DriverNotified.driverNotified()
                .withUpdatedEndorsements(null)
                .withRemovedEndorsements(null)
                .build();
        assertThat(getEndorsementType(driverNotified4), is(NEW));
    }

    @Test
    public void shouldGetHasMultipleConvictionsResult() {
        final DriverNotified driverNotified1 = DriverNotified.driverNotified()
                .withCases(asList(Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("C1").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("C1").build())).build(),
                        Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("c1").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("c1").build())).build())).build();

        assertThat(hasMultipleConvictionDates(driverNotified1), is(false));
        assertThat(hasMultipleConvictingCourts(driverNotified1), is(false));

        final DriverNotified driverNotified2 = DriverNotified.driverNotified()
                .withCases(asList(Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("C1").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("11/11/1111")
                                                .withConvictingCourtCode("c1").build())).build(),
                        Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("c2").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("c1").build())).build())).build();

        assertThat(hasMultipleConvictionDates(driverNotified2), is(true));
        assertThat(hasMultipleConvictingCourts(driverNotified2), is(true));

        final DriverNotified driverNotified3 = DriverNotified.driverNotified()
                .withCases(asList(Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("C1").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("")
                                                .withConvictingCourtCode("").build())).build(),
                        Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("10/10/1010")
                                                .withConvictingCourtCode("c1").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate(null)
                                                .withConvictingCourtCode(null).build())).build())).build();

        assertThat(hasMultipleConvictionDates(driverNotified3), is(false));
        assertThat(hasMultipleConvictingCourts(driverNotified3), is(false));

        final DriverNotified driverNotified4 = DriverNotified.driverNotified()
                .withCases(asList(Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                        .withConvictionDate("10/10/1010")
                                        .withConvictingCourtCode("c1").build())).build(),
                        Cases.cases().withDefendantCaseOffences(
                                asList(DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate("")
                                                .withConvictingCourtCode("").build(),
                                        DefendantCaseOffences.defendantCaseOffences()
                                                .withConvictionDate(null)
                                                .withConvictingCourtCode(null).build())).build())).build();

        assertThat(hasMultipleConvictionDates(driverNotified4), is(false));
        assertThat(hasMultipleConvictingCourts(driverNotified4), is(false));

        final DriverNotified driverNotified5 = DriverNotified.driverNotified()
                .withCases(asList(Cases.cases().withDefendantCaseOffences(
                        asList(DefendantCaseOffences.defendantCaseOffences().build())).build()))
                .build();

        assertThat(hasMultipleConvictionDates(driverNotified5), is(false));
        assertThat(hasMultipleConvictingCourts(driverNotified5), is(false));
    }

    @Test
    public void shouldThrowExceptionWhenDisqualificationPeriodMoreThan2Digit() {
        final DriverNotified withMonth = DriverNotified.driverNotified()
                .withValuesFrom(driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withDisqualificationPeriod("8000000")
                                .withDttpDtetp("")
                                .build()))
                        .build()))
                .build();

        assertThrows(IllegalStateException.class, () -> transformToNotifyDrivingConviction(withMonth));
    }

    @Test
    public void shouldGetOtherSentenceForImprisonmentForMixedPeriods() {
        for (int i = 0; i < 1500; i++) {
            String code = MixedDurationHelper.generateNextImprisonmentMixedPeriod(VALID_SINGLE_CHARACTER_PATTERN);
            if (code == null) {
                break;
            }

            final Offence offence = createOffenceForGivenOtherSentenceMixedPeriod(code);
            shouldAssertYearPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertMonthPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertWeekPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertDayPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertHourPeriodForGivenOtherSentenceType(offence, code);

        }
    }

    @Test
    public void shouldGetOtherSentenceForSuspendedPrisonSentenceForMixedPeriods() {
        for (int i = 0; i < 1500; i++) {
            String code = MixedDurationHelper.generateNextSuspendedPrisonSentenceMixedPeriod(VALID_SINGLE_CHARACTER_PATTERN);
            if (code == null) {
                break;
            }

            final Offence offence = createOffenceForGivenOtherSentenceMixedPeriod(code);
            shouldAssertYearPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertMonthPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertWeekPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertDayPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertHourPeriodForGivenOtherSentenceType(offence, code);

        }
    }

    @Test
    public void shouldGetOtherSentenceForConditionalDischargeForMixedPeriods() {
        for (int i = 0; i < 1500; i++) {
            String code = MixedDurationHelper.generateNextConditionalDischargeMixedPeriod(VALID_SINGLE_CHARACTER_PATTERN);
            if (code == null) {
                break;
            }

            final Offence offence = createOffenceForGivenOtherSentenceMixedPeriod(code);
            shouldAssertYearPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertMonthPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertWeekPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertDayPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertHourPeriodForGivenOtherSentenceType(offence, code);

        }
    }

    @Test
    public void shouldGetOtherSentenceForAbsoluteDischargeForMixedPeriods() {
        for (int i = 0; i < 1500; i++) {
            String code = MixedDurationHelper.generateNextAbsoluteDischargeMixedPeriod(VALID_MULTIPLE_CHARACTER_PATTERN);
            if (code == null) {
                break;
            }
            final Offence offence = createOffenceForGivenOtherSentenceMixedPeriod(code);
            if (offence.getOtherSentence() == null ){
                return;
            }
            shouldAssertYearPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertMonthPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertWeekPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertDayPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertHourPeriodForGivenOtherSentenceType(offence, code);

        }
    }

    @Test
    public void shouldGetOtherSentenceForCommunityOrderForMixedPeriods() {
        for (int i = 0; i < 1500; i++) {
            String code = MixedDurationHelper.generateNextCommunityOrderSentenceMixedPeriod(VALID_MULTIPLE_CHARACTER_PATTERN);
            if (code == null) {
                break;
            }
            final Offence offence = createOffenceForGivenOtherSentenceMixedPeriod(code);
            if (offence.getOtherSentence() == null ){
                return;
            }
            shouldAssertYearPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertMonthPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertWeekPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertDayPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertHourPeriodForGivenOtherSentenceType(offence, code);

        }
    }

    @Test
    public void shouldGetOtherSentenceForYouthCustodyForMixedPeriods() {
        for (int i = 0; i < 1500; i++) {
            String code = MixedDurationHelper.generateNextYouthCustodySentenceMixedPeriod(VALID_SINGLE_CHARACTER_PATTERN);
            if (code == null) {
                break;
            }
            final Offence offence = createOffenceForGivenOtherSentenceMixedPeriod(code);
            shouldAssertYearPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertMonthPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertWeekPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertDayPeriodForGivenOtherSentenceType(offence, code);
            shouldAssertHourPeriodForGivenOtherSentenceType(offence, code);

        }
    }

    private Offence createOffenceForGivenOtherSentenceMixedPeriod(String code) {
        final DriverNotified driverNotified = DriverNotified.driverNotified()
                .withValuesFrom(this.driverNotified)
                .withCases(asList(Cases.cases()
                        .withValuesFrom(this.driverNotified.getCases().get(0))
                        .withDefendantCaseOffences(asList(DefendantCaseOffences.defendantCaseOffences()
                                .withValuesFrom(this.driverNotified.getCases().get(0).getDefendantCaseOffences().get(0))
                                .withOtherSentence(code)
                                .build()))
                        .build()))
                .build();

        final NotifyDrivingConviction transformedToNotifyDrivingConviction = transformToNotifyDrivingConviction(driverNotified);

        return transformedToNotifyDrivingConviction.getOffences().get(0);
    }

    private void shouldAssertYearPeriodForGivenOtherSentenceType(Offence offence, String code) {
        Integer years = offence.getOtherSentence().getYears();
        Integer months = offence.getOtherSentence().getMonths();
        Integer weeks = offence.getOtherSentence().getWeeks();
        Integer days = offence.getOtherSentence().getDays();
        Integer hours = offence.getOtherSentence().getHours();

        if (Objects.nonNull(years)) {
            String splitNumericFromPeriod = getHighestMixedDurationPeriod(code);
            if (Objects.nonNull(splitNumericFromPeriod)) {
                Integer numericMixedPeriodValue = getNumericFromHighestPeriod(splitNumericFromPeriod);
                assertEquals(years, numericMixedPeriodValue);
                assertTrue(Objects.isNull(months));
                assertTrue(Objects.isNull(weeks));
                assertTrue(Objects.isNull(days));
                assertTrue(Objects.isNull(hours));
            }
        }
    }

    private void shouldAssertMonthPeriodForGivenOtherSentenceType(Offence offence, String code) {
        Integer years = offence.getOtherSentence().getYears();
        Integer months = offence.getOtherSentence().getMonths();
        Integer weeks = offence.getOtherSentence().getWeeks();
        Integer days = offence.getOtherSentence().getDays();
        Integer hours = offence.getOtherSentence().getHours();

        if (Objects.nonNull(months)) {
            String splitNumericFromPeriod = getHighestMixedDurationPeriod(code);
            if (Objects.nonNull(splitNumericFromPeriod)) {
                Integer numericMixedPeriodValue = getNumericFromHighestPeriod(splitNumericFromPeriod);
                assertEquals(months, numericMixedPeriodValue);
                assertTrue(Objects.isNull(years));
                assertTrue(Objects.isNull(weeks));
                assertTrue(Objects.isNull(days));
                assertTrue(Objects.isNull(hours));

            }
        }
    }

    private void shouldAssertWeekPeriodForGivenOtherSentenceType(Offence offence, String code) {
        Integer years = offence.getOtherSentence().getYears();
        Integer months = offence.getOtherSentence().getMonths();
        Integer weeks = offence.getOtherSentence().getWeeks();
        Integer days = offence.getOtherSentence().getDays();
        Integer hours = offence.getOtherSentence().getHours();

        if (Objects.nonNull(weeks)) {
            String splitNumericFromPeriod = getHighestMixedDurationPeriod(code);
            if (Objects.nonNull(splitNumericFromPeriod)) {
                Integer numericMixedPeriodValue = getNumericFromHighestPeriod(splitNumericFromPeriod);
                assertEquals(weeks, numericMixedPeriodValue);
                assertTrue(Objects.isNull(years));
                assertTrue(Objects.isNull(months));
                assertTrue(Objects.isNull(days));
                assertTrue(Objects.isNull(hours));

            }
        }
    }

    private void shouldAssertDayPeriodForGivenOtherSentenceType(Offence offence, String code) {
        Integer years = offence.getOtherSentence().getYears();
        Integer months = offence.getOtherSentence().getMonths();
        Integer weeks = offence.getOtherSentence().getWeeks();
        Integer days = offence.getOtherSentence().getDays();
        Integer hours = offence.getOtherSentence().getHours();

        if (Objects.nonNull(days)) {
            String splitNumericFromPeriod = getHighestMixedDurationPeriod(code);
            if (Objects.nonNull(splitNumericFromPeriod)) {
                Integer numericMixedPeriodValue = getNumericFromHighestPeriod(splitNumericFromPeriod);
                assertEquals(days, numericMixedPeriodValue);
                assertTrue(Objects.isNull(years));
                assertTrue(Objects.isNull(weeks));
                assertTrue(Objects.isNull(months));
                assertTrue(Objects.isNull(hours));

            }
        }
    }

    private void shouldAssertHourPeriodForGivenOtherSentenceType(Offence offence, String code) {
        Integer years = offence.getOtherSentence().getYears();
        Integer months = offence.getOtherSentence().getMonths();
        Integer weeks = offence.getOtherSentence().getWeeks();
        Integer days = offence.getOtherSentence().getDays();
        Integer hours = offence.getOtherSentence().getHours();

        if (Objects.nonNull(hours)) {
            String splitNumericFromPeriod = getHighestMixedDurationPeriod(code);
            if (Objects.nonNull(splitNumericFromPeriod)) {
                Integer numericMixedPeriodValue = getNumericFromHighestPeriod(splitNumericFromPeriod);
                assertEquals(hours, numericMixedPeriodValue);
                assertTrue(Objects.isNull(years));
                assertTrue(Objects.isNull(months));
                assertTrue(Objects.isNull(weeks));
                assertTrue(Objects.isNull(days));
            }
        }
    }


    private Integer getNumericFromHighestPeriod(String period) {
        if (period.length() == 3) {
            return Integer.valueOf(period.substring(1, 2));
        } else if (period.length() == 4) {
            return Integer.valueOf(period.substring(1, 3));
        }
        return 0;
    }


    private JsonObject getDriverNotifiedJson() throws IOException {
        final String inputPayload = Resources.toString(getResource("stagingdvla.driver-notified.dvla-api.json"), defaultCharset())
                .replace("IDENTIFIER_ID", identifier);

        return stringToJsonObjectConverter.convert(inputPayload);
    }
}
