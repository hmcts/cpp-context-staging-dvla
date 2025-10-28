package uk.gov.moj.stagingdvla.it;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;
import static uk.gov.moj.stagingdvla.it.DriverNotifiedEventAssertion.EMPTY_STRING;
import static uk.gov.moj.stagingdvla.stubs.DVLANotificationStub.verifyDVLANotificationCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.stubGenerateDocument;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.verifyGenerateDocumentStubCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.ProgressionStub.stubProgressionAddCourtDocument;
import static uk.gov.moj.stagingdvla.util.QueueUtil.privateEvents;
import static uk.gov.moj.stagingdvla.util.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.stagingdvla.util.RestHelper.postCommandWithUserId;
import static uk.gov.moj.stagingdvla.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.stagingdvla.util.FileUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DvlaNotificationScenariosIT extends AbstractIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DvlaNotificationScenariosIT.class.getName());

    public static final String USER_GROUP = UUID.randomUUID().toString();
    private String hearingId;
    private String defendantId;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);
    private final MessageConsumer consumerForDriverNotified = privateEvents.createPrivateConsumer("stagingdvla.event.driver-notified");
    private final String DRIVER_NOTIFICATION_MEDIA_TYPE = "application/vnd.stagingdvla.command.driver-notification+json";

    private static final String STAGINGDVLA_CONTEXT = "stagingdvla";

    private final Matcher NOT_NULL_VALUE = notNullValue();
    private final Matcher NULL_VALUE = nullValue();


    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP), "stub-data/usersgroups.get-specific-groups-by-user.json");
        stubDocumentCreate("Dummy");
        stubGenerateDocument("Dummy");
        stubProgressionAddCourtDocument();
    }

    @BeforeEach
    public void setup() {
        defendantId = randomUUID().toString();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());

        final ImmutableMap<String, Boolean> features = of("driverOut", true);
        stubFeaturesFor(STAGINGDVLA_CONTEXT, features);

        LOGGER.info("Running scenario with Defendant Id: " + defendantId);
    }


    // D20 Simple scenarios - start

    /**
     * Given that a case is created in CC
     * And has an endorsable offences (more than 1)
     * And is listed for a court hearing
     * When one or more than one offence has an endorsable result  and is shared
     * Then trigger D20 API notification
     */
    @Test
    public void simpleScenario1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("simple/scenario1/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW61403840")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-11")
                .hasAmountOfFine("£100.00")
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }
    /**
     * Given that a case is created in CC
     * And has 2 endorsable offences
     * And is listed for a court hearing
     * When in first hearing add guilty plea on 1 offence and adjourn result on all offences
     * And in subsequent hearing
     * On the 2nd offence add guilty plea and sentence both offences (endorsement result)
     * Then we should generate new D20 pdf showing different conviction date
     */
    @Test
    public void simpleScenario2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("simple/scenario2/command1.json",  0);

        driverNotifiedList = sendAndVerifyEvent("simple/scenario2/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW27593102")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("000300")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-29")
                .hasAmountOfFine("£100.00")
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Given that a case is created in CC
     * And has 2 endorsable offences
     * And is listed for a court hearing
     * When in first hearing add guilty plea on 1 offence and adjourn result (to a different court) on all offences
     * And in subsequent hearing
     * On the 2nd offence add guilty plea and sentence both offences (endorsement result)
     * Then we should generate new D20 pdf showing different conviction dates and courts
     */
    @Test
    public void simpleScenario3() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("simple/scenario3/command1.json",  0);
        driverNotifiedList = sendAndVerifyEvent("simple/scenario3/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("10AP830016")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode("RT88007")
                .hasDVLACode("DR10")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-11")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Alcohol limit")
                .hasOffenceCode(2,"RT88319")
                .hasDVLACode(2,"IN14")
                .hasDisqualificationPeriod(2,"010000")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,EMPTY_STRING)
                .hasConvictingCourt(2,"2576")
                .hasConvictionDate(2,"2023-04-10")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasResults(2,2)
                .hasWording(2,"Driving Insurance");

        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Given that a case is created in CC
     * And has an endorsable offences (more than 1)
     * And is listed for a court hearing
     * When one of offence has an interim disqualification endorsement result and an adjourn result  and is shared
     * *** Need to record the guilty plea in order to include the conviction tags or finding of guilt ***
     * Then trigger D20 API notification
     */
    @Test
    public void simpleScenario4() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("simple/scenario4/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW89117379")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-21")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(5)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    // D20 Simple scenarios - End

    // D20 Scenarios for match defendant cases in same hearing

    /**
     * Given that 2 or more cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all cases are listed in same hearing
     * When an endorsement result is entered against offences on all cases and shared
     * Then trigger D20 API notification for each case
     */
    @Test
    public void matchedDefendantSameHearingScenario1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario1/command1.json",  2);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("10AP830022")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88007")
                .hasDVLACode("DR10")
                .hasDisqualificationPeriod("000300")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2576")
                .hasConvictionDate("2023-04-11")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Alcohol limit")
                .hasResults(2)
                .hasOffenceCode(2,"RT88319")
                .hasDisqualificationPeriod(2,"000300")
                .hasDVLACode(2,"IN14")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,EMPTY_STRING)
                .hasConvictingCourt(2,"2576")
                .hasConvictionDate(2,"2023-04-11")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Driving Insurance");

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(1))
                .hasCaseReference("10AP830023")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod("000300")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2576")
                .hasConvictionDate("2023-04-11")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Driving Insurance")
                .hasResults(2)
                .hasOffenceCode(2,"RT88007")
                .hasDisqualificationPeriod(2,"000300")
                .hasDVLACode(2,"DR10")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,EMPTY_STRING)
                .hasConvictingCourt(2,"2576")
                .hasConvictionDate(2,"2023-04-11")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Alcohol limit")
                .hasResults(2,2);

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in same hearing
     * When an endorsement result is entered against offences on 2 cases
     * And a non endorsement result entered on 1 case offences (3rd one)
     * And shared
     * Then trigger  D20 API notification  for each case for offences which had an endorsement result (2 D20)
     * And do NOT trigger D20 for the case which had no endorsement result
     */
    @Test
    public void matchedDefendantSameHearingScenario2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario2/command1.json", 0);
        sendAndVerifyEvent("matchedDefendantSameHearing/scenario2/command2.json", 0);
        sendAndVerifyEvent("matchedDefendantSameHearing/scenario2/command3.json", 0);
        sendAndVerifyEvent("matchedDefendantSameHearing/scenario2/command4.json", 0);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario2/command5.json", 2);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("14AP830005")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88007")
                .hasDVLACode("DR10")
                .hasDisqualificationPeriod("000800")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-15")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Alcohol limit");


        DriverNotifiedEventAssertion.with(driverNotifiedList.get(1))
                .hasCaseReference("14AP830004")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88340")
                .hasDVLACode("DR80")
                .hasDisqualificationPeriod("000400")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-15")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Driving Insurance")
                .hasResults(2);

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Scenario 3
     * Given that 2 or more cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in same hearing
     * And an endorsement result is entered against offences on all cases and shared
     * And D20 API notification generated for each case
     * When the HMTC user amends the endorsement result on same offence which had an endorsement result, on few cases but not all
     * And re-shares
     * Then generate an update D20 for each Case which had endorsement result amended
     * And do NOT generate update/removal for any Case where the endorsement result was not amended
     */

    @Test
    public void matchedDefendantSameHearingScenario3() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario3/command1.json", 2);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW156454954")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("1")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(1))
                .hasCaseReference("JW156461575")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario3/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW156461575")
                .hasCourtApplications(0)
                .hasUpdatedEndorsementContains("IN14")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario3/command3.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW156461575")
                .hasCourtApplications(0)
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("IN14")
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     *Scenario 4
     * Given that 2 or more cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in same hearing
     * And an endorsement result is entered against offences on all cases and shared
     * And D20 API notification generated for each case
     * When the HMTC user removes the endorsement result on all offences in one case which had an endorsement result
     * e.g. enters a result FINE
     * And re-shares
     * Then generate a removal D20 for that 1 Case which had endorsement result removed
     * And do NOT generate update/removal for any Case where the endorsement result was not removed
     */
    @Test
    public void matchedDefendantSameHearingScenario4() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario4/command1.json", 2);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW182216687")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(1))
                .hasCaseReference("JW182226118")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario4/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW182216687")
                .hasCourtApplications(0)
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     *Scenario 5
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in same hearing
     * When an endorsement result is entered against offences on 2 cases
     * And a non endorsement result entered on 1 case (3rd one) e.g. Withdrawn
     * And shared
     * And D20 API notification generated for each case with endorseable result
     * When the HMTC user amends a NON endorsement result on same offence which had a non endorsement result previously, for the 3rd case e.g. Dismissed
     * And re-shares
     * Then do NOT generate any update/removal D20 for any Case
     */
    @Test
    public void matchedDefendantSameHearingScenario5() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario5/command1.json", 2);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW183165878")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(1))
                .hasCaseReference("JW183162908")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        sendAndVerifyEvent("matchedDefendantSameHearing/scenario5/command2.json", 0);
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     *Scenario 6
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in same hearing
     * When an endorsement result is entered against offences on 2 cases
     * And a non endorsement result entered on 1 case (3rd one) e.g. Withdrawn
     * And shared
     * And D20 API notification generated for each case with endorseable result
     * When the HMTC user amends a NON endorsement result on same offence which had a non endorsement result previously, for the 3rd case
     * And add an endorsement result
     * And re-shares
     * Then generate a D20 API notification for the 3rd case ONLY
     */
    @Test
    public void matchedDefendantSameHearingScenario6() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario6/command1.json", 2);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW184580177")
                .hasCourtApplications(0)
                .hasUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(1))
                .hasCaseReference("JW184582658")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantSameHearing/scenario6/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW184570878")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDrugLevel("500")
                .hasPenaltyPoints("4")
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    // D20 Scenarios for match defendant cases in same hearing - End

    // D20 Scenarios for match defendant cases in different hearing - Start

    /**
     * Scenario 1
     *
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in different hearing
     * When an endorsement result is entered against offences on each case in each hearing and shared
     * Then generate a D20 API notification for each case
     *
     * @throws IOException
     */
    @Test
    public void matchedDefendantDifferentHearingScenario1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario1/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW153140187")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("1")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario1/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW153157041")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario1/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW153162473")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 2
     *
     * Given that 3 cases are created in CC with defendant matching
     * And all are listed in different hearing
     * And 2 have an endorsable offence (e.g. excess alcohol)
     * When an endorsement result is entered against offences on first 2 cases and shared
     * Then generate a D20 API notification for first 2 case
     * And 1 has a non endorseable offence (e.g. theft)
     * When a non endorsable result is entered on 3rd case (theft)  and shared
     * Then do NOT generate any update/removal D20 for first 2 cases or API notification
     *
     * @throws IOException
     */
    @Test
    public void matchedDefendantDifferentHearingScenario2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario2/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW161457775")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario2/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW161460500")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario2/command3.json",  0);
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 3
     *
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in different hearing
     * And an endorsement result is entered against offences on each of the cases in each hearing and shared
     * And a D20 generated for each case
     * When the HMTC user amends the endorsement result (e.g. 2 months to 6 months) on any offence on any Case which had an endorsement result previously but not ALL cases
     * And re-shares
     * Then generate an update D20 for only the Cases which had endorsement result amended
     * And there should not be any update/removal for other cases where there was no amendment
     *
     * @throws IOException
     */
    @Test
    public void matchedDefendantDifferentHearingScenario3() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario3/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW163988054")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("1")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario3/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW164036715")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario3/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW164014801")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario3/command4.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW163988054")
                .hasUpdatedEndorsementContains("IN14")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("4")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 4
     *
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in different hearing
     * And an endorsement result is entered against offences on all cases in each hearing and shared
     * And a D20 generated for each case
     * When the HMTC user removes the endorsement result on any offence on any Case which had an endorsement result previously and enters a non endorsable result e.g. Fine
     * And re-shares
     * Then generate a removal D20 for only the Cases which had endorsement result removed
     * And there should not be any update/removal for other cases where there was no amendments
     *
     * @throws IOException
     */
    @Test
    public void matchedDefendantDifferentHearingScenario4() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario4/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW165867388")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("1")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario4/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW165888703")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario4/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW165905894")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario4/command4.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW165867388")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("IN14")
                .hasCourtApplications(0)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 5
     *
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in different hearing
     * When an endorsement result is entered against offences on 2 cases
     * And a non endorsement result entered on 1 case offences
     * And a D20 generated for each case with endorsement result
     * When the HMTC user amends a NON endorsement result on same offence which had a non endorsement result previously, for any Case
     * And re-shares
     * Then do NOT generate any update/removal D20 for any Case
     * @throws IOException
     */
    @Test
    public void matchedDefendantDifferentHearingScenario5() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario5/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW169267278")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario5/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW169471965")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario5/command3.json",  0);
        sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario5/command4.json",  0);
    }

    /**
     * Scenario 6
     *
     * Given that 3 cases are created in CC with defendant matching
     * And all have an endorsable offence
     * And all are listed in different hearing
     * When an endorsement result is entered against offences on 2 cases
     * And a non endorsement result entered on 1 case offences
     * And a D20 generated for each case with endorsement result
     * When the HMTC user adds an endorsement result on same offence which had a non endorsement result previously, for any Case
     * And re-shares
     * Then trigger API D20 notification for that case
     * Then do NOT generate any update/removal D20 for other Cases which had no amendments
     * @throws IOException
     */
    @Test
    public void matchedDefendantDifferentHearingScenario6() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario6/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW170824348")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario6/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW170834544")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario6/command3.json",  0);

        driverNotifiedList = sendAndVerifyEvent("matchedDefendantDifferentHearing/scenario6/command4.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW170846999")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    // D20 Scenarios for match defendant cases in different hearing - End

    // D20 Scenarios for appeal amend and reshare - Start

    /**
     * D20 endorsement information - Updated endorsements where Sentence Varied
     *
     * GIVEN I am applying results to an appeal / Application
     * AND I am applying an endorsement result to an offence
     * AND the offence had an endorsement result applied and shared in a previous hearing
     * AND the Sentence Varied result is being applied to the offence
     * WHEN the endorsement notification is generated
     * THEN the D20 pdf- Offences subsection contains information from the the newly applied endorsement results
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW25396781")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW25396781")
                .hasUpdatedEndorsementContains("TS10")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(4)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * D20 endorsement information - Updated endorsements where Sentence Varied
     *
     * GIVEN I am applying results to an appeal / Application
     * AND the previous result had a disqualification pending appeal (DSPA) - DDO 12 months
     * AND I am applying an endorsement result to an offence
     * AND the offence had an endorsement result applied and shared in a previous hearing
     * AND the Sentence Varied result is being applied to the offence (SV in the offences)
     *
     * AND disqualification period is updated to 6 months
     * WHEN the endorsement notification is generated
     * THEN the D20 pdf- Offences subsection contains information from the the newly applied endorsement results
     * AND the disqualification suspended pending appeal date is the date from the previous notification (if applicable)
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario1a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW97425483")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("001200")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1a/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW97425483")
                .hasCourtApplications(1)
                .hasUpdatedEndorsementContains("TS10")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("001200")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * D20 endorsement information - Updated endorsements where Sentence Varied
     *
     * GIVEN I am applying results to an appeal / Application
     * AND the previous result had a disqualification pending appeal (DSPA) - DDO 12 months
     * AND I am applying an endorsement result to an offence
     * AND the offence had an endorsement result applied and shared in a previous hearing
     * AND the Sentence Varied result is being applied to the offence (SV in the offences)
     * AND disqualification period is withdrawn
     * WHEN the endorsement notification is generated
     * THEN the D20 pdf- Offences subsection contains information from the the newly applied endorsement results
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario1b() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1b/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW97893281")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("021220")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1b/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW97893281")
                .hasCourtApplications(1)
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * D20 endorsement information - Removal endorsements where Sentence Varied
     *
     * GIVEN I am applying results to an appeal / Application
     * AND the previous result had a disqualification pending appeal (DSPA) - DDO 12 months
     * AND I am applying NOT applying an endorsement result to an offence
     * AND the offence had an endorsement result applied and shared in a previous hearing
     * AND the Sentence Varied result is being applied to the offence (SV in the offences) and Dismissed
     * WHEN the endorsement notification is generated
     * THEN the D20 pdf- Offences subsection contains information from the newly applied endorsement results
     *
     * @throws IOException
     */

    @Test
    public void appealAmendReshareScenario1c() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1c/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW31113590")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(4)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario1c/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW31113590")
                .hasCourtApplications(1)
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Points / totting disqualification
     *
     * GIVEN I am applying results to an appeal / Application
     * AND I am applying an endorsement result that has a 'points disqualification code' value i.e. 9 points and a TOTTER
     * AND the offence had an endorsement result applied and shared in a previous hearing
     * AND the Sentence Varied result is being applied to the offence with a different points disqualification value i.e. 10 points and a TOTTER
     * WHEN the results are reshared
     * THEN the D20 pdf - offence information includes 2 entries :
     *
     *     the offence DVLA code and details that triggered the points
     *     the TT99 DVLA code containing the disqualification details
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario2/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW11603859")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("9")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario2/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW11603859")
                .hasUpdatedEndorsementContains("TS10")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(2)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Points / totting disqualification
     *
     * GIVEN I am applying results to an appeal / Application
     * AND I am applying an endorsement result that has a 'points disqualification code' value i.e. 9 points and a TOTTER
     * AND the offence had an endorsement result applied and shared in a previous hearing
     * AND the Sentence Varied result is being applied to the offence with a different points disqualification value i.e. 3 points and no longer a TOTTER
     * WHEN the results are reshared
     * THEN the Remove D20 pdf - offence information includes 1 entry :
     *
     *     the offence DVLA code and the details of the points endorsement.
     *     Including updated offence details
     *     Including removed offence details
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario2a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario2a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88971TT99")
                .hasDisqualificationPeriod(2,"001200")
                .hasDVLACode(2,"TT99")
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario2a/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasCourtApplications(1)
                .hasUpdatedEndorsementContains("TS10")
                .hasRemovedEndorsementContains("TT99")
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(4)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    @Test
    void appealAmendReshare_DD32212_scenario1_regression() throws IOException {
        sendAndVerifyEvent("appealAmendReshare/DD-32212/command1.json",  1);
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
        sendAndVerifyEvent("appealAmendReshare/DD-32212/command2.json",  0);
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
    }

    @Test
    public void appealAmendReshare_DD27008_scenario1_regression() throws IOException {
        sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario1/command1.json",  0);

        sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario1/command2.json",  0);

        List<DriverNotified> firstNotice = sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario1/command3.json",  1);
        DriverNotifiedEventAssertion.with(firstNotice.get(0))
                .hasCaseReference("DVLA02022364")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("010000")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("£121.00")
                .hasResults(5)
                .hasConvictionDate("2024-10-04")
                .hasConvictingCourt("2570")
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(firstNotice);
        verifyGenerateDocumentStubCommandInvoked(firstNotice);


        List<DriverNotified> secondNotice = sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario1/command4.json",  1);
        DriverNotifiedEventAssertion.with(secondNotice.get(0))
                .hasCaseReference("DVLA02022364")
                .hasUpdatedEndorsementContains("SP50")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("010000")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("£121.00")
                .hasResults(5)
                .hasConvictionDate("2024-10-04")
                .hasConvictingCourt("2570")
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(secondNotice);
        verifyGenerateDocumentStubCommandInvoked(secondNotice);
    }

    @Test
    public void appealAmendReshare_sni_6564_scenario1() throws IOException {
        sendAndVerifyEvent("appealAmendReshare/sni-6564/command1.json",  0);

        sendAndVerifyEvent("appealAmendReshare/sni-6564/command2.json",  0);

        List<DriverNotified> firstNotice = sendAndVerifyEvent("appealAmendReshare/sni-6564/command3.json",  1);
        DriverNotifiedEventAssertion.with(firstNotice.get(0))
                .hasCaseReference("DVLA02022358")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("010101")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("??12.00")
                .hasResults(4)
                .hasConvictionDate("2024-09-20")
                .hasConvictingCourt("2570")
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates??? court was removed ??? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(firstNotice);
        verifyGenerateDocumentStubCommandInvoked(firstNotice);

        List<DriverNotified> secondNotice = sendAndVerifyEvent("appealAmendReshare/sni-6564/command4.json",  1);
        DriverNotifiedEventAssertion.with(secondNotice.get(0))
                .hasCaseReference("DVLA02022358")
                .hasUpdatedEndorsementContains("SP50")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("010101")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("??12.00")
                .hasResults(4)
                .hasConvictionDate("2024-09-20")
                .hasConvictingCourt("2570")
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates??? court was removed ??? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(secondNotice);
        verifyGenerateDocumentStubCommandInvoked(secondNotice);

    }

    @Test
    public void appealAmendReshare_DD27008_scenario2_prod_issue() throws IOException {
        List<DriverNotified> firstNotice = sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario2/command1.json",  1);
        DriverNotifiedEventAssertion.with(firstNotice.get(0))
                .hasCaseReference("DVLA02022362")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("£1111.00")
                .hasResults(4)
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(firstNotice);
        verifyGenerateDocumentStubCommandInvoked(firstNotice);

        List<DriverNotified> secondNotice = sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario2/command2.json",  1);
        DriverNotifiedEventAssertion.with(secondNotice.get(0))
                .hasCaseReference("DVLA02022362")
                .hasUpdatedEndorsementContains("SP50")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("£1111.00")
                .hasResults(4)
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(secondNotice);
        verifyGenerateDocumentStubCommandInvoked(secondNotice);

        List<DriverNotified> thirdNotice = sendAndVerifyEvent("appealAmendReshare/dd-27008/scenario2/command3.json",  1);
        DriverNotifiedEventAssertion.with(thirdNotice.get(0))
                .hasCaseReference("DVLA02022362")
                .hasUpdatedEndorsementContains("SP50")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel(null)
                .hasPenaltyPoints(EMPTY_STRING)
                .hasAmountOfFine("£1111.00")
                .hasResults(4)
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        verifyDVLANotificationCommandInvoked(thirdNotice);
        verifyGenerateDocumentStubCommandInvoked(thirdNotice);

    }

    /**
     * Removal Scenarios
     *
     * Endorsement Removed following appeal - Sentence Varied
     * GIVEN I am applying results to an appeal / Application
     * AND I am applying results to an offence
     * AND none of the results I am applying to the offence are endorsement results
     * AND I am applying the sentence varied result for the offence
     * AND I am applying the appeal allowed on the appeal
     * AND the previous results included an endorsement for the offence
     * WHEN the results are shared
     * THEN the notification is sent to rehab@dvla.gov.uk
     * AND the title of the notification is "DVLA Driver Notification – Remove Endorsement"
     * AND the summary section identifies that the notification is to be removed plus a list of the removed endorsements
     * AND the  D20 pdf section is omitted for the removed endorsement
     * AND the Previous D20 section is included
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario7() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario7/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW98466038")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario7/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW98466038")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Endorsement Removed following appeal - Appeal against conviction allowed
     *
     * GIVEN I am applying results to an appeal
     * AND I am applying the Appeal Against Conviction Allowed result against the appeal
     * AND an offence on the appeal previously included an endorsement result
     * WHEN the results are shared
     * THEN the notification is sent to rehab@dvla.gov.uk
     * AND the title of the notification is "DVLA Driver Notification – Remove Endorsement"
     * AND the summary section has a list of the removed endorsements
     * AND the Conviction date & Convicting court in the Driver Information section shows the previous details (even though the successful appeal has removed their conviction)
     * AND the Main D20 offence info subsection includes the previous endorsement details
     * AND the previous D20 information section is included
     * AND the appeal against conviction box is included
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario9() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario9/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC007")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario9/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC007")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("MW10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Removing a points disqualification (Totting) - sentence varied
     * GIVEN I am applying results to an appeal / Application
     * AND I am applying results to an offence
     * AND none of the results I am applying to the offence are endorsement results
     * AND I am applying the sentence varied result for the offence
     * AND the previous results included an endorsement for the offence
     * AND the previous results included a points disqualification (ie result had triggered an additional TT99 code)
     * When I share the result
     * And D20 pdf is generated
     * AND the title of the notification is "DVLA Driver Notification – Remove Endorsement"
     * WHEN I view the Summary section
     * THEN the list of removed endorsements includes the TT99 code
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario10() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario10/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC008")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario10/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC008")
                .hasCourtApplications(1)
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("MW10","TT99")
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Removing a points disqualification (Totting) - Appeal against conviction allowed
     * GIVEN I am applying results to an appeal
     * AND I am applying the Appeal Against Conviction Allowed result against the appeal
     * AND an offence on the appeal previously included an endorsement result
     * AND the previous results included a points disqualification (ie result had triggered an additional TT99 code)
     * When I share the result
     * And D20 pdf is generated
     * AND the title of the notification is "DVLA Driver Notification – Remove Endorsement"
     * WHEN I view the Summary section
     * THEN the list of removed endorsements includes the TT99 code
     *
     * @throws IOException
     */
    @Test
    public void appealAmendReshareScenario11() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario11/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC009")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway")
                .hasDVLACode(2,"TT99")
                .hasOffenceCode(2,"ME82005TT99")
                .hasWording(2,"Drive / move vehicle making 'u' turn on the motorway")
                .hasDisqualificationPeriod(2,"000600");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/scenario11/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC009")
                .hasCourtApplications(1)
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("MW10","TT99")
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    @Test
    void applicationAmendReshareDD27546() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/dd-27546/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
               .hasCaseReference("RQ286110796")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88339")
                .hasDVLACode("DR20")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("1")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(4)
                .hasWording("Drive whilst unfit through drink");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        sendAndVerifyEvent("applicationAmendReshare/dd-27546/command4.json",  0);

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

    }

    // D20 Scenarios for appeal amend and reshare - End

    // D20 Scenarios for application(stat dec) amend and reshare - Start

    /**
     * Given that a case is created in CC with 1 defendant
     * And has an endorsable offence
     * And is listed for hearing
     * And an endorsement result is entered against offences on the case
     * And a Community order is entered against the offence
     * And shared
     * And a D20 API notification is generated for the case
     * When the HMTC user creates an application e.g. “Application within criminal proceedings" linked to the case
     * And list for a court hearing
     * And a Application is resulted with Granted and COV (community order varied)
     * And shares the result
     * Then do NOT generate any update/removal D20 for original case
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario1/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW74896350")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(5)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario1/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW74896350")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("IN14")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Don't generate D20 removal for Application Criminal Proceedings hearing results of INACTIVE case.
     *
     * @throws IOException
     */
    @Test
    void sni7046NoRemoveEndorsementOnInactiveCaseWithApplicationTypeCriminalProceedings() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario1/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW74896350")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88319")
                .hasDVLACode("IN14")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-04-23")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(5)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario1/command2.json",  0);
    }

    @Test
    void sni7046NoRemoveEndorsementOnInactiveCaseWithApplicationTypeCriminalProceedingsScenario2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario2/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("DVLA02022394")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("010000")
                .hasAmountOfFine("£1111.00")
                .hasResults(4)
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario2/command2.json",  0);
    }

    @Test
    void sni7046NoRemoveEndorsementOnInactiveCaseWithApplicationTypeCriminalProceedingsScenario3() throws IOException {
        sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario3/command1.json",  0);

        sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario3/command2.json",  1);

        sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario3/command3.json",  0);
    }

    @Test
    void sni7046NoRemoveEndorsementOnInactiveCaseWithApplicationTypeCriminalProceedingsScenario4() throws IOException {
        sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario4/command1.json",  1);

        final List<DriverNotified> driverNotifieds = sendAndVerifyEvent("applicationAmendReshare/sni7046/scenario4/command2.json", 1);
        DriverNotifiedEventAssertion.with(driverNotifieds.get(0))
                .hasCaseReference("DVLA02022416")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("SP50")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

    }

    /**
     * Scenario 4a: Application and Application offence resulted in different hearings - New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Stat Dec has been created for the case
     * AND the Application has been listing in a Criminal courts hearing
     * AND the application has been granted i.e. resulted with the STDEC result
     * AND the result has been shared
     * AND a D20 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario4a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW126523173")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("3")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4a/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW126523173")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS30")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4a/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW126523173")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


    }

    /**
     * Scenario 4b: Application and Application offence resulted in different hearings - New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Stat Dec has been created for the case
     * AND the Application has been referred to box work
     * AND the application has been granted i.e. resulted with the STDEC result
     *
     * AND the offences have been adjourned to another day
     * AND the result has been shared
     * AND a D20 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     * @throws IOException
     */

    @Test
    public void applicationAmendReshareScenario4b() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4b/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW132310877")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("3")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4b/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW132310877")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS30")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4b/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW132310877")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 4c: Application and Application offence resulted in different hearings (listed) - New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the case
     * AND the Application has been listing in a Criminal courts hearing
     * AND the application has been granted i.e. resulted with the ROPENED result
     * AND the result has been shared
     * AND a D905 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */

    @Test
    public void applicationAmendReshareScenario4c() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4c/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW127755221")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)

                .hasPenaltyPoints("3")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4c/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW127755221")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS30")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4c/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW127755221")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 4d: Application and Application offence resulted in different hearings (boxwork) - New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the case
     * AND the Application has been referred to box work
     * AND the application has been granted i.e. resulted with the ROPENED result
     *
     * AND the offences have been adjourned to another day
     * AND the result has been shared
     * AND a D905 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario4d() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4d/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW133764758")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("3")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4d/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW133764758")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS30")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4d/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW133764758")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("CA03012")
                .hasDVLACode("TS30")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(3)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 4f: Application and Application offence resulted in different hearings (boxwork) - New D20 sent
     *
     * GIVEN that an CC case has been resulted with an endorsement
     * AND subsequently a Stat Dec Application has been created for the case
     * AND the Application has been referred to box work
     * AND the application has been granted i.e. resulted with the STDEC result
     *
     * AND the offences have been adjourned to another day
     * AND the result has been shared
     * AND a D905 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario4f() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4f/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW27684335")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4f/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW27684335")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4f/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW27684335")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario DD-38336: Application resulted in ERR - No D20 remove
     *
     * GIVEN that an CC case has been resulted with an endorsement
     * AND subsequently a Application has been created for the case
     * AND the Application has been referred to box work
     * AND the application offences have been adjourned to another day
     * AND the result has been shared
     * AND in a subsequent hearing, the Application is resulted with an ERR
     * WHEN the result is shared
     * THEN no remove endorsement generated
     * WHEN same resulted ammended and shared with G and withdrawal
     * THEN it should generate removal D20
     * @throws IOException
     */
    @Test
    void applicationAmendReshareDd38336a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/dd-38336/a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("KW27684335")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");


        sendAndVerifyEvent("applicationAmendReshare/dd-38336/a/command2.json",  0);
        sendAndVerifyEvent("applicationAmendReshare/dd-38336/a/command3.json",  0);
    }
    /**
     * Scenario DD-38336: Application resulted in ERR - No D20 remove
     *
     * GIVEN that an CC case has been resulted with an endorsement
     * AND subsequently a Application has been created for the case
     * AND the Application has been referred to box work
     * AND the application offences have been adjourned to another day
     * AND the result has been shared
     * AND in a subsequent hearing, the Application is resulted with an ERR
     * WHEN the result is shared
     * THEN no remove endorsement generated
     * WHEN same resulted ammended and shared with G and withdrawal
     * THEN it should generate removal D20
     * @throws IOException
     */
    @Test
    void applicationAmendReshareDd38336b() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/dd-38336/b/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("DVLA02022454")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("000600")
                .hasAmountOfFine("£111.00")
                .hasResults(4)
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        sendAndVerifyEvent("applicationAmendReshare/dd-38336/b/command2.json",  0);
        sendAndVerifyEvent("applicationAmendReshare/dd-38336/b/command3.json",  0);
        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/dd-38336/b/command4.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("DVLA02022454")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("SP50")
                .hasCourtApplications(1)
                .hasOffences(0);
    }

    /**
     * Scenario DD-38336: Application resulted in ERR - No D20 remove
     *
     * GIVEN that an CC case has been resulted with an endorsement
     * AND subsequently a Application has been created for the case
     * AND the Application has been referred to box work
     * AND the application offences have been adjourned to another day
     * AND the result has been shared
     * AND in a subsequent hearing, the Application is resulted with G and withdrawal
     * THEN it should generate removal D20
     * @throws IOException
     */
    @Test
    void applicationAmendReshareDd38336c() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/dd-38336/c/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("DVLA02022455")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RR84702")
                .hasDVLACode("SP50")
                .hasDisqualificationPeriod("000600")
                .hasAmountOfFine("£111.00")
                .hasResults(4)
                .hasWording("A fine not exceeding level five on the standard scale.Time limit for prosecutions:6 monthsOn the 12th March 2015 the limit on fines imposed by a Magistrates? court was removed ? as such, the potential fine on summary conviction in relation to an offence committed after this date is unlimited.");

        sendAndVerifyEvent("applicationAmendReshare/dd-38336/c/command2.json",  0);
        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/dd-38336/c/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("DVLA02022455")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("SP50")
                .hasCourtApplications(1)
                .hasOffences(0);
    }

    /**
     * Scenario 4g: Application and Application offence resulted in different hearings (listed) - New D20 sent
     *
     * GIVEN that an CC case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the case
     * AND the Application has been listing in a Criminal courts hearing
     * AND the application has been granted i.e. resulted with the REOPENED result
     * AND the result has been shared
     * AND a D905 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */

    @Test
    public void applicationAmendReshareScenario4g() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4g/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW33206369")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4g/command2.json",  1);
        verify(driverNotifiedList.get(0), "JW33206369", NOT_NULL_VALUE, NOT_NULL_VALUE, asList(), NOT_NULL_VALUE, asList("TS10"));
        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW33206369")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4g/command3.json",  1);
        verify(driverNotifiedList.get(0), "JW33206369", NOT_NULL_VALUE, NULL_VALUE, null, NULL_VALUE, null);
        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW33206369")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("5")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 4h: Application and Application offence resulted in different hearings (boxwork) - New D20 sent
     *
     * GIVEN that an CC case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the case
     * AND the Application has been referred to box work
     * AND the application has been granted i.e. resulted with the ROPENED result
     *
     * AND the offences have been adjourned to another day
     * AND the result has been shared
     * AND a D905 has been generated and sent to DVLA (existing behaviour)
     * AND in a subsequent hearing, the Application offence is resulted with an endorsement result
     * WHEN the result is shared
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */

    @Test
    public void applicationAmendReshareScenario4h() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4h/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW34086049")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("2")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4h/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW34086049")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario4h/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW34086049")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("5")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 5a: Application dealt with on SJP and Case offence resulted in same session- New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Stat Dec (COA) has been created for the case
     *
     * AND the Application has been referred to SJP
     * AND the application has been accepted
     * AND a D20 has been generated and sent to DVLA (existing behaviour)
     * AND the case has been resulted again with an endorsement result in the same session
     * WHEN the decision is saved
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario5a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario5a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW135015757")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("2")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario5a/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW135015757")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario5a/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW135015757")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("5")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 5b: Application dealt with on SJP and Case offence resulted in same session- New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the caseAND the Application has been referred to SJP
     * AND the application has been accepted
     * AND a D20 has been generated and sent to DVLA (existing behaviour)
     * AND the case has been resulted again with an endorsement result in the same session
     * WHEN the decision is saved
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario5b() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario5b/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW135641358")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("2")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario5b/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW135641358")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario5b/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW135641358")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("5")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 6a: Application dealt with on SJP and Case offence resulted in a different session- New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Stat Dec (COA) Application has been created for the case
     * AND the Application has been referred to SJP
     * AND the application has been accepted
     *
     * AND you end the session  (after the first check your answers page)
     * AND a D20 has been generated and sent to DVLA (existing behaviour)
     * AND the case has been resulted again with an endorsement result in a different session
     * WHEN the decision is saved
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario6a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario6a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW136354298")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("2")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario6a/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW136354298")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario6a/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW136354298")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("5")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 6b: Application dealt with on SJP and Case offence resulted in a different session- New D20 sent
     *
     * GIVEN that an SJP case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the case
     * AND the Application has been referred to SJP
     * AND the application has been accepted
     *
     * AND you end the session (after the first check your answers page)
     * AND a D20 has been generated and sent to DVLA (existing behaviour)
     * AND the case has been resulted again with an endorsement result in a different session
     * WHEN the decision is saved
     * THEN a new D20 notification is sent to DVLA detailing the new endorsement
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario6b() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario6b/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW136915653")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("2")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario6b/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW136915653")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(0)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario6b/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW136915653")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasPenaltyPoints("5")
                .hasAmountOfFine("£147.00")
                .hasResults(5)
                .hasWording("On 20 Nov 2022, that Josh Austin failed to hand over their ticket for inspection and verification of validity when asked to do so by an authorised person.");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 7a: Application offence result amended - Updated D20 sent
     *
     * GIVEN that a CC case has been resulted with an endorsement
     * AND subsequently a Stat Dec Application has been created for the case
     * AND the application has been granted i.e. resulted with the STDEC result
     * AND and in the same hearing or a subsequent hearing the Application offence has been resulted with an endorsement result
     * AND D20 notifications have been sent to DVLA (Note - Current behaviour may not send a new D20, it may be an updated D20)
     * WHEN the endorsement result on the Application offence is amended (but still remains as an endorsement result) and re-shared
     * THEN an updated D20 is sent to DVLA
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario7a() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario7a/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC001")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway");
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario7a/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC001")
                .hasUpdatedEndorsementContains("MW10")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway")
                .hasPreviousCase();
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario7a/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC001")
                .hasUpdatedEndorsementContains("MW10")
                .hasEmptyRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("5")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway")
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Scenario 7b: Application offence result amended - Updated D20 sent
     *
     * GIVEN that a CC case has been resulted with an endorsement
     * AND subsequently a Re-opening Application has been created for the case
     * AND the application has been granted i.e. resulted with the ROPENED result
     * AND and in the same hearing or a subsequent hearing the Application offence has been resulted with an endorsement result
     * AND D20 notifications have been sent to DVLA (Note - Current behaviour may not send a new D20, it may be an updated D20)
     * WHEN the endorsement result on the Application offence is amended (but still remains as an endorsement result) and re-shared
     * THEN an updated D20 is sent to DVLA
     *
     * @throws IOException
     */
    @Test
    public void applicationAmendReshareScenario7b() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario7b/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC000")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway");
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario7b/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC000")
                .hasCourtApplications(1)
                .hasUpdatedEndorsementContains("MW10")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("4")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway")
                .hasPreviousCase();
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("applicationAmendReshare/scenario7b/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("VT05MAYC000")
                .hasCourtApplications(1)
                .hasUpdatedEndorsementContains("MW10")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("ME82005")
                .hasDVLACode("MW10")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("5")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Drive / move vehicle making 'u' turn on the motorway")
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    // D20 Scenarios for application(stat dec) amend and reshare - End

    // D20 Scenarios for non-endorsable - Start

    /**
     * Scenario 1 One non endorsable offence in CC
     *
     * Given that a case is created in CC
     * And has a non-endorsable offence
     * And is listed for a court hearing
     * When the offence has an endorsable result(DDRN with disqual period of 6 months)  and is shared
     * Then trigger D20 API notification
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario1/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW56967707")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-06")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    /**
     * Scenario 2 One non endorsable offence in CC Amend and re-share
     *
     * Given that a case is created in CC
     * And has a non-endorsable offences
     * And is listed for a court hearing
     * And the offence has an endorsable result(DDRN with disqual period of 6 months)  and is shared
     * And D20 API notification triggered
     * When the result is amended (disqual period amended to 9 months) and re-shared
     * Then generate the Update D20 pdf
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario2/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW60164798")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-06")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario2/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW60164798")
                .hasCourtApplications(0)
                .hasUpdatedEndorsementContains("NE98")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000900")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-06")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 5: One non-endorsable offence in CP and Application added
     *
     * GIVEN that a CC case is created in CC
     * And has a non endorsable offence
     * Has been resulted with an endorsement result
     * And D20 API notification sent
     * AND subsequently a Stat Dec /Re-opening Application has been created for the case
     * AND the application has been granted i.e. resulted with the STDEC /ROPENED result
     * AND and in the same hearing or a subsequent hearing the Application offence has been resulted with an endorsement result
     * AND D905 and D20 notifications have been sent to DVLA
     * WHEN the endorsement result on the Application offence is amended (but still remains as an endorsement result) and re-shared
     * THEN an updated D20 is sent to DVLA
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario5() throws IOException {

        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario5/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW50416889")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("TH68023")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-08-18")
                .hasAmountOfFine("£100.00")
                .hasResults(3)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");
        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);


        driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario5/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW50416889")
                .hasUpdatedEndorsementContains("NE98")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("TH68023")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000900")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-08-18")
                .hasAmountOfFine("£150.00")
                .hasResults(4)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario5/command3.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW50416889")
                .hasCourtApplications(1)
                .hasUpdatedEndorsementContains("NE98")
                .hasEmptyRemovedEndorsements()
                .hasOffences(1)
                .hasOffenceCode("TH68023")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("001000")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-08-18")
                .hasAmountOfFine("£150.00")
                .hasResults(4)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }
    /**
     * Scenario 6 Two non endorsable offences in CC
     *
     * Given that a case is created in CC
     * And has two non-endorsable offences
     * And is listed for a court hearing
     * When all the offences have an endorsable result(DDRN with disqual period of 6 months)  and is shared
     * Then trigger D20 API notification
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario6() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario6/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW76734888")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod(EMPTY_STRING)
                .hasDrugLevel("500")
                .hasPenaltyPoints("3")
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-06")
                .hasAmountOfFine(EMPTY_STRING)
                .hasResults(2)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88045")
                .hasDisqualificationPeriod(2,EMPTY_STRING)
                .hasDVLACode(2,"NE98")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,"4")
                .hasConvictingCourt(2,"2577")
                .hasConvictionDate(2,"2023-07-06")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasResults(2,2);

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 7 Two non endorsable offence in CC Amend and re-share
     *
     * Given that a case is created in CC
     * And has two non-endorsable offences
     * And is listed for a court hearing
     * And all the offences have an endorsable result(DDRN with disqual period of 6 months)  and is shared
     * And D20 API notification triggered
     * When the result is amended (disqual period amended to 9 months) and re-shared
     * Then generate the Update D20 pdf
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario7() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario7/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW179778064")
                .hasUpdatedEndorsementContains()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-06")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88045")
                .hasDisqualificationPeriod(2,EMPTY_STRING)
                .hasDVLACode(2,"NE98")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,"3")
                .hasConvictingCourt(2,"2577")
                .hasConvictionDate(2,"2023-07-06")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario7/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW179778064")
                .hasUpdatedEndorsementContains("NE98","NE98")
                .hasEmptyRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000900")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-06")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88045")
                .hasDisqualificationPeriod(2,EMPTY_STRING)
                .hasDVLACode(2,"NE98")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,"3")
                .hasConvictingCourt(2,"2577")
                .hasConvictionDate(2,"2023-07-06")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 8 Mix of 1 endorsable and 1 non-endorsable offence in CC
     *
     * Given that a case is created in CC
     * And has 1 endorsable offences and 1 non-endorsable offence
     * And is listed for a court hearing
     * When all the offences have an endorsable result(DDRN with disqual period of 6 months)  and is shared
     * Then trigger D20 API notification
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario8() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario8/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW129289016")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-07")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88971")
                .hasDisqualificationPeriod(2,"000600")
                .hasDVLACode(2,"TS10")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,EMPTY_STRING)
                .hasConvictingCourt(2,"2577")
                .hasConvictionDate(2,"2023-07-07")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 9 Mix of 1 endorsable and 1 non-endorsable offence in CC Amend and re-share
     *
     * Given that a case is created in CC
     * And has 1 endorsable offences and 1 non-endorsable offence
     * And is listed for a court hearing
     * And all the offences have an endorsable result(DDRN with disqual period of 6 months)  and is shared
     * And D20 API notification triggered
     * When the result is amended (disqual period amended to 9 months) and re-shared
     * Then generate the Update D20 pdf
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario9() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario9/command1.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW129508366")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000600")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-07")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88971")
                .hasDisqualificationPeriod(2,"000600")
                .hasDVLACode(2,"TS10")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,EMPTY_STRING)
                .hasConvictingCourt(2,"2577")
                .hasConvictionDate(2,"2023-07-07")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("nonendorsable/scenario9/command2.json",  1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW129508366")
                .hasUpdatedEndorsementContains("NE98", "TS10")
                .hasEmptyRemovedEndorsements()
                .hasOffences(2)
                .hasOffenceCode("RT88045")
                .hasDVLACode("NE98")
                .hasDisqualificationPeriod("000900")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-07-07")
                .hasAmountOfFine(EMPTY_STRING)
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasOffenceCode(2,"RT88971")
                .hasDisqualificationPeriod(2,"000600")
                .hasDVLACode(2,"TS10")
                .hasDrugLevel(2,"500")
                .hasPenaltyPoints(2,EMPTY_STRING)
                .hasConvictingCourt(2,"2577")
                .hasConvictionDate(2,"2023-07-07")
                .hasAmountOfFine(2,EMPTY_STRING)
                .hasWording(2,"Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

    }

    /**
     * Scenario 10 Negative scenario
     *
     * Given that a case is created in CC
     * And has a non-endorsable offence
     * And is listed for a court hearing
     * When the offence has an non-endorsable result (Fine)  and is shared
     * Then do NOT trigger D20 API notification
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario10() throws IOException {
        sendAndVerifyEvent("nonendorsable/scenario10/command1.json",  0);
    }

    /**
     * Scenario 11 Negative scenario
     *
     * Given that a case is created in CC
     * And has an endorsable offence
     * And is listed for a court hearing
     * When the offence has an non-endorsable result (Fine)  and is shared
     * Then do NOT trigger D20 API notification
     *
     * @throws IOException
     */
    @Test
    public void nonendorsableScenario11() throws IOException {
        sendAndVerifyEvent("nonendorsable/scenario11/command1.json",  0);
    }

    /**
     * Scenario 12 - https://tools.hmcts.net/jira/browse/DD-27020
     * GIVEN I am applying results to an appeal / Application
     *
     * And has 2 offences
     * AND I am applying results to offence 1
     * AND none of the results I am applying to the offence 1 are endorsement results
     * AND I am applying the sentence varied result for the offence 1
     * AND the previous results included an endorsement for the offence 1
     * AND I am updating an endorsement for a offence 2
     *
     * And both offences had endorsement in previous hearing
     * WHEN the results are shared
     * THEN the notification is sent to rehab@dvla.gov.uk
     * AND the title of the notification is "DVLA Driver Notification – Remove Endorsement"
     * AND the summary has a list of the removed endorsements
     * AND the summary section has a list of endorsements to be updated
     * AND the D20 pdf offence info subsection is omitted for the removed endorsement
     * AND the D20 pdf offence info subsection is present for the updated endorsement
     * AND the Previous D20 section is included
     *
     */
    @Test
    public void nonndorsableScenario12_mix_of_updated_removed_endorsement() throws Exception {
        List<DriverNotified> driverNotifiedList1 = sendAndVerifyEvent("nonendorsable/scenario12/command1.json", 1);
        DriverNotifiedEventAssertion.with(driverNotifiedList1.get(0))
                .hasCaseReference("JW48022584")
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("000100")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt("2577")
                .hasConvictionDate("2023-10-16")
                .hasAmountOfFine("£100.00")
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasResults(3)
                .hasOffenceCode(2, "RT88971")
                .hasDisqualificationPeriod(2, "000200")
                .hasDVLACode(2, "TS10")
                .hasDrugLevel(2, "500")
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasConvictingCourt(2, "2577")
                .hasConvictionDate(2, "2023-10-16")
                .hasAmountOfFine(2, "£150.00")
                .hasResults(2, 3)
                .hasWording(2, "Has a violent past and fear that he will commit further offences and interfere with witnesse");

        verifyDVLANotificationCommandInvoked(driverNotifiedList1);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList1);

        List<DriverNotified> driverNotifiedList2 = sendAndVerifyEvent("nonendorsable/scenario12/command2.json", 1);
        DriverNotifiedEventAssertion.with(driverNotifiedList2.get(0))
                .hasCaseReference("JW48022584")
                .hasEmptyUpdatedEndorsements()
                .hasRemovedEndorsementContains("TS10")
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode("RT88971")
                .hasDVLACode("TS10")
                .hasDisqualificationPeriod("000300")
                .hasDrugLevel("500")
                .hasPenaltyPoints(EMPTY_STRING)
                .hasConvictingCourt(null)
                .hasConvictionDate(null)
                .hasAmountOfFine("£250.00")
                .hasWording("Has a violent past and fear that he will commit further offences and interfere with witnesse")
                .hasResults(4)
                .hasPreviousCase();

        verifyDVLANotificationCommandInvoked(driverNotifiedList2);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList2);
    }

    /**
     * Scenario : Given a Hearing 1 resulted with interim disqualification and adjourn it to Hearing 2
     * and adjourn hearing 2 to hearing 3 with just adjournment
     * Expected : "remove endorsement PDF" should not be generated
     */
    @Test
    public void sni5768() throws IOException {
        sendAndVerifyEvent("adjournedHearing/sni-5768/command1.json",  1);
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

        sendAndVerifyEvent("adjournedHearing/sni-5768/command2.json",  0);

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

    }

    /**
     * Scenario : Given a Hearing 1 resulted with interim disqualification and adjourn it to Hearing 2
     * and adjourn hearing 2 to hearing 3 with  Remand unconditional bail
     * Expected : "remove endorsement PDF" should not be generated
     */
    @Test
    void sni7666() throws IOException {
        sendAndVerifyEvent("adjournedHearing/sni-7666/command1.json",  1);
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

        sendAndVerifyEvent("adjournedHearing/sni-7666/command2.json",  0);

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

    }

    /**
     * Scenario : Given a Hearing 1 resulted with interim disqualification and adjourn it to Hearing 2
     * and adjourn hearing 2 to hearing 3 with  Remand unconditional bail
     * Expected : "remove endorsement PDF" should not be generated
     */
    @Test
    void sni7666_1() throws IOException {
        sendAndVerifyEvent("adjournedHearing/sni-7666-1a/command1.json",  1);
        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();
        sendAndVerifyEvent("adjournedHearing/sni-7666-1a/command2.json",  0);

        verifyDVLANotificationCommandInvoked();
        verifyGenerateDocumentStubCommandInvoked();

    }
    // D20 Scenarios for non-endorsable - End
    private List<DriverNotified> sendAndVerifyEvent(final String filePath, final int expectedEventCount) throws IOException {

        final String body = getPayload(filePath);
        final Response writeResponse = postCommandWithUserId(getWriteUrl("/driver-notification"),
                DRIVER_NOTIFICATION_MEDIA_TYPE, body, USER_GROUP);

        assertThat(writeResponse.getStatusCode(), equalTo(SC_ACCEPTED));

        final List<DriverNotified> driverNotifiedList = new ArrayList<>();

        for(int i = 0; i < expectedEventCount; i++){
            Optional<JsonObject> jsonObject = retrieveMessageAsJsonObject(consumerForDriverNotified);

            if (jsonObject.isPresent()) {
                driverNotifiedList.add(jsonToObjectConverter.convert(jsonObject.get(), DriverNotified.class));
            }
        }

        assertThat(driverNotifiedList.size(), equalTo(expectedEventCount));

        return driverNotifiedList;
    }

    private void verify(final DriverNotified driverNotified,
                        final String expectedCaseReference,
                        final Matcher<Object> driverNotifiedMatcher,
                        final Matcher<Object> updatedMatcher,
                        final List<String> updatedEndorsements,
                        final Matcher<Object> removedMatcher,
                        final List<String> removedEndorsements) {

        assertThat(driverNotified.getCases(), NOT_NULL_VALUE);
        assertThat(driverNotified.getCases().size(), equalTo(1));
        assertThat(driverNotified.getCases().get(0).getReference(), equalTo(expectedCaseReference));

        assertThat(driverNotified, is(driverNotifiedMatcher));
        assertThat(driverNotified.getUpdatedEndorsements(), is(updatedMatcher));
        assertThat(driverNotified.getRemovedEndorsements(), is(removedMatcher));

        if (!isNull(driverNotified.getUpdatedEndorsements())) {
            assertThat(driverNotified.getUpdatedEndorsements(), is(updatedEndorsements));
        }

        if (!isNull(driverNotified.getRemovedEndorsements())) {
            assertThat(driverNotified.getRemovedEndorsements(), is(removedEndorsements));
        }

    }

    private String getPayload(String fileName) {
        hearingId = randomUUID().toString();

        String body = FileUtil.getPayload(fileName);
        body = body.replaceAll("%HEARING_ID%", hearingId)
                .replaceAll("%DEFENDANT_ID%", defendantId);

        return body;
    }

}

