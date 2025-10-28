package uk.gov.moj.cpp.stagingdvla.service;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class DocumentGenerationRequest {

    private final String originatingSource;
    private final String templateIdentifier;
    private final ConversionFormat conversionFormat;
    private final String sourceCorrelationId;
    private final UUID payloadFileServiceId;

    public DocumentGenerationRequest(final String originatingSource,
                                     final String templateIdentifier,
                                     final ConversionFormat conversionFormat,
                                     final String sourceCorrelationId,
                                     final UUID payloadFileServiceId) {
        this.originatingSource = originatingSource;
        this.templateIdentifier = templateIdentifier;
        this.conversionFormat = conversionFormat;
        this.sourceCorrelationId = sourceCorrelationId;
        this.payloadFileServiceId = payloadFileServiceId;
    }

    public String getOriginatingSource() {
        return originatingSource;
    }

    public String getTemplateIdentifier() {
        return templateIdentifier;
    }

    public ConversionFormat getConversionFormat() {
        return conversionFormat;
    }

    public String getSourceCorrelationId() {
        return sourceCorrelationId;
    }

    public UUID getPayloadFileServiceId() {
        return payloadFileServiceId;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}