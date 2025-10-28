package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;

public class Error implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String status;
    private String detail;

    public static class Builder {
        private String title;
        private String status;
        private String detail;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Error build() {
            return new Error(title, status, detail);
        }
    }

    private Error(String title, String status, String detail) {
        this.title = title;
        this.status = status;
        this.detail = detail;
    }

    public Error() {}

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public String getDetail() {
        return detail;
    }

}