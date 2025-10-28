package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class DriverSummaryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<DriverSummary> results;
    private Error error;
    private String count;

    public DriverSummaryResponse(final List<DriverSummary> results, final Error error) {
        this.results = results;
        this.error = error;
    }

    public DriverSummaryResponse(){}

    public List<DriverSummary> getResults() {
        return results;
    }

    public Error getError() {
        return error;
    }

    public String getCount() {
        return count;
    }

    public void setCount(final String count) {
        this.count = count;
    }

    public static final class Builder {
        private List<DriverSummary> results;
        private Error error;

        public Builder results(final List<DriverSummary> results) {
            this.results = results;
            return this;
        }

        public Builder error(final Error error) {
            this.error = error;
            return this;
        }

        public DriverSummaryResponse build() {
            return new DriverSummaryResponse(results, error);
        }
    }
}
