package uk.gov.moj.cpp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DvlaDocumentDeliveryRepositoryTest {

    @Inject
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Test
    public void shouldSaveAndRetrieveByMaterialId() {

        final UUID materialId = randomUUID();
        final ZonedDateTime createdAt = now();

        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        final DvlaDocumentDeliveryEntity entity = new DvlaDocumentDeliveryEntity(materialId, createdAt, "PENDING", "payload/blob/uri", "document/blob/uri",
                caseId, sjpCorrelationId, "AWAITING_RESULT");

        dvlaDocumentDeliveryRepository.save(entity);

        final DvlaDocumentDeliveryEntity retrieved = dvlaDocumentDeliveryRepository.findBy(materialId);

        assertThat(retrieved.getMaterialId(), equalTo(materialId));
        assertThat(retrieved.getCreatedAt(), equalTo(createdAt));
        assertThat(retrieved.getMaterialStatus(), equalTo("PENDING"));
        assertThat(retrieved.getPayloadBlobUri(), equalTo("payload/blob/uri"));
        assertThat(retrieved.getDocumentBlobUri(), equalTo("document/blob/uri"));
        assertThat(retrieved.getCaseId(), equalTo(caseId));
        assertThat(retrieved.getSjpCorrelationId(), equalTo(sjpCorrelationId));
        assertThat(retrieved.getSjpStatus(), equalTo("AWAITING_RESULT"));
    }

    @Test
    public void shouldRetrieveByCaseId() {

        final UUID caseId = randomUUID();
        final DvlaDocumentDeliveryEntity withCase = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "PENDING", null, null,
                caseId, randomUUID(), "PENDING");
        final DvlaDocumentDeliveryEntity withoutCase = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "PENDING", null, null);

        dvlaDocumentDeliveryRepository.save(withCase);
        dvlaDocumentDeliveryRepository.save(withoutCase);

        final List<DvlaDocumentDeliveryEntity> results = dvlaDocumentDeliveryRepository.findByCaseId(caseId).getResultList();

        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0).getMaterialId(), equalTo(withCase.getMaterialId()));
    }

    @Test
    public void shouldRetrieveByCaseIdAndMaterialStatus() {

        final UUID caseId = randomUUID();
        final DvlaDocumentDeliveryEntity pendingWithCase = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "PENDING", null, null,
                caseId, randomUUID(), "PENDING");
        final DvlaDocumentDeliveryEntity completedWithCase = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "COMPLETED", null, null,
                caseId, randomUUID(), "PENDING");

        dvlaDocumentDeliveryRepository.save(pendingWithCase);
        dvlaDocumentDeliveryRepository.save(completedWithCase);

        final List<DvlaDocumentDeliveryEntity> results = dvlaDocumentDeliveryRepository.findByCaseIdAndMaterialStatus(caseId, "PENDING").getResultList();

        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0).getMaterialId(), equalTo(pendingWithCase.getMaterialId()));
    }

    @Test
    public void shouldRetrieveByDocumentBlobUri() {

        final String documentBlobUri = "https://filestore/stagingdvla/generated/" + randomUUID() + ".pdf";
        final DvlaDocumentDeliveryEntity matching = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "SUCCESS", "payload/blob/uri", documentBlobUri,
                randomUUID(), randomUUID(), "PENDING");
        final DvlaDocumentDeliveryEntity otherDocument = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "SUCCESS", "payload/blob/uri", "other/document/blob/uri",
                randomUUID(), randomUUID(), "PENDING");

        dvlaDocumentDeliveryRepository.save(matching);
        dvlaDocumentDeliveryRepository.save(otherDocument);

        final List<DvlaDocumentDeliveryEntity> results = dvlaDocumentDeliveryRepository.findByDocumentBlobUri(documentBlobUri);

        assertThat(results.size(), equalTo(1));
        assertThat(results.get(0).getMaterialId(), equalTo(matching.getMaterialId()));
    }

    @Test
    public void shouldPageAndCountByCaseId() {

        final UUID caseId = randomUUID();
        for (int i = 0; i < 3; i++) {
            dvlaDocumentDeliveryRepository.save(new DvlaDocumentDeliveryEntity(randomUUID(), now(), "PENDING", null, null,
                    caseId, randomUUID(), "PENDING"));
        }

        final long totalCount = dvlaDocumentDeliveryRepository.findByCaseId(caseId).count();
        final List<DvlaDocumentDeliveryEntity> firstPage = dvlaDocumentDeliveryRepository.findByCaseId(caseId)
                .orderAsc("createdAt").firstResult(0).maxResults(2).getResultList();
        final List<DvlaDocumentDeliveryEntity> secondPage = dvlaDocumentDeliveryRepository.findByCaseId(caseId)
                .orderAsc("createdAt").firstResult(2).maxResults(2).getResultList();

        assertThat(totalCount, equalTo(3L));
        assertThat(firstPage.size(), equalTo(2));
        assertThat(secondPage.size(), equalTo(1));
    }

    @Test
    public void shouldSaveWithNullableFieldsUnset() {

        final UUID materialId = randomUUID();
        final ZonedDateTime createdAt = now();

        final DvlaDocumentDeliveryEntity entity = new DvlaDocumentDeliveryEntity(materialId, createdAt, null, null, null);

        dvlaDocumentDeliveryRepository.save(entity);

        final DvlaDocumentDeliveryEntity retrieved = dvlaDocumentDeliveryRepository.findBy(materialId);

        assertThat(retrieved.getMaterialId(), equalTo(materialId));
        assertThat(retrieved.getMaterialStatus(), nullValue());
        assertThat(retrieved.getPayloadBlobUri(), nullValue());
        assertThat(retrieved.getDocumentBlobUri(), nullValue());
    }

    @Test
    public void shouldRetrieveAllByMaterialStatus() {

        final DvlaDocumentDeliveryEntity sent = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "SENT", null, null);
        final DvlaDocumentDeliveryEntity sentToo = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "SENT", null, null);
        final DvlaDocumentDeliveryEntity pending = new DvlaDocumentDeliveryEntity(randomUUID(), now(), "PENDING", null, null);

        dvlaDocumentDeliveryRepository.save(sent);
        dvlaDocumentDeliveryRepository.save(sentToo);
        dvlaDocumentDeliveryRepository.save(pending);

        final List<DvlaDocumentDeliveryEntity> results = dvlaDocumentDeliveryRepository.findByMaterialStatus("SENT").getResultList();

        assertThat(results.size(), equalTo(2));
        assertThat(results.stream().map(DvlaDocumentDeliveryEntity::getMaterialId).collect(toList()),
                containsInAnyOrder(sent.getMaterialId(), sentToo.getMaterialId()));
        results.forEach(result -> assertThat(result.getMaterialStatus(), equalTo("SENT")));
    }
}
