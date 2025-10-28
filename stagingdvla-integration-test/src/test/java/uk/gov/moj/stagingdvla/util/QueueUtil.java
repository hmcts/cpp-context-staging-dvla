package uk.gov.moj.stagingdvla.util;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.junit.Assert.fail;
import static uk.gov.moj.stagingdvla.util.OptionalPresent.ifPresent;

import java.io.StringReader;
import java.util.Optional;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.json.Json;
import javax.json.JsonObject;

import io.restassured.path.json.JsonPath;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class QueueUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueUtil.class);

    private static final String EVENT_SELECTOR_TEMPLATE = "CPPNAME IN ('%s')";

    private static final String HOST = System.getProperty("INTEGRATION_HOST_KEY", "localhost");

    private static final String QUEUE_URI = System.getProperty("queueUri", "tcp://" + HOST + ":61616");

    private static final long RETRIEVE_TIMEOUT = 10000;
    private static final long MESSAGE_RETRIEVE_TRIAL_TIMEOUT = 10000;
    private static final int RETRY_TIMEOUT_IN_MILLIS = 5000;
    private static final int DEFAULT_POLL_TIMEOUT_IN_MILLIS = 30000;

    private Session session;

    private Topic topic;

    private Connection connection;

    public static final QueueUtil privateEvents = new QueueUtil("stagingdvla.event.event");

    public static final QueueUtil publicEvents = new QueueUtil("jms.topic.public.event");

    private QueueUtil(final String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            final Connection connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
        } catch (final JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumer(final String eventSelector) {
        try {
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageProducer createProducer() {
        try {
            return session.createProducer(topic);
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageConsumer createConsumerForMultipleSelectors(final String... eventSelectors) {
        final StringBuffer str = new StringBuffer("CPPNAME IN (");
        for (int i = 0; i < eventSelectors.length; i++) {
            if (i != 0) {
                str.append(", ");
            }

            str.append("'").append(eventSelectors[i]).append("'");
        }
        str.append(")");

        try {
            return session.createConsumer(topic, str.toString());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isAlive(Connection connection) {
        try {
            return (connection != null && connection.getMetaData() != null);
        } catch (JMSException ex) {
            LOGGER.error("Failed on isAlive", ex);
            return false;
        }
    }

    public MessageConsumer createPrivateConsumer(final String eventSelector) {
        try {
            if (!isAlive(connection)) {
                initialize("jms.topic.stagingdvla.event");
            }
            return session.createConsumer(topic, String.format(EVENT_SELECTOR_TEMPLATE, eventSelector));
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer, final Matcher matchers) {
        final long startTime = System.currentTimeMillis();
        JsonPath message;
        do {
            message = retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
            if (ofNullable(message).isPresent()) {
                if (matchers.matches(message.prettify())) {
                    return message;
                }
            }
        } while (MESSAGE_RETRIEVE_TRIAL_TIMEOUT > (System.currentTimeMillis() - startTime));
        return null;
    }

    public static JsonPath retrieveMessage(final MessageConsumer consumer) {
        return retrieveMessage(consumer, RETRIEVE_TIMEOUT).orElse(null);
    }

    public static Optional<JsonPath> retrieveMessage(final MessageConsumer consumer, final long customTimeOutInMillis) {
        return ifPresent(retrieveMessageAsString(consumer, customTimeOutInMillis),
                (x) -> Optional.of(new JsonPath(x))
        ).orElse(Optional::empty);
    }

    public static Optional<JsonObject> retrieveMessageAsJsonObject(final MessageConsumer consumer) {
        return ifPresent(retrieveMessageAsString(consumer, RETRIEVE_TIMEOUT),
                (x) -> Optional.of(Json.createReader(new StringReader(x)).readObject())
        ).orElse(Optional::empty);
    }

    public static Optional<String> retrieveMessageAsString(final MessageConsumer consumer, final long customTimeOutInMillis) {
        try {
            final TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
            if (message == null) {
                LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                return Optional.empty();
            }
            return Optional.of(message.getText());
        } catch (final JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public static QueueUtil getPrivateTopicInstance(final String topicName) {
        return new QueueUtil(topicName);
    }

    public static QueueUtil getPublicTopicInstance() {
        return new QueueUtil("jms.topic.public.event");
    }

    public static EventListener listenFor(final String mediaType, final String topicName) {
        return new EventListener(mediaType, DEFAULT_POLL_TIMEOUT_IN_MILLIS, topicName);
    }

    public static EventListener listenFor(final String mediaType) {
        return new EventListener(mediaType);
    }

    public static EventListener listenFor(final String mediaType, final long timeout) {
        return new EventListener(mediaType, timeout);
    }

    private void initialize(String topicName) {
        try {
            LOGGER.info("Artemis URI: {}", QUEUE_URI);
            final ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(QUEUE_URI);
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic = new ActiveMQTopic(topicName);
        } catch (final JMSException e) {
            LOGGER.error("Fatal error initialising Artemis", e);
            throw new RuntimeException(e);
        }
    }

    public static class EventListener implements AutoCloseable {

        private static final Logger LOGGER = LoggerFactory.getLogger(EventListener.class);
        private MessageConsumer messageConsumer;
        private String eventType;
        private Matcher<?> matcher;
        private long timeout;
        private QueueUtil queueUtil;

        public EventListener(final String eventType) {
            this(eventType, DEFAULT_POLL_TIMEOUT_IN_MILLIS);
        }

        public EventListener(final String eventType, final long timeout) {
            this.eventType = eventType;
            this.queueUtil = getPublicTopicInstance();
            this.messageConsumer = queueUtil.createConsumer(eventType);
            this.timeout = timeout;
        }

        public EventListener(final String eventType, final long timeout, final String topicName) {
            this.eventType = eventType;
            this.queueUtil = getPrivateTopicInstance(topicName);
            this.messageConsumer = queueUtil.createConsumer(eventType);
            this.timeout = timeout;
        }

        public void expectNoneWithin(final long timeout) {
            JsonPath message = retrieveMessage(messageConsumer, timeout);

            while (message != null && !this.matcher.matches(message.prettify())) {
                message = retrieveMessage(messageConsumer);
            }
            if (message != null) {
                fail(format("expected no messages but got %s", message.prettify()));
            }
        }

        public JsonPath waitFor() {
            int numberOfRetries = 1;
            final long startTime = System.currentTimeMillis();
            JsonPath message;
            StringDescription description = new StringDescription();
            do {
                message = retrieveMessage(messageConsumer, RETRY_TIMEOUT_IN_MILLIS);

                if (message != null) {
                    if (this.matcher.matches(message.prettify())) {
                        LOGGER.info("message:" + message.prettify());
                        return message;
                    } else {
                        description = new StringDescription();
                        description.appendText("Expected ");
                        this.matcher.describeTo(description);
                        description.appendText(" but ");
                        this.matcher.describeMismatch(message.prettify(), description);
                    }
                }
                numberOfRetries++;

            } while (timeout > (System.currentTimeMillis() - startTime));

            fail("Expected '" + eventType + "' Retries " + numberOfRetries + "  message to emit on the jms.topic.public.event topic: " + description.toString());
            return null;
        }

        public EventListener withFilter(final Matcher<?> matcher) {
            this.matcher = matcher;
            return this;
        }

        @Override
        public void close() {
            try {
                messageConsumer.close();
            } catch (JMSException ignored) {
            }

        }

        public static JsonPath retrieveMessage(final MessageConsumer consumer) {
            return retrieveMessage(consumer, RETRIEVE_TIMEOUT);
        }

        public static JsonPath retrieveMessage(final MessageConsumer consumer, final long customTimeOutInMillis) {
            final String messageString = retrieveMessageString(consumer, customTimeOutInMillis);
            return messageString != null ? new JsonPath(messageString) : null;
        }

        public static String retrieveMessageString(final MessageConsumer consumer, final long customTimeOutInMillis) {
            try {
                final TextMessage message = (TextMessage) consumer.receive(customTimeOutInMillis);
                if (message == null) {
                    LOGGER.error("No message retrieved using consumer with selector {}", consumer.getMessageSelector());
                    return null;
                }
                return message.getText();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
    }
}