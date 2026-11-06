package uk.gov.moj.cpp.persistence.entity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DriverAuditEntityTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();
    private static final LocalDate DOB = LocalDate.of(1990, 1, 2);

    @Test
    void shouldExposeAllFieldsSuppliedToTheAllArgsConstructor() {
        final DriverAuditEntity entity = new DriverAuditEntity(ID, USER_ID, "user@hmcts.net", DATE_TIME,
                "REASON", "REF-1", "DL123", "First Middle", "Last", "M", "AB1 2CD", DOB);

        assertThat(entity.getId(), is(ID));
        assertThat(entity.getUserId(), is(USER_ID));
        assertThat(entity.getUserEmail(), is("user@hmcts.net"));
        assertThat(entity.getDateTime(), is(DATE_TIME));
        assertThat(entity.getReasonType(), is("REASON"));
        assertThat(entity.getReference(), is("REF-1"));
        assertThat(entity.getDrivingLicenseNumber(), is("DL123"));
        assertThat(entity.getFirstNames(), is("First Middle"));
        assertThat(entity.getLastName(), is("Last"));
        assertThat(entity.getGender(), is("M"));
        assertThat(entity.getPostcode(), is("AB1 2CD"));
        assertThat(entity.getDateOfBirth(), is(DOB));
    }

    @Test
    void shouldExposeAllFieldsSetViaSetters() {
        final DriverAuditEntity entity = new DriverAuditEntity();
        entity.setId(ID);
        entity.setUserId(USER_ID);
        entity.setUserEmail("a@b.net");
        entity.setDateTime(DATE_TIME);
        entity.setReasonType("R");
        entity.setReference("REF");
        entity.setDrivingLicenseNumber("DL");
        entity.setFirstNames("F");
        entity.setLastName("L");
        entity.setGender("F");
        entity.setPostcode("PC1 1AA");
        entity.setDateOfBirth(DOB);

        assertThat(entity.getId(), is(ID));
        assertThat(entity.getUserId(), is(USER_ID));
        assertThat(entity.getUserEmail(), is("a@b.net"));
        assertThat(entity.getDateTime(), is(DATE_TIME));
        assertThat(entity.getReasonType(), is("R"));
        assertThat(entity.getReference(), is("REF"));
        assertThat(entity.getDrivingLicenseNumber(), is("DL"));
        assertThat(entity.getFirstNames(), is("F"));
        assertThat(entity.getLastName(), is("L"));
        assertThat(entity.getGender(), is("F"));
        assertThat(entity.getPostcode(), is("PC1 1AA"));
        assertThat(entity.getDateOfBirth(), is(DOB));
    }
}
