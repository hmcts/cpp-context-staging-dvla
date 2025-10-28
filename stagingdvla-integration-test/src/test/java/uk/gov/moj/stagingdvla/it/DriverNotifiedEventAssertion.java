package uk.gov.moj.stagingdvla.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.cpp.stagingdvla.event.DefendantCaseOffences;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Fluent assertion to validate a notified event
 */
public class DriverNotifiedEventAssertion {

    private DriverNotified notifiedEvent;
    public static final String EMPTY_STRING = "";

    private DriverNotifiedEventAssertion(DriverNotified notifiedEvent) {
        this.notifiedEvent = notifiedEvent;
    }

    public static DriverNotifiedEventAssertion with(DriverNotified notifiedEvent) {
        Objects.requireNonNull(notifiedEvent);
        return new DriverNotifiedEventAssertion(notifiedEvent);
    }

    public DriverNotifiedEventAssertion hasOneCase() {
        assertThat(notifiedEvent.getCases(), hasSize(1));
        return this;
    }

    public DriverNotifiedEventAssertion hasCaseReference(String reference) {
        assertThat(notifiedEvent.getCases().get(0).getReference(), equalTo(reference));
        return this;
    }

    public DriverNotifiedEventAssertion hasOffences(int size) {
        assertThat(getOffences(), hasSize(size));
        return this;
    }

    public DriverNotifiedEventAssertion hasUpdatedEndorsements() {
        assertThat(notifiedEvent.getUpdatedEndorsements(), not(empty()));
        return this;
    }

    public DriverNotifiedEventAssertion hasEmptyUpdatedEndorsements() {
        return isEmpty(notifiedEvent::getUpdatedEndorsements);
    }

    public DriverNotifiedEventAssertion hasRemovedEndorsements() {
        assertThat(notifiedEvent.getRemovedEndorsements(), not(empty()));
        return this;
    }

    public DriverNotifiedEventAssertion hasEmptyRemovedEndorsements() {
        return isEmpty(notifiedEvent::getRemovedEndorsements);
    }

    public DriverNotifiedEventAssertion hasNoRemovedEndorsements() {
        return isNull(notifiedEvent::getRemovedEndorsements);
    }

    public DriverNotifiedEventAssertion hasNoUpdatedEndorsements() {
        return isNull(notifiedEvent::getUpdatedEndorsements);
    }

    public DriverNotifiedEventAssertion hasUpdatedEndorsementContains(String...updatedEndorsements) {
        Objects.requireNonNull(updatedEndorsements);
        assertThat(notifiedEvent.getUpdatedEndorsements(), hasItems(updatedEndorsements));
        return this;
    }

    public DriverNotifiedEventAssertion hasRemovedEndorsementContains(String...removedEndorsements) {
        assertThat(notifiedEvent.getRemovedEndorsements(), hasItems(removedEndorsements));
        return this;
    }
    public DriverNotifiedEventAssertion hasResults(int size) {
        return hasResults(1,size);
    }
    public DriverNotifiedEventAssertion hasAmountOfFine(String fineAmount) {
        return hasAmountOfFine(1, fineAmount);
    }

    public DriverNotifiedEventAssertion hasPenaltyPoints(String penaltyPoints) {
        return hasPenaltyPoints(1, penaltyPoints);
    }

    public DriverNotifiedEventAssertion hasDVLACode(String dvlaCode) {
        return hasDVLACode(1, dvlaCode);
    }

    public DriverNotifiedEventAssertion hasDrugLevel(String drugLevel) {
        return hasDrugLevel(1, drugLevel);
    }

    public DriverNotifiedEventAssertion hasDisqualificationPeriod(String disQualificationPeriod) {
        return hasDisqualificationPeriod(1, disQualificationPeriod);
    }

    public DriverNotifiedEventAssertion hasConvictionDate(String convictionDate) {
        return hasConvictionDate(1, convictionDate);
    }

    public DriverNotifiedEventAssertion hasConvictingCourt(String courtCode) {
        return hasConvictingCourt(1, courtCode);
    }

    public DriverNotifiedEventAssertion hasAppealDate(String appealDate) {
        return hasAppealDate(1, appealDate);
    }

    public DriverNotifiedEventAssertion hasWording(String wording) {
        return hasWording(1, wording);
    }

    public DriverNotifiedEventAssertion hasOffenceCode(String offenceCode) {
        return validate(getOffence(1), DefendantCaseOffences::getCode, offenceCode);
    }
    public DriverNotifiedEventAssertion hasResults(int offenceNumber,int size) {
        assertThat(getOffence(offenceNumber).getResults(), hasSize(size));
        return this;
    }
    public DriverNotifiedEventAssertion hasAmountOfFine(int offenceNumber, String fineAmount) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getFine, fineAmount);
    }

    public DriverNotifiedEventAssertion hasPenaltyPoints(int offenceNumber, String penaltyPoints) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getPenaltyPoints, penaltyPoints);
    }

    public DriverNotifiedEventAssertion hasDVLACode(int offenceNumber, String dvlaCode) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getDvlaCode, dvlaCode);
    }

    public DriverNotifiedEventAssertion hasDrugLevel(int offenceNumber, String drugLevel) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getAlcoholReadingAmount, drugLevel);
    }

    public DriverNotifiedEventAssertion hasDisqualificationPeriod(int offenceNumber, String disQualificationPeriod) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getDisqualificationPeriod, disQualificationPeriod);
    }

    public DriverNotifiedEventAssertion hasConvictionDate(int offenceNumber, String convictionDate) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getConvictionDate, convictionDate);
    }

    public DriverNotifiedEventAssertion hasConvictingCourt(int offenceNumber, String courtCode) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getConvictingCourtCode, courtCode);
    }

    public DriverNotifiedEventAssertion hasAppealDate(int offenceNumber, String appealDate) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getConvictionDate, appealDate);
    }

    public DriverNotifiedEventAssertion hasWording(int offenceNumber, String wording) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getWording, wording);
    }

    public DriverNotifiedEventAssertion hasOffenceCode(int offenceNumber, String offenceCode) {
        return validate(getOffence(offenceNumber), DefendantCaseOffences::getCode, offenceCode);
    }

    public DriverNotifiedEventAssertion hasPreviousCase(){
        assertThat(notifiedEvent.getPrevious(),notNullValue());
        return this;
    }
    public DriverNotifiedEventAssertion hasCourtApplications(int size){
        assertThat(notifiedEvent.getCourtApplications(),hasSize(size));
        return this;
    }
    private DefendantCaseOffences getOffence(int offenceNumber) {
        return getOffences().get(offenceNumber - 1);
    }

    private List<DefendantCaseOffences> getOffences() {
        return notifiedEvent.getCases().get(0).getDefendantCaseOffences();
    }

    private <T, R> DriverNotifiedEventAssertion validate(T input, Function<T, R> getFunction, R expectedValue) {
        assertThat(getFunction.apply(input), equalTo(expectedValue));
        return this;
    }

    private DriverNotifiedEventAssertion isEmpty(Supplier<Collection<?>> getValues) {
        assertThat(getValues.get(), empty());
        return this;
    }

    private DriverNotifiedEventAssertion isNull(Supplier<Collection<?>> getValues) {
        assertThat(getValues.get(), nullValue());
        return this;
    }

}
