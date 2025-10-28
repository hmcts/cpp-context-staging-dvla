package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.stagingdvla.query.api.service.UserDetails;
import uk.gov.moj.cpp.stagingdvla.query.api.service.UserGroupService;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UserGroupServiceTest {


    @InjectMocks
    private UserGroupService userGroupService;

    @Mock
    private Requester requester;

    private static final UUID ID = UUID.randomUUID();

    @Test
    public void shouldGetUserDetailsByDriverNumber() {

        // given
        final Metadata metadata = Envelope.metadataBuilder()
                .withName("stagingdvla.query.drivernumber")
                .withId(randomUUID())
                .withUserId(ID.toString())
                .build();

        JsonObject jsonObject = createObjectBuilder()
                .add("userId", ID.toString())
                .add("email", "peter21")
                .build();

        final Envelope<JsonObject> jsonObjectEnvelope = Envelope.envelopeFrom(metadata, jsonObject);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(jsonObjectEnvelope);

        //When
        UserDetails userById = userGroupService.getUserById(metadata);

        //Then
        assertThat(userById.getUserId(), is(ID));
        assertThat(userById.getEmail(), is("peter21"));

    }

    @Test
    public void shouldGetUserDetailsByDriverDetails() {

        // given
        final Metadata metadata = Envelope.metadataBuilder()
                .withName("stagingdvla.query.driverdetails")
                .withId(randomUUID())
                .withUserId(ID.toString())
                .build();

        JsonObject jsonObject = createObjectBuilder()
                .add("userId", ID.toString())
                .add("email", "peter21")
                .build();

        final Envelope<JsonObject> jsonObjectEnvelope = Envelope.envelopeFrom(metadata, jsonObject);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class))).thenReturn(jsonObjectEnvelope);

        //When
        UserDetails userById = userGroupService.getUserById(metadata);

        //Then
        assertThat(userById.getUserId(), is(ID));
        assertThat(userById.getEmail(), is("peter21"));
    }
}
