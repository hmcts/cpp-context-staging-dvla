package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotifyDrivingConviction implements Serializable {
    private static final long serialVersionUID = 1L;

    private Conviction conviction;

    private List<Offence> offences;

    private Appeal appeal;

    public Conviction getConviction() {
        return conviction;
    }

    public List<Offence> getOffences() {
        return Collections.unmodifiableList(offences);
    }

    public Appeal getAppeal() {
        return appeal;
    }

    public static Builder notifyDrivingConviction() {
        return new NotifyDrivingConviction.Builder();
    }

    @Override
    public String toString() {
        return "NotifyDrivingConviction{" +
                "conviction=" + conviction +
                ", offences=" + offences +
                ", appeal=" + appeal +
                '}';
    }

    public static final class Builder {
        private Conviction conviction;
        private List<Offence> offences;
        private Appeal appeal;

        private Builder() {
        }

        public static Builder aNotifyDrivingConviction() {
            return new Builder();
        }

        public Builder withConviction(Conviction conviction) {
            this.conviction = conviction;
            return this;
        }

        public Builder withOffences(List<Offence> offences) {
            this.offences = Collections.unmodifiableList(new ArrayList<>(offences));
            return this;
        }

        public Builder withAppeal(Appeal appeal) {
            this.appeal = appeal;
            return this;
        }

        public NotifyDrivingConviction build() {
            final NotifyDrivingConviction notifyDrivingConviction = new NotifyDrivingConviction();
            notifyDrivingConviction.conviction = this.conviction;
            notifyDrivingConviction.appeal = this.appeal;
            notifyDrivingConviction.offences = this.offences;
            return notifyDrivingConviction;
        }
    }
}