package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class Error implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status;

    private String code;

    private String detail;

    public String getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getDetail() {
        return detail;
    }

    public static Builder error() {
        return new Error.Builder();
    }

    @Override
    public String toString() {
        return "Error{" +
                "status='" + status + '\'' +
                ", code='" + code + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }

    public static final class Builder {
        private String status;
        private String code;
        private String detail;

        private Builder() {
        }

        public static Builder anError() {
            return new Builder();
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public Builder withCode(String code) {
            this.code = code;
            return this;
        }

        public Builder withDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public Error build() {
            final Error error = new Error();
            error.detail = this.detail;
            error.status = this.status;
            error.code = this.code;
            return error;
        }
    }
}