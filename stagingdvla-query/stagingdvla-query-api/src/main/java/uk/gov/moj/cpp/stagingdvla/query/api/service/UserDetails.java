package uk.gov.moj.cpp.stagingdvla.query.api.service;

import java.util.UUID;

public class UserDetails {
    private UUID userId;
    private String email;

    public UserDetails() {
    }

    public UserDetails(final UUID userId, final String email) {
        this.userId = userId;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(final UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
}
