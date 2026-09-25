package uk.gov.moj.cpp.stagingdvla.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.cpp.stagingdvla.command.handler.DriverNotificationDocumentDelivery;
import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.aggregate.MaterialAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DriverNotificationDocumentDeliveryHandlerTest {

    private static final String STAGINGDVLA_EVENT_DVLA_DOCUMENT_DELIVERY_RECORDED = "stagingdvla.event.dvla-document-delivery-recorded";

    private static final UUID MATERIAL_ID = randomUUID();
    private static final String MATERIAL_STATUS = "PENDING";
    private static final String PAYLOAD_BLOB_URI = "payload/blob/uri";
    private static final String DOCUMENT_BLOB_URI = "document/blob/uri";

    @InjectMocks
    private DriverNotificationDocumentDeliveryHandler handler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(DvlaDocumentDeliveryRecorded.class);

    @Test
    public void shouldHandleDocumentDelivery() {
        assertThat(handler, isHandler(COMMAND_HANDLER)
                .with(method("handleDocumentDelivery")
                        .thatHandles(DriverNotificationDocumentDeliveryHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY)));
    }

    @Test
    public void shouldRecordDocumentDeliveryAndRaiseEvent() throws EventStreamException {
        final MaterialAggregate materialAggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);

        final Envelope<DriverNotificationDocumentDelivery> envelope = createDocumentDeliveryEnvelope();
        handler.handleDocumentDelivery(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(STAGINGDVLA_EVENT_DVLA_DOCUMENT_DELIVERY_RECORDED),
                        payload().isJson(allOf(
                                withJsonPath("$.materialId", is(MATERIAL_ID.toString())),
                                withJsonPath("$.materialStatus", is(MATERIAL_STATUS)),
                                withJsonPath("$.payloadBlobUri", is(PAYLOAD_BLOB_URI)),
                                withJsonPath("$.documentBlobUri", is(DOCUMENT_BLOB_URI))
                        ))
                )
        ));
    }

    @Test
    public void shouldRecordDocumentDeliveryWithSjpCaseFieldsAndRaiseEvent() throws EventStreamException {
        final MaterialAggregate materialAggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);

        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        final DriverNotificationDocumentDelivery documentDelivery = DriverNotificationDocumentDelivery
                .driverNotificationDocumentDelivery()
                .withMaterialId(MATERIAL_ID)
                .withPayloadBlobUri("url")
                .withCaseId(caseId)
                .withSjpCorrelationId(sjpCorrelationId)
                .withSjpStatus("PENDING")
                .withMaterialStatus("PENDING")
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        final Envelope<DriverNotificationDocumentDelivery> envelope = Enveloper.envelop(documentDelivery)
                .withName(DriverNotificationDocumentDeliveryHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY)
                .withMetadataFrom(requestEnvelope);

        handler.handleDocumentDelivery(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName(STAGINGDVLA_EVENT_DVLA_DOCUMENT_DELIVERY_RECORDED),
                        payload().isJson(allOf(
                                withJsonPath("$.materialId", is(MATERIAL_ID.toString())),
                                withJsonPath("$.caseId", is(caseId.toString())),
                                withJsonPath("$.sjpCorrelationId", is(sjpCorrelationId.toString())),
                                withJsonPath("$.sjpStatus", is("PENDING"))
                        ))
                )
        ));
    }

    private Envelope<DriverNotificationDocumentDelivery> createDocumentDeliveryEnvelope() {
        final DriverNotificationDocumentDelivery documentDelivery = DriverNotificationDocumentDelivery
                .driverNotificationDocumentDelivery()
                .withMaterialId(MATERIAL_ID)
                .withMaterialStatus(MATERIAL_STATUS)
                .withPayloadBlobUri(PAYLOAD_BLOB_URI)
                .withDocumentBlobUri(DOCUMENT_BLOB_URI)
                .build();

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(randomUUID().toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(documentDelivery)
                .withName(DriverNotificationDocumentDeliveryHandler.STAGINGDVLA_COMMAND_HANDLER_DRIVER_NOTIFICATION_DOCUMENT_DELIVERY)
                .withMetadataFrom(requestEnvelope);
    }
}
