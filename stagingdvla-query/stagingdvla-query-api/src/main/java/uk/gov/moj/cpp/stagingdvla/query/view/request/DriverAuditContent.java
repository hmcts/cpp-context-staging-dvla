package uk.gov.moj.cpp.stagingdvla.query.view.request;

import java.time.ZonedDateTime;
import java.util.UUID;

public class DriverAuditContent {

    private final UUID id;
    private final UUID userId;
    private final String userEmail;
    private final ZonedDateTime dateTime;
    private final SearchReason searchReason;
    private final SearchCriteria searchCriteria;


    public DriverAuditContent(final UUID id, final UUID userId, final String userEmail, final ZonedDateTime dateTime, final SearchReason searchReason, final SearchCriteria searchCriteria) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.dateTime = dateTime;
        this.searchReason = searchReason;
        this.searchCriteria = searchCriteria;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    public SearchReason getSearchReason() {
        return searchReason;
    }

    public SearchCriteria getSearchCriteria() {
        return searchCriteria;
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private String userEmail;
        private ZonedDateTime dateTime;
        private SearchReason searchReason;
        private SearchCriteria searchCriteria;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder userEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public Builder dateTime(ZonedDateTime dateTime) {
            this.dateTime = dateTime;
            return this;
        }

        public Builder searchReason(SearchReason searchReason) {
            this.searchReason = searchReason;
            return this;
        }

        public Builder searchCriteria(SearchCriteria searchCriteria) {
            this.searchCriteria = searchCriteria;
            return this;
        }

        public DriverAuditContent build() {
            return new DriverAuditContent(id, userId, userEmail, dateTime, searchReason, searchCriteria);
        }
    }
}
