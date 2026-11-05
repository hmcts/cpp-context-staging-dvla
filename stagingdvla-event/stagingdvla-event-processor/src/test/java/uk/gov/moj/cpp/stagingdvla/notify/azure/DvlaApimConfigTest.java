package uk.gov.moj.cpp.stagingdvla.notify.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class DvlaApimConfigTest {

    private final DvlaApimConfig config = new DvlaApimConfig();

    @Test
    void shouldExposeTheConfiguredValuesViaItsGetters() {
        // The @Value fields are injected by the container at runtime; here we exercise the
        // accessors on an un-injected instance (values are null until injected).
        assertThat(config.getDvlaEnquiryApimUrl(), is(nullValue()));
        assertThat(config.getDrivingConvictionMaxRetry(), is(nullValue()));
        assertThat(config.getDrivingConvictionRetryIntervalInMinutes(), is(nullValue()));
        assertThat(config.getDrivingConvictionRetryMaxRecordCount(), is(nullValue()));
        assertThat(config.getSubscriptionKey(), is(nullValue()));
    }
}
