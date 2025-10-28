package uk.gov.moj.cpp.persistence.entity;


import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "driving_conviction_retry")
public class DrivingConvictionRetryEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "conviction_id", nullable = false)
    private UUID convictionId;

    @Column(name = "master_defendant_id", nullable = false)
    private UUID masterDefendantId;

    @Column(name = "created_date_time", nullable = false)
    private ZonedDateTime createdDateTime;

    public DrivingConvictionRetryEntity() {
    }

    public DrivingConvictionRetryEntity(final UUID convictionId, final UUID masterDefendantId, final ZonedDateTime createdDateTime) {
        this.convictionId = convictionId;
        this.masterDefendantId = masterDefendantId;
        this.createdDateTime = createdDateTime;
    }

    public UUID getConvictionId() {
        return convictionId;
    }

    public void setConvictionId(final UUID convictionId) {
        this.convictionId = convictionId;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(final UUID masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setCreatedDateTime(final ZonedDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }
}
