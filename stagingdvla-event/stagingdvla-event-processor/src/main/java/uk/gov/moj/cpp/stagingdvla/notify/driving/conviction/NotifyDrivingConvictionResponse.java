package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class NotifyDrivingConvictionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer status;

    private String errors;

    public Integer getStatus() {
        return status;
    }

    public String getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "NotifyDrivingConvictionResponse{" +
                "status=" + status +
                ", errors='" + errors + '\'' +
                '}';
    }

    public static Builder notifyDrivingConvictionResponse() {
        return new NotifyDrivingConvictionResponse.Builder();
    }


    public static final class Builder {
        private Integer status;
        private String errors;

        private Builder() {
        }

        public static Builder aNotifyDrivingConvictionResponse() {
            return new Builder();
        }

        public Builder withStatus(Integer status) {
            this.status = status;
            return this;
        }

        public Builder withErrors(String errors) {
            this.errors = errors;
            return this;
        }

        public NotifyDrivingConvictionResponse build() {
            final NotifyDrivingConvictionResponse notifyDrivingConvictionResponse = new NotifyDrivingConvictionResponse();
            notifyDrivingConvictionResponse.status = this.status;
            notifyDrivingConvictionResponse.errors = this.errors;
            return notifyDrivingConvictionResponse;
        }
    }
}
