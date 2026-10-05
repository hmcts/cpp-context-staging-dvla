package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.notification.EmailChannel;
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
                materialId, "PENDING", "payload/blob/uri", "document/blob/uri", null, null, null, null).collect(toList());

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
                materialId, "PENDING", "payload/blob/uri", "document/blob/uri", null, null, null, null).collect(toList());


        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).collect(toList());

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getMaterialId(), is(materialId));
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getSjpCorrelationId(), is(sjpCorrelationId));
        assertThat(event.getSjpStatus(), is("PENDING"));
    }

    @Test
    public void shouldRecordEmailStatusPendingWhenMaterialSucceedsAndEmailNotificationIsRequired() {
        final UUID materialId = randomUUID();
        aggregate.create(MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .withEmailNotifications(List.of(EmailChannel.emailChannel().withSendToAddress("dvla-test@example.com").build()))
                .build());
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getMaterialStatus(), is("SUCCESS"));
        assertThat(event.getEmailStatus(), is("PENDING"));
    }

    @Test
    public void shouldRecordEmailStatusNotRequiredWhenMaterialSucceedsWithoutEmailNotification() {
        final UUID materialId = randomUUID();
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getEmailStatus(), is("NOT_REQUIRED"));
    }

    @Test
    public void shouldRecordEmailStatusNotRequiredWhenMaterialDetailsHaveNoEmailNotifications() {
        final UUID materialId = randomUUID();
        aggregate.create(MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .build());
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getEmailStatus(), is("NOT_REQUIRED"));
    }

    @Test
    public void shouldNotRecordEmailStatusWhenMaterialFails() {
        final UUID materialId = randomUUID();
        aggregate.create(MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .withEmailNotifications(List.of(EmailChannel.emailChannel().withSendToAddress("dvla-test@example.com").build()))
                .build());
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "FAILED", null, null, null, null, null, null).toList();

        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getMaterialStatus(), is("FAILED"));
        assertThat(event.getEmailStatus(), is(nullValue()));
    }

    @Test
    public void shouldNotRecordEmailStatusOnSjpStatusRecord() {
        final UUID materialId = randomUUID();
        aggregate.create(MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .withEmailNotifications(List.of(EmailChannel.emailChannel().withSendToAddress("dvla-test@example.com").build()))
                .build());
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, randomUUID(), randomUUID(), "SUCCESS", null).toList();

        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getSjpStatus(), is("SUCCESS"));
        assertThat(event.getEmailStatus(), is(nullValue()));
    }

    @Test
    public void shouldNotRecordEmailStatusWhenMaterialHasNotSucceeded() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null, null, null).toList();

        final DvlaDocumentDeliveryRecorded event = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(event.getEmailStatus(), is(nullValue()));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenMaterialSucceedsForNonSjpCase() {
        final UUID materialId = randomUUID();
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

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
                materialId, "FAILED", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobOnPendingRecord() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldIgnoreMaterialSuccessWhenDocumentDeliveryIsNotTracked() {
        final UUID materialId = randomUUID();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenSjpFailsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "FAILED", null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenSjpSucceedsBeforeMaterialSucceedsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenSjpSucceedsAfterMaterialSucceedsAndEmailNotRequiredForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        assertThat(eventStream.size(), is(2));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getSjpStatus(), is("SUCCESS"));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenMaterialSucceedsAfterSjpSucceedsAndEmailNotRequiredForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(2));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getEmailStatus(), is("NOT_REQUIRED"));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhileEmailIsPendingForNonSjpCase() {
        final UUID materialId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getEmailStatus(), is("PENDING"));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhileEmailIsPendingForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenEmailSucceedsForNonSjpCase() {
        final UUID materialId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, null, null, null, "SUCCESS").toList();

        assertThat(eventStream.size(), is(2));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getEmailStatus(), is("SUCCESS"));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenEmailFailsForNonSjpCase() {
        final UUID materialId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, null, null, null, "FAILED").toList();

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getEmailStatus(), is("FAILED"));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenSjpSucceedsAfterEmailSucceedsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();
        aggregate.recordDocumentDelivery(materialId, null, null, null, null, null, null, "SUCCESS").toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        assertThat(eventStream.size(), is(2));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobWhenEmailSucceedsAfterSjpSucceedsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, null, null, null, "SUCCESS").toList();

        assertThat(eventStream.size(), is(2));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenEmailSucceedsButSjpIsPendingForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, null, null, null, "SUCCESS").toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldKeepEmailSuccessAndRaiseDocumentDeletedFromBlobWhenMaterialSuccessArrivesAfterEmailForNonSjpCase() {
        final UUID materialId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, null, null, null, "SUCCESS").toList();

        // the material SUCCESS record was delayed past the email round trip
        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(2));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getMaterialStatus(), is("SUCCESS"));
        assertThat(recorded.getEmailStatus(), is(nullValue()));
        assertDocumentDeletedFromBlob(eventStream.get(1), materialId);
    }

    @Test
    public void shouldKeepEmailFailedAndNotRaiseDocumentDeletedFromBlobWhenMaterialSuccessArrivesAfterEmailForNonSjpCase() {
        final UUID materialId = randomUUID();
        createWithEmailNotification(materialId);
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, null, null, null, "FAILED").toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getMaterialStatus(), is("SUCCESS"));
        assertThat(recorded.getEmailStatus(), is(nullValue()));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenMaterialSucceedsBeforeSjpPendingForSjpCaseKnownFromBlobPendingRecord() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        // the blob PENDING record already carries the SJP case
        aggregate.recordDocumentDelivery(materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, caseId, null, null, null).toList();

        // material SUCCESS overtakes the sjp PENDING record
        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getEmailStatus(), is("NOT_REQUIRED"));
    }

    @Test
    public void shouldNotRecordLateSjpPendingAfterSjpSucceeded() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();

        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getSjpStatus(), is(nullValue()));
        assertThat(recorded.getCaseId(), is(caseId));
        assertThat(recorded.getSjpCorrelationId(), is(sjpCorrelationId));
    }

    @Test
    public void shouldNotRecordLateSjpPendingAfterSjpFailed() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "FAILED", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();

        final DvlaDocumentDeliveryRecorded recorded = (DvlaDocumentDeliveryRecorded) eventStream.get(0);
        assertThat(recorded.getSjpStatus(), is(nullValue()));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobAgainOnLateSjpPendingAfterDeliveryCompleted() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        aggregate.recordDocumentDelivery(materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, caseId, null, null, null).toList();
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "SUCCESS", null).toList();
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        // delivery already completed and the blob was deleted with the material SUCCESS record
        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(((DvlaDocumentDeliveryRecorded) eventStream.get(0)).getSjpStatus(), is(nullValue()));
    }

    @Test
    public void shouldRaiseDocumentDeletedFromBlobOnlyOnce() {
        final UUID materialId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, "SUCCESS", null, null, null, null, null, null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldNotRaiseDocumentDeletedFromBlobWhenMaterialSucceedsForSjpCase() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        recordPendingWithBlob(materialId);
        aggregate.recordDocumentDelivery(materialId, null, null, null, caseId, sjpCorrelationId, "PENDING", null).toList();

        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                materialId, "SUCCESS", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0), instanceOf(DvlaDocumentDeliveryRecorded.class));
    }

    @Test
    public void shouldIgnorePendingWithoutBlobWhenDocumentDeliveryIsNotTracked() {
        final List<Object> eventStream = aggregate.recordDocumentDelivery(
                randomUUID(), "PENDING", null, null, null, null, null, null).toList();

        assertThat(eventStream.size(), is(0));
    }

    private void createWithEmailNotification(final UUID materialId) {
        aggregate.create(MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .withEmailNotifications(List.of(EmailChannel.emailChannel().withSendToAddress("dvla-test@example.com").build()))
                .build());
    }

    private void recordPendingWithBlob(final UUID materialId) {
        aggregate.recordDocumentDelivery(
                materialId, "PENDING", PAYLOAD_BLOB_URI, DOCUMENT_BLOB_URI, null, null, null, null);
    }

    private static void assertDocumentDeletedFromBlob(final Object event, final UUID materialId) {
        assertThat(event, instanceOf(DocumentDeletedFromBlob.class));
        final DocumentDeletedFromBlob documentDeletedFromBlob = (DocumentDeletedFromBlob) event;
        assertThat(documentDeletedFromBlob.getMaterialId(), is(materialId));
        assertThat(documentDeletedFromBlob.getPayloadBlobUri(), is(PAYLOAD_BLOB_URI));
        assertThat(documentDeletedFromBlob.getDocumentBlobUri(), is(DOCUMENT_BLOB_URI));
    }

}
