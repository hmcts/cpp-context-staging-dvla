package uk.gov.moj.cpp.stagingdvla.service;

import static com.jayway.jsonassert.JsonAssert.with;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NotificationNotifyServiceTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private NotificationNotifyService notificationNotifyService;

    @Test
    public void shouldSendEmailNotification() {
        final String NOTIFICATION_ID = randomUUID().toString();
        final String TEMPLATE_ID = randomUUID().toString();
        final String SEND_TO_ADDRESS = "aa@aa.com";
        final String MATERIAL_URL = "my materialUrl";

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("notificationnotify.send-email-notification"),
                createObjectBuilder());

        final JsonObjectBuilder notifyObjectBuilder = createObjectBuilder();
        notifyObjectBuilder.add("notificationId", NOTIFICATION_ID);
        notifyObjectBuilder.add("templateId", TEMPLATE_ID);
        notifyObjectBuilder.add("sendToAddress", SEND_TO_ADDRESS);
        notifyObjectBuilder.add("materialUrl", MATERIAL_URL);

        notificationNotifyService.sendEmailNotification(event, notifyObjectBuilder.build());

        final ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(sender).sendAsAdmin(captor.capture());

        final Envelope<?> emailCommandEnvelope = captor.getValue();

        final Metadata metadata = emailCommandEnvelope.metadata();

        assertThat(metadata.name(), is("notificationnotify.send-email-notification"));

        final JsonObject payload = (JsonObject) emailCommandEnvelope.payload();

        with(payload.toString())
                .assertThat("notificationId", is(NOTIFICATION_ID))
                .assertThat("templateId", is(TEMPLATE_ID))
                .assertThat("sendToAddress", is(SEND_TO_ADDRESS))
                .assertThat("materialUrl", is(MATERIAL_URL));
    }

}
