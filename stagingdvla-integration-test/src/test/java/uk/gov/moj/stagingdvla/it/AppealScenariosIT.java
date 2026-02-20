package uk.gov.moj.stagingdvla.it;

import static uk.gov.moj.stagingdvla.it.DriverNotifiedEventAssertion.EMPTY_STRING;
import static uk.gov.moj.stagingdvla.stubs.DVLANotificationStub.verifyDVLANotificationCommandInvoked;
import static uk.gov.moj.stagingdvla.stubs.DocumentGeneratorStub.verifyGenerateDocumentStubCommandInvoked;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.cpp.stagingdvla.event.NotificationType;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;


public class AppealScenariosIT extends AbstractIntegrationTest {

    @Test
    public void cimd_3230_ac1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3230_ac1/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, "6")
                .hasAmountOfFine(1, "£100.00")
                .hasAmountOfFine(2, "£200.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3230_ac1/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN20")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, "8")
                .hasAmountOfFine(1, "£100.00")
                .hasAmountOfFine(2, "£200.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3231_ac1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3231_ac1/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode(1, "RA88001")
                .hasDVLACode(1, "IN10")
                .hasPenaltyPoints(1, "6")
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasSentenceDate(1, null)
                .hasResults(1, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3231_ac1/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode(1, "RA88001")
                .hasDVLACode(1, "IN10")
                .hasPenaltyPoints(1, "8")
                .hasAmountOfFine(EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasSentenceDate(1, "2025-12-10")
                .hasResults(1, 1)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3232_ac2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3232_ac2/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, "001200")
                .hasResults(2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3232_ac2/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN20")
                .hasRemovedEndorsementContains("IN10")
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode(1, "RA88002")
                .hasDVLACode(1, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasAmountOfFine("£200.00")
                .hasDisqualificationPeriod(1, "001200")
                .hasResults(1, 2)
                .hasWording(1, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3235_ac1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3235_ac1/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3235_ac1/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN20")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, "001200")
                .hasResults(1, 3)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");
    }

    @Test
    public void cimd_3235_ac2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3235_ac2/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, "001200")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        sendAndVerifyEvent("appealAmendReshare/cimd_3235_ac2/command2.json", 0);
    }

    @Test
    public void cimd_3236_ac3() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3236_ac3/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(1)
                .hasOffenceCode(1, "RA88001")
                .hasDVLACode(1, "IN10")
                .hasPenaltyPoints(1, "6")
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasResults(1, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3236_ac3/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN20")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(2)
                .hasDVLACode(1, "IN20")
                .hasDVLACode(2, "IN10")
                .hasOffenceCode(1, "RA88002")
                .hasOffenceCode(2, "RA88001")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, "6")
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasResults(1, 1)
                .hasResults(2, 2)
                .hasWording(1, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence")
                .hasWording(2, "Use a motor vehicle on a road / public place without third party insurance");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3237_mixed_acs() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3237_mixed_acs/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, "6")
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£100.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasResults(2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3237_mixed_acs/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN20", "IN30")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(3)
                .hasOffenceCode(1, "RA88003")
                .hasOffenceCode(2, "RA88001")
                .hasOffenceCode(3, "RA88002")
                .hasDVLACode(1, "IN30")
                .hasDVLACode(2, "IN10")
                .hasDVLACode(3, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasPenaltyPoints(3, "6")
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, EMPTY_STRING)
                .hasAmountOfFine(3, "£100.00")
                .hasDisqualificationPeriod(1, "000600")
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasDisqualificationPeriod(3, EMPTY_STRING)
                .hasResults(1, 1)
                .hasResults(2, 2)
                .hasResults(3, 2)
                .hasWording(1, "Drive motor vehicle with a proportion of a specified controlled drug above the specified limit")
                .hasWording(2, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(3, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3237_mixed_acs/command3.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN20", "IN30")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(3)
                .hasOffenceCode(1, "RA88003")
                .hasOffenceCode(2, "RA88001")
                .hasOffenceCode(3, "RA88002")
                .hasDVLACode(1, "IN30")
                .hasDVLACode(2, "IN10")
                .hasDVLACode(3, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasPenaltyPoints(3, "6")
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, EMPTY_STRING)
                .hasAmountOfFine(3, "£100.00")
                .hasDisqualificationPeriod(1, "001200")
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasDisqualificationPeriod(3, EMPTY_STRING)
                .hasResults(1, 1)
                .hasResults(2, 2)
                .hasResults(3, 2)
                .hasWording(1, "Drive motor vehicle with a proportion of a specified controlled drug above the specified limit")
                .hasWording(2, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(3, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3238_ac1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3238_ac1/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, "001200")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3238_ac1/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN20")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, "001200")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3241_ac1_ac2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3241_ac1_ac2/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasCourtApplications(0)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasOffences(5)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasOffenceCode(3, "RA88003")
                .hasOffenceCode(4, "RA88006")
                .hasOffenceCode(5, "RA88007")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasDVLACode(3, "IN30")
                .hasDVLACode(4, "IN60")
                .hasDVLACode(5, "IN70")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasPenaltyPoints(3, EMPTY_STRING)
                .hasPenaltyPoints(4, EMPTY_STRING)
                .hasPenaltyPoints(5, EMPTY_STRING)
                .hasAmountOfFine(1, "£100.00")
                .hasAmountOfFine(2, EMPTY_STRING)
                .hasAmountOfFine(3, "£300.00")
                .hasAmountOfFine(4, "£600.00")
                .hasAmountOfFine(5, "£700.00")
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasDisqualificationPeriod(3, EMPTY_STRING)
                .hasDisqualificationPeriod(4, "001200")
                .hasDisqualificationPeriod(5, EMPTY_STRING)
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasResults(3, 2)
                .hasResults(4, 2)
                .hasResults(5, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance - IN10")
                .hasWording(2, "Use a motor vehicle on a road / public place without third party insurance - IN20")
                .hasWording(3, "Use a motor vehicle on a road / public place without third party insurance - IN30")
                .hasWording(4, "Use a motor vehicle on a road / public place without third party insurance - IN60")
                .hasWording(5, "Use a motor vehicle on a road / public place without third party insurance - IN70");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3241_ac1_ac2/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasCourtApplications(1)
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN10", "IN20", "IN60")
                .hasRemovedEndorsementContains("IN70")
                .hasOffences(4)
                .hasOffenceCode(1, "RA88004")
                .hasOffenceCode(2, "RA88001")
                .hasOffenceCode(3, "RA88002")
                .hasOffenceCode(4, "RA88006")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN10")
                .hasDVLACode(3, "IN20")
                .hasDVLACode(4, "IN60")
                .hasPenaltyPoints(1, "6")
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasPenaltyPoints(3, EMPTY_STRING)
                .hasPenaltyPoints(4, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£100.00")
                .hasAmountOfFine(3, EMPTY_STRING)
                .hasAmountOfFine(4, EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasDisqualificationPeriod(3, EMPTY_STRING)
                .hasDisqualificationPeriod(4, "001200")
                .hasResults(1, 1)
                .hasResults(2, 2)
                .hasResults(3, 2)
                .hasResults(4, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance - IN40")
                .hasWording(2, "Use a motor vehicle on a road / public place without third party insurance - IN10")
                .hasWording(3, "Use a motor vehicle on a road / public place without third party insurance - IN20")
                .hasWording(4, "Use a motor vehicle on a road / public place without third party insurance - IN60");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3241_ac1_ac2/command3.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasCourtApplications(1)
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN10", "IN10", "IN20", "IN60")
                .hasNoRemovedEndorsements()
                .hasOffences(4)
                .hasOffenceCode(1, "RA88004")
                .hasOffenceCode(2, "RA88001")
                .hasOffenceCode(3, "RA88002")
                .hasOffenceCode(4, "RA88006")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN10")
                .hasDVLACode(3, "IN20")
                .hasDVLACode(4, "IN60")
                .hasPenaltyPoints(1, "6")
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasPenaltyPoints(3, EMPTY_STRING)
                .hasPenaltyPoints(4, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£100.00")
                .hasAmountOfFine(3, "£200.00")
                .hasAmountOfFine(4, EMPTY_STRING)
                .hasDisqualificationPeriod(1, EMPTY_STRING)
                .hasDisqualificationPeriod(2, EMPTY_STRING)
                .hasDisqualificationPeriod(3, "001200")
                .hasDisqualificationPeriod(4, "001200")
                .hasResults(1, 1)
                .hasResults(2, 2)
                .hasResults(3, 3)
                .hasResults(4, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance - IN40")
                .hasWording(2, "Use a motor vehicle on a road / public place without third party insurance - IN10")
                .hasWording(3, "Use a motor vehicle on a road / public place without third party insurance - IN20")
                .hasWording(4, "Use a motor vehicle on a road / public place without third party insurance - IN60");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3242_ac1() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac1/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac1/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.REMOVE)
                .hasNoUpdatedEndorsements()
                .hasRemovedEndorsementContains("IN10", "IN20")
                .hasCourtApplications(1)
                .hasOffences(0);

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3242_ac2() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac2/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac2/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.REMOVE)
                .hasNoUpdatedEndorsements()
                .hasRemovedEndorsementContains("IN10", "IN20")
                .hasCourtApplications(1)
                .hasOffences(0);

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3242_ac3() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac3/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac3/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.REMOVE)
                .hasNoUpdatedEndorsements()
                .hasRemovedEndorsementContains("IN10", "IN20")
                .hasCourtApplications(1)
                .hasOffences(0);

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }

    @Test
    public void cimd_3242_ac4() throws IOException {
        List<DriverNotified> driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac4/command1.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.NEW)
                .hasNoUpdatedEndorsements()
                .hasNoRemovedEndorsements()
                .hasCourtApplications(0)
                .hasOffences(2)
                .hasOffenceCode(1, "RA88001")
                .hasOffenceCode(2, "RA88002")
                .hasDVLACode(1, "IN10")
                .hasDVLACode(2, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasPenaltyPoints(2, EMPTY_STRING)
                .hasAmountOfFine(1, EMPTY_STRING)
                .hasAmountOfFine(2, "£200.00")
                .hasResults(1, 2)
                .hasResults(2, 2)
                .hasWording(1, "Use a motor vehicle on a road / public place without third party insurance")
                .hasWording(2, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);

        driverNotifiedList = sendAndVerifyEvent("appealAmendReshare/cimd_3242_ac4/command2.json", 1);

        DriverNotifiedEventAssertion.with(driverNotifiedList.get(0))
                .hasCaseReference("JW29150867")
                .hasNotificationType(NotificationType.UPDATE)
                .hasUpdatedEndorsementContains("IN20")
                .hasNoRemovedEndorsements()
                .hasCourtApplications(1)
                .hasOffences(1)
                .hasOffenceCode(1, "RA88002")
                .hasDVLACode(1, "IN20")
                .hasPenaltyPoints(1, EMPTY_STRING)
                .hasAmountOfFine(1, "£200.00")
                .hasResults(1, 2)
                .hasWording(1, "Drive a motor vehicle otherwise than in accordance with a licence - endorsable offence");

        verifyDVLANotificationCommandInvoked(driverNotifiedList);
        verifyGenerateDocumentStubCommandInvoked(driverNotifiedList);
    }
}

