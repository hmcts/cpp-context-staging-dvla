package uk.gov.moj.cpp.stagingdvla.domain.constants;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Lives in this module rather than stagingdvla-domain-common because that module declares no test
 * dependencies, and this is where the value is set and matched.
 */
public class StagingDvlaClientContextTest {

    @Test
    public void shouldRecogniseItsOwnClientContext() {
        assertThat(StagingDvlaClientContext.isOurs(StagingDvlaClientContext.CLIENT_CONTEXT), is(true));
    }

    @Test
    public void shouldRejectAnotherContextsValue() {
        // correspondence sets the literal "correspondence" in this same field
        assertThat(StagingDvlaClientContext.isOurs("correspondence"), is(false));
    }

    @Test
    public void shouldRejectAnAbsentValue() {
        // clientContext is optional on both notification events, and notificationnotify's
        // markAsInvalid route omits it entirely
        assertThat(StagingDvlaClientContext.isOurs(null), is(false));
    }

    @Test
    public void shouldMatchExactlyRatherThanLoosely() {
        assertThat(StagingDvlaClientContext.isOurs("staging_dvla"), is(false));
        assertThat(StagingDvlaClientContext.isOurs("STAGING_DVLA_AUDIT"), is(false));
        assertThat(StagingDvlaClientContext.isOurs(""), is(false));
    }
}
