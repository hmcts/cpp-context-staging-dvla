package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.cpp.stagingdvla.event.DocumentDeletedFromBlob;
import uk.gov.justice.cpp.stagingdvla.event.DvlaDocumentDeliveryRecorded;

import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialAggregateTest {

    private static final String PAYLOAD_BLOB_URI = "payload/blob/uri";
    private static final String DOCUMENT_BLOB_URI = "document/blob/uri";

    @InjectMocks
    private MaterialAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new MaterialAggregate();
    }

    @Test
    public void shouldCreate() {
        final UUID applicationId = randomUUID();
        final UUID materialId = randomUUID();
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withApplicationId(applicationId)
                .withMaterialId(materialId)
                .build();
        aggregate.apply(materialDetails);
        final List<Object> eventStream = aggregate.create(materialDetails).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowsMaterialRequestRecorded.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(NowsMaterialRequestRecorded.class)));
    }

    @Test
    public void shouldRecordDocumentDelivery() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "PENDING", "payload/blob/uri", "document/blob/uri", null, null, null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getMaterialId(), is(materialId));
        assertThat(event.getMaterialStatus(), is("PENDING"));
        assertThat(event.getPayloadBlobUri(), is("payload/blob/uri"));
        assertThat(event.getDocumentBlobUri(), is("document/blob/uri"));
    }

    @Test
    public void shouldRecordDocumentDeliveryWithSjpCaseFields() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();

        aggregate.recordDocumentDelivery(
                materialId, "PENDING", "payload/blob/uri", "document/blob/uri", null, null, null).collect(toList());


        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "PENDING").collect(toList());

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getMaterialId(), is(materialId));
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getSjpCorrelationId(), is(sjpCorrelationId));
        assertThat(event.getSjpStatus(), is("PENDING"));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenMaterialSucceedsForNonSjpCase() {
        final UUID materialId = randomUUID();
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(2));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getMaterialStatus(), is("SUCCESS"));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenMaterialFailsForNonSjpCase() {
        final UUID materialId = randomUUID();
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "FAILED", null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobOnPendingRecord() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldIgnoreMaterialSuccessWhenDocumentDeliveryIsNotTracked() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenSjpFailsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING").toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "FAILED").toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenSjpSucceedsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING").toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS").toList();

        assertThat(eventStream.size(), is(2));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getSjpStatus(), is("SUCCESS"));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenMaterialSucceedsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING").toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldIgnorePendingWithoutBlobWhenDocumentDeliveryIsNotTracked() {
        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                randomUUID(), "PENDING", null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(0));
    }

    private void recordPendingWithBlob(final UUID materialId) {
        aggregate.recordDocumentDelivery(
                materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null, null);
    }

    private static void assertDocumentDeletedFromBlob(final Object event, final UUID materialId) {
        assertThat(event, instanceOf(DocumentDeletedFromBlob.class));
        final DocumentDeletedFromBlob documentDeletedFromBlob = (DocumentDeletedFromBlob) event;
        assertThat(documentDeletedFromBlob.getMaterialId(), is(materialId));
        assertThat(documentDeletedFromBlob.getPayloadBlobUri(), is(PAYLOAD_BLOB_URI));
        assertThat(documentDeletedFromBlob.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
    }

}
