package uk.gov.moj.cpp.stagingdvla.processor.helper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.util.List;

public class EnvelopeHelper {


    public static void verifySendAtIndex(final List<Envelope<?>> messageEnvelope,
                                         final String commandOrEventName, final int index) {
        final DefaultEnvelope argumentCaptorValue = (DefaultEnvelope) messageEnvelope.get(index);
        assertThat(argumentCaptorValue.metadata().name(), is(commandOrEventName));
    }
}
