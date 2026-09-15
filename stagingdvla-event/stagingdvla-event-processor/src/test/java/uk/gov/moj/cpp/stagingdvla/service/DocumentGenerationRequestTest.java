package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class DocumentGenerationRequestTest {

    private static final String ORIGINATING_SOURCE = "DVLADocumentOrder";
    private static final String TEMPLATE_IDENTIFIER = "EDT_DriverOutNotification";

    @Test
    public void shouldExposeAllConstructorSuppliedFieldsViaGetters() {
        final UUID payloadFileServiceId = randomUUID();
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.PDF, "correlation-1", payloadFileServiceId,
                "payload-file-uri", "destination-file-uri");

        assertThat(request.getOriginatingSource(), is(ORIGINATING_SOURCE));
        assertThat(request.getTemplateIdentifier(), is(TEMPLATE_IDENTIFIER));
        assertThat(request.getConversionFormat(), is(ConversionFormat.PDF));
        assertThat(request.getSourceCorrelationId(), is("correlation-1"));
        assertThat(request.getPayloadFileServiceId(), is(payloadFileServiceId));
        assertThat(request.getPayloadFileUri(), is("payload-file-uri"));
        assertThat(request.getDestinationFileUri(), is("destination-file-uri"));
    }

    @Test
    public void shouldTreatTwoRequestsWithTheSameFieldValuesAsEqualWithMatchingHashCode() {
        final UUID payloadFileServiceId = randomUUID();
        final DocumentGenerationRequest first = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.CSV, "correlation-1", payloadFileServiceId, null, null);
        final DocumentGenerationRequest second = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.CSV, "correlation-1", payloadFileServiceId, null, null);

        assertThat(first, is(second));
        assertThat(first.hashCode(), is(second.hashCode()));
    }

    @Test
    public void shouldTreatRequestsWithDifferentFieldValuesAsNotEqual() {
        final DocumentGenerationRequest first = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.CSV, "correlation-1", randomUUID(), null, null);
        final DocumentGenerationRequest second = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.CSV, "correlation-2", randomUUID(), null, null);

        assertThat(first, is(not(second)));
    }

    @Test
    public void shouldIncludeFieldValuesInToString() {
        final DocumentGenerationRequest request = new DocumentGenerationRequest(
                ORIGINATING_SOURCE, TEMPLATE_IDENTIFIER, ConversionFormat.PDF, "correlation-1", null, "payload-uri", "destination-uri");

        final String stringRepresentation = request.toString();

        assertThat(stringRepresentation.contains(ORIGINATING_SOURCE), is(true));
        assertThat(stringRepresentation.contains(TEMPLATE_IDENTIFIER), is(true));
        assertThat(stringRepresentation.contains("correlation-1"), is(true));
        assertThat(stringRepresentation.contains("payload-uri"), is(true));
        assertThat(stringRepresentation.contains("destination-uri"), is(true));
    }
}
