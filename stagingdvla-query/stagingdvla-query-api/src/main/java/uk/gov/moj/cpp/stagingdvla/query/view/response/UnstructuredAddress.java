package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class UnstructuredAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    private String line1;
    private String line2;
    private String line3;
    private String line4;
    private String line5;
    private String postcode;

    public static class Builder {
        private String line1;
        private String line2;
        private String line3;
        private String line4;
        private String line5;
        private String postcode;

        public Builder line1(String line1) {
            this.line1 = line1;
            return this;
        }

        public Builder line2(String line2) {
            this.line2 = line2;
            return this;
        }

        public Builder line3(String line3) {
            this.line3 = line3;
            return this;
        }

        public Builder line4(String line4) {
            this.line4 = line4;
            return this;
        }

        public Builder line5(String line5) {
            this.line5 = line5;
            return this;
        }

        public Builder postcode(String postcode) {
            this.postcode = postcode;
            return this;
        }

        public UnstructuredAddress build() {
            return new UnstructuredAddress(line1, line2, line3, line4, line5, postcode);
        }
    }

    private UnstructuredAddress(String line1, String line2, String line3, String line4, String line5, String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.line3 = line3;
        this.line4 = line4;
        this.line5 = line5;
        this.postcode = postcode;
    }

    public UnstructuredAddress(){}

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

    public String getLine5() {
        return line5;
    }

    public String getPostcode() {
        return postcode;
    }
}
