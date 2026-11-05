package uk.gov.moj.cpp.stagingdvla.azure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.jupiter.api.Test;

public class AzureEmulatorConfigurationTest {

    @Test
    public void shouldBeLocalEmulatorEnabledWhenFlagIsTrue() {
        final AzureEmulatorConfiguration configuration = configurationWith("true");

        assertThat(configuration.isLocalEmulatorEnabled(), is(true));
    }

    @Test
    public void shouldNotBeLocalEmulatorEnabledWhenFlagIsFalse() {
        final AzureEmulatorConfiguration configuration = configurationWith("false");

        assertThat(configuration.isLocalEmulatorEnabled(), is(false));
    }

    @Test
    public void shouldNotBeLocalEmulatorEnabledWhenFlagIsMissing() {
        final AzureEmulatorConfiguration configuration = configurationWith(null);

        assertThat(configuration.isLocalEmulatorEnabled(), is(false));
    }

    private AzureEmulatorConfiguration configurationWith(final String localEmulatorEnabled) {
        final AzureEmulatorConfiguration configuration = new AzureEmulatorConfiguration();
        setField(configuration, "localEmulatorEnabled", localEmulatorEnabled);
        return configuration;
    }
}
