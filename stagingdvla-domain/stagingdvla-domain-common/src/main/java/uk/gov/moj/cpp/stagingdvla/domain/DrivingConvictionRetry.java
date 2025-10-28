package uk.gov.moj.cpp.stagingdvla.domain;

import java.io.Serializable;
import java.util.UUID;

@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class DrivingConvictionRetry implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID convictionId;
    private final UUID masterDefendantId;

    public DrivingConvictionRetry(final UUID convictionId, final UUID masterDefendantId) {
        this.convictionId = convictionId;
        this.masterDefendantId = masterDefendantId;
    }

    public UUID getConvictionId() {
        return convictionId;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public static Builder drivingConvictionRetry() {
        return new Builder();
    }

    public static class Builder {
        private UUID convictionId;
        private UUID masterDefendantId;

        public Builder withConvictionId(final UUID convictionId) {
            this.convictionId = convictionId;
            return this;
        }

        public Builder withMasterDefendantId(final UUID masterDefendantId) {
            this.masterDefendantId = masterDefendantId;
            return this;
        }

        public Builder withValuesFrom(final DrivingConvictionRetry drivingConvictionRetry) {
            this.convictionId = drivingConvictionRetry.getConvictionId();
            this.masterDefendantId = drivingConvictionRetry.getMasterDefendantId();
            return this;
        }

        public DrivingConvictionRetry build() {
            return new DrivingConvictionRetry(convictionId, masterDefendantId);
        }
    }
}
