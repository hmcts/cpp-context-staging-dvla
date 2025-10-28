package uk.gov.moj.cpp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "case_text")
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public class CaseTextEntity implements Serializable {

    private static final long serialVersionUID = 2441781778236204986L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "case_id", unique = false, nullable = false)
    private UUID caseId;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Column(name = "created_date_time", nullable = false)
    private ZonedDateTime createdDateTime;


    public CaseTextEntity(final UUID caseId, final String text, final ZonedDateTime createdDateTime) {
        this.id = UUID.randomUUID();
        this.caseId = caseId;
        this.text = text;
        this.createdDateTime = createdDateTime;
    }

    public CaseTextEntity() {
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getText() {
        return text;
    }


    public ZonedDateTime getCreatedDateTime() {
        return createdDateTime;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public void setCreatedDateTime(final ZonedDateTime createdDateTime) {
        this.createdDateTime = createdDateTime;
    }
}
