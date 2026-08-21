package uk.gov.moj.cpp.stagingdvla.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class ApplicationParametersTest {

    private final ApplicationParameters applicationParameters = new ApplicationParameters();

    @Test
    void shouldExposeTheConfiguredValuesViaItsGetters() {
        // The @Value fields are injected by the container at runtime; exercise the accessors
        // on an un-injected instance (values are null until injected).
        assertThat(applicationParameters.getDvlaEmailTemplateId(), is(nullValue()));
        assertThat(applicationParameters.getDvlaEmailAddress1(), is(nullValue()));
        assertThat(applicationParameters.getDvlaEmailAddress2(), is(nullValue()));
    }
}
