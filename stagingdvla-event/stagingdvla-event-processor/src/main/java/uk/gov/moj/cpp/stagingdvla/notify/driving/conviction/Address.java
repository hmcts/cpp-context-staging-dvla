package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class Address implements Serializable {
    private static final long serialVersionUID = 1L;

    private String line1;

    private String line2;

    private String line3;

    private String line4;

    private String postcode;

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getLine3() {
        return line3;
    }

    public String getLine4() {
        return line4;
    }

    public String getPostcode() {
        return postcode;
    }

    public static Builder address() {
        return new Address.Builder();
    }

    @Override
    public String toString() {
        return "Address{" +
                "line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", line3='" + line3 + '\'' +
                ", line4='" + line4 + '\'' +
                ", postcode='" + postcode + '\'' +
                '}';
    }

    public static final class Builder {
        private String line1;
        private String line2;
        private String line3;
        private String line4;
        private String postcode;

        private Builder() {
        }

        public static Builder anAddress() {
            return new Builder();
        }

        public Builder withLine1(String line1) {
            this.line1 = line1;
            return this;
        }

        public Builder withLine2(String line2) {
            this.line2 = line2;
            return this;
        }

        public Builder withLine3(String line3) {
            this.line3 = line3;
            return this;
        }

        public Builder withLine4(String line4) {
            this.line4 = line4;
            return this;
        }

        public Builder withPostcode(String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Address build() {
            final Address address = new Address();
            address.line1 = this.line1;
            address.postcode = this.postcode;
            address.line3 = this.line3;
            address.line4 = this.line4;
            address.line2 = this.line2;
            return address;
        }
    }
}