package uk.gov.moj.cpp.stagingdvla.query.view.request;

public class SearchReason {
    private final String reasonType;
    private final String reference;

    public SearchReason(final String reasonType, final String reference) {
        this.reasonType = reasonType;
        this.reference = reference;
    }

    private SearchReason(final Builder builder) {
        reasonType = builder.reasonType;
        reference = builder.reference;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getReasonType() {
        return reasonType;
    }

    public String getReference() {
        return reference;
    }


    public static final class Builder {
        private String reasonType;
        private String reference;

        private Builder() {
        }

        public Builder reasonType(final String reasonType) {
            this.reasonType = reasonType;
            return this;
        }

        public Builder reference(final String val) {
            reference = val;
            return this;
        }

        public SearchReason build() {
            return new SearchReason(this);
        }
    }
}
