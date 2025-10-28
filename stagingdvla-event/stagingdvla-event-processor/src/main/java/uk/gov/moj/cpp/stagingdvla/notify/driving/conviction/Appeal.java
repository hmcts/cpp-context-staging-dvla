package uk.gov.moj.cpp.stagingdvla.notify.driving.conviction;

import java.io.Serializable;

public class Appeal implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean appealAgainstConviction;

    private Boolean appealAgainstSentence;

    private Boolean appealAllowed;

    private Boolean appealDismissed;

    private Boolean appealAbandoned;

    private Boolean sentenceVaried;

    private String dismissalDate;

    private String abandonedDate;

    public Boolean getAppealAgainstConviction() {
        return appealAgainstConviction;
    }

    public Boolean getAppealAgainstSentence() {
        return appealAgainstSentence;
    }

    public Boolean getAppealAllowed() {
        return appealAllowed;
    }

    public Boolean getAppealDismissed() {
        return appealDismissed;
    }

    public Boolean getAppealAbandoned() {
        return appealAbandoned;
    }

    public Boolean getSentenceVaried() {
        return sentenceVaried;
    }

    public String getDismissalDate() {
        return dismissalDate;
    }

    public String getAbandonedDate() {
        return abandonedDate;
    }

    public static Builder appeal() {
        return new Appeal.Builder();
    }

    @Override
    public String toString() {
        return "Appeal{" +
                "appealAgainstConviction=" + appealAgainstConviction +
                ", appealAgainstSentence=" + appealAgainstSentence +
                ", appealAllowed=" + appealAllowed +
                ", appealDismissed=" + appealDismissed +
                ", appealAbandoned=" + appealAbandoned +
                ", sentenceVaried=" + sentenceVaried +
                ", dismissalDate='" + dismissalDate + '\'' +
                ", abandonedDate='" + abandonedDate + '\'' +
                '}';
    }

    public static final class Builder {
        private Boolean appealAgainstConviction;
        private Boolean appealAgainstSentence;
        private Boolean appealAllowed;
        private Boolean appealDismissed;
        private Boolean appealAbandoned;
        private Boolean sentenceVaried;
        private String dismissalDate;
        private String abandonedDate;

        private Builder() {
        }

        public static Builder anAppeal() {
            return new Builder();
        }

        public Builder withAppealAgainstConviction(Boolean appealAgainstConviction) {
            this.appealAgainstConviction = appealAgainstConviction;
            return this;
        }

        public Builder withAppealAgainstSentence(Boolean appealAgainstSentence) {
            this.appealAgainstSentence = appealAgainstSentence;
            return this;
        }

        public Builder withAppealAllowed(Boolean appealAllowed) {
            this.appealAllowed = appealAllowed;
            return this;
        }

        public Builder withAppealDismissed(Boolean appealDismissed) {
            this.appealDismissed = appealDismissed;
            return this;
        }

        public Builder withAppealAbandoned(Boolean appealAbandoned) {
            this.appealAbandoned = appealAbandoned;
            return this;
        }

        public Builder withSentenceVaried(Boolean sentenceVaried) {
            this.sentenceVaried = sentenceVaried;
            return this;
        }

        public Builder withDismissalDate(String dismissalDate) {
            this.dismissalDate = dismissalDate;
            return this;
        }

        public Builder withAbandonedDate(String abandonedDate) {
            this.abandonedDate = abandonedDate;
            return this;
        }

        public Appeal build() {
            final Appeal appeal = new Appeal();
            appeal.appealAgainstSentence = this.appealAgainstSentence;
            appeal.abandonedDate = this.abandonedDate;
            appeal.sentenceVaried = this.sentenceVaried;
            appeal.dismissalDate = this.dismissalDate;
            appeal.appealDismissed = this.appealDismissed;
            appeal.appealAllowed = this.appealAllowed;
            appeal.appealAbandoned = this.appealAbandoned;
            appeal.appealAgainstConviction = this.appealAgainstConviction;
            return appeal;
        }
    }
}
