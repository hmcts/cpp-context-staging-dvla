package uk.gov.moj.cpp.persistence.entity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.persistence.entity.DriverAuditReportEntity.DriverAuditReportEntityBuilder.builder;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DriverAuditReportEntityTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final ZonedDateTime DATE_TIME = ZonedDateTime.now();

    @Test
    void shouldExposeAllFieldsSuppliedToTheAllArgsConstructor() {
        final DriverAuditReportEntity entity = new DriverAuditReportEntity(ID, USER_ID, DATE_TIME,
                "criteria", "COMPLETE", "file-1", MATERIAL_ID);

        assertThat(entity.getId(), is(ID));
        assertThat(entity.getUserId(), is(USER_ID));
        assertThat(entity.getDateTime(), is(DATE_TIME));
        assertThat(entity.getReportSearchCriteria(), is("criteria"));
        assertThat(entity.getStatus(), is("COMPLETE"));
        assertThat(entity.getReportFileId(), is("file-1"));
        assertThat(entity.getMaterialId(), is(MATERIAL_ID));
    }

    @Test
    void shouldExposeAllFieldsSetViaSetters() {
        final DriverAuditReportEntity entity = new DriverAuditReportEntity();
        entity.setId(ID);
        entity.setUserId(USER_ID);
        entity.setDateTime(DATE_TIME);
        entity.setReportSearchCriteria("c");
        entity.setStatus("IN_PROGRESS");
        entity.setReportFileId("f");
        entity.setMaterialId(MATERIAL_ID);

        assertThat(entity.getId(), is(ID));
        assertThat(entity.getUserId(), is(USER_ID));
        assertThat(entity.getDateTime(), is(DATE_TIME));
        assertThat(entity.getReportSearchCriteria(), is("c"));
        assertThat(entity.getStatus(), is("IN_PROGRESS"));
        assertThat(entity.getReportFileId(), is("f"));
        assertThat(entity.getMaterialId(), is(MATERIAL_ID));
    }

    @Test
    void shouldBuildAnEntityViaTheBuilder() {
        final DriverAuditReportEntity entity = builder()
                .withId(ID)
                .withUserId(USER_ID)
                .withDateTime(DATE_TIME)
                .withReportSearchCriteria("criteria")
                .withStatus("COMPLETE")
                .withReportFileId("file-1")
                .withMaterialId(MATERIAL_ID)
                .build();

        assertThat(entity.getId(), is(ID));
        assertThat(entity.getUserId(), is(USER_ID));
        assertThat(entity.getDateTime(), is(DATE_TIME));
        assertThat(entity.getReportSearchCriteria(), is("criteria"));
        assertThat(entity.getStatus(), is("COMPLETE"));
        assertThat(entity.getReportFileId(), is("file-1"));
        assertThat(entity.getMaterialId(), is(MATERIAL_ID));
    }
}
