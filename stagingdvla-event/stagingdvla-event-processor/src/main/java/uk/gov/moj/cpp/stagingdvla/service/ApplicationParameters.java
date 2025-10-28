package uk.gov.moj.cpp.stagingdvla.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class ApplicationParameters {

    @Inject
    @Value(key = "dvlaEmailTemplateId", defaultValue = "d3ce1ce3-5233-4b9d-b881-3857351fbfb0")
    private String dvlaEmailTemplateId;

    @Inject
    @Value(key = "dvlaEmailAddress1", defaultValue = "name.surname@HMCTS.NET")
    private String dvlaEmailAddress1;

    @Inject
    @Value(key = "dvlaEmailAddress2", defaultValue = "name.surname@HMCTS.NET")
    private String dvlaEmailAddress2;

    public String getDvlaEmailTemplateId() {
        return dvlaEmailTemplateId;
    }

    public String getDvlaEmailAddress1() {
        return dvlaEmailAddress1;
    }

    public String getDvlaEmailAddress2() {
        return dvlaEmailAddress2;
    }

}
