package uk.gov.moj.cpp.stagingdvla.query.api.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;


public class UserGroupService {
    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;


    public UserDetails getUserById(final Metadata data) {
        final UUID userId = fromString(data.userId().orElseThrow(() -> new RuntimeException("UserId missing from event.")));

        final Metadata metadata = metadataFrom(data).withName("usersgroups.get-user-details").build();
        final Envelope<JsonValue> requestEnvelope = envelopeFrom(metadata, createObjectBuilder().add("userId",
                userId.toString()).build());
        final Envelope<JsonObject> jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        final JsonObject userJson = jsonResultEnvelope.payload();
        final UserDetails userDetails = new UserDetails();
        if (nonNull(userJson)) {
            userDetails.setEmail(ofNullable(userJson.getString("email")).orElse(""));
        }
        userDetails.setUserId(userId);
        return userDetails;
    }
}


