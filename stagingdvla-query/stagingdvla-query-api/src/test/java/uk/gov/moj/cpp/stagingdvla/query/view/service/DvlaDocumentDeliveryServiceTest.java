package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DvlaDocumentDeliveryQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DvlaDocumentDeliveryResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DvlaDocumentDeliveryView;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.apache.deltaspike.data.api.QueryResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DvlaDocumentDeliveryServiceTest {

    @Mock
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    @Mock(answer = Answers.RETURNS_SELF)
    private QueryResult<DvlaDocumentDeliveryEntity> queryResult;

    @InjectMocks
    private DvlaDocumentDeliveryService service;

    @Test
    void shouldRejectRequestWithNoFilterAtAll() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(new DvlaDocumentDeliveryQueryParameters(null, null, null)));
    }

    @Test
    void shouldReturnDeliveryByMaterialIdWithoutSjpCaseFieldsWhenNoneExist() {
        final UUID materialId = randomUUID();
        final DvlaDocumentDeliveryEntity delivery = deliveryEntity(materialId, "SENT");

        when(dvlaDocumentDeliveryRepository.findOptionalBy(materialId)).thenReturn(Optional.of(delivery));

        final DvlaDocumentDeliveryResponse response = service.findDocumentDeliveries(
                new DvlaDocumentDeliveryQueryParameters(materialId.toString(), null, null));

        assertThat(response.getDocumentDeliveries(), hasSize(1));
        final DvlaDocumentDeliveryView view = response.getDocumentDeliveries().get(0);
        assertThat(view.getMaterialId(), equalTo(materialId));
        assertThat(view.getCaseId(), equalTo(null));
        assertThat(response.getTotalResults(), equalTo(1L));
    }

    @Test
    void shouldReturnDeliveryByMaterialIdWithItsSjpCaseFieldsWhenTheyExist() {
        final UUID materialId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID sjpCorrelationId = randomUUID();
        final DvlaDocumentDeliveryEntity delivery = deliveryEntityWithSjpCase(materialId, "SENT", caseId, sjpCorrelationId, "AWAITING_RESULT");

        when(dvlaDocumentDeliveryRepository.findOptionalBy(materialId)).thenReturn(Optional.of(delivery));

        final DvlaDocumentDeliveryResponse response = service.findDocumentDeliveries(
                new DvlaDocumentDeliveryQueryParameters(materialId.toString(), null, null));

        final DvlaDocumentDeliveryView view = response.getDocumentDeliveries().get(0);
        assertThat(view.getCaseId(), equalTo(caseId));
        assertThat(view.getSjpCorrelationId(), equalTo(sjpCorrelationId));
        assertThat(view.getSjpStatus(), equalTo("AWAITING_RESULT"));
    }

    @Test
    void shouldReturnEmptyWhenMaterialIdNotFound() {
        final UUID materialId = randomUUID();
        when(dvlaDocumentDeliveryRepository.findOptionalBy(materialId)).thenReturn(Optional.empty());

        final DvlaDocumentDeliveryResponse response = service.findDocumentDeliveries(
                new DvlaDocumentDeliveryQueryParameters(materialId.toString(), null, null));

        assertThat(response.getDocumentDeliveries(), empty());
        assertThat(response.getTotalResults(), equalTo(0L));
    }

    @Test
    void shouldApplyMaterialIdAndStatusAsAnAndFilter() {
        final UUID materialId = randomUUID();
        final DvlaDocumentDeliveryEntity delivery = deliveryEntity(materialId, "PENDING");

        when(dvlaDocumentDeliveryRepository.findOptionalBy(materialId)).thenReturn(Optional.of(delivery));

        final DvlaDocumentDeliveryResponse response = service.findDocumentDeliveries(
                new DvlaDocumentDeliveryQueryParameters(materialId.toString(), null, "SENT"));

        assertThat(response.getDocumentDeliveries(), empty());
    }

    @Test
    void shouldPageAndCountByMaterialStatusOnly() {
        final DvlaDocumentDeliveryEntity first = deliveryEntity(randomUUID(), "SENT");
        final DvlaDocumentDeliveryEntity second = deliveryEntity(randomUUID(), "SENT");

        when(dvlaDocumentDeliveryRepository.findByMaterialStatus("SENT")).thenReturn(queryResult);
        when(queryResult.getResultList()).thenReturn(List.of(first, second));
        when(queryResult.count()).thenReturn(2L);

        final DvlaDocumentDeliveryResponse response = service.findDocumentDeliveries(
                new DvlaDocumentDeliveryQueryParameters(null, null, "SENT"));

        assertThat(response.getDocumentDeliveries(), hasSize(2));
        assertThat(response.getDocumentDeliveries().stream().map(DvlaDocumentDeliveryView::getMaterialId).collect(toList()),
                containsInAnyOrder(first.getMaterialId(), second.getMaterialId()));
        assertThat(response.getTotalResults(), equalTo(2L));
        assertThat(response.getPageNumber(), equalTo(DvlaDocumentDeliveryService.DEFAULT_PAGE_NUMBER));
        assertThat(response.getPageSize(), equalTo(DvlaDocumentDeliveryService.DEFAULT_PAGE_SIZE));

        verify(queryResult).orderAsc("createdAt");
        verify(queryResult).firstResult(0);
        verify(queryResult).maxResults(DvlaDocumentDeliveryService.DEFAULT_PAGE_SIZE);
    }

    @Test
    void shouldApplyRequestedPageNumberAndPageSizeByCaseId() {
        final UUID caseId = randomUUID();

        when(dvlaDocumentDeliveryRepository.findByCaseId(caseId)).thenReturn(queryResult);
        when(queryResult.getResultList()).thenReturn(List.of());
        when(queryResult.count()).thenReturn(45L);

        final DvlaDocumentDeliveryResponse response = service.findDocumentDeliveries(
                queryParams(null, caseId.toString(), null, "3", "10"));

        assertThat(response.getPageNumber(), equalTo(3));
        assertThat(response.getPageSize(), equalTo(10));
        assertThat(response.getTotalResults(), equalTo(45L));

        verify(queryResult).firstResult(20);
        verify(queryResult).maxResults(10);
    }

    @Test
    void shouldFilterByCaseIdAndMaterialStatusTogether() {
        final UUID caseId = randomUUID();

        when(dvlaDocumentDeliveryRepository.findByCaseIdAndMaterialStatus(caseId, "SENT")).thenReturn(queryResult);
        when(queryResult.getResultList()).thenReturn(List.of());
        when(queryResult.count()).thenReturn(0L);

        service.findDocumentDeliveries(new DvlaDocumentDeliveryQueryParameters(null, caseId.toString(), "SENT"));

        verify(dvlaDocumentDeliveryRepository).findByCaseIdAndMaterialStatus(caseId, "SENT");
    }

    @Test
    void shouldThrowBadRequestForInvalidMaterialId() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(new DvlaDocumentDeliveryQueryParameters("not-a-uuid", null, null)));
    }

    @Test
    void shouldThrowBadRequestForInvalidCaseId() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(new DvlaDocumentDeliveryQueryParameters(null, "not-a-uuid", null)));
    }

    @Test
    void shouldThrowBadRequestForNonNumericPageNumber() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(queryParams(null, randomUUID().toString(), null, "not-a-number", null)));
    }

    @Test
    void shouldThrowBadRequestForZeroPageNumber() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(queryParams(null, randomUUID().toString(), null, "0", null)));
    }

    @Test
    void shouldThrowBadRequestForPageSizeAboveTheMaximum() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(queryParams(null, randomUUID().toString(), null, null, "101")));
    }

    @Test
    void shouldThrowBadRequestForZeroOrNegativePageSize() {
        assertThrows(BadRequestException.class, () ->
                service.findDocumentDeliveries(queryParams(null, randomUUID().toString(), null, null, "0")));
    }

    private DvlaDocumentDeliveryQueryParameters queryParams(final String materialId, final String caseId,
                                                                                            final String materialStatus, final String pageNumber,
                                                                                            final String pageSize) {
        return new DvlaDocumentDeliveryQueryParameters.Builder()
                .materialId(materialId)
                .caseId(caseId)
                .materialStatus(materialStatus)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .build();
    }

    private DvlaDocumentDeliveryEntity deliveryEntity(final UUID materialId, final String materialStatus) {
        final ZonedDateTime createdAt = now();
        return new DvlaDocumentDeliveryEntity(materialId, createdAt, materialStatus, "DELIVERED", "payload/uri", "document/uri");
    }

    private DvlaDocumentDeliveryEntity deliveryEntityWithSjpCase(final UUID materialId, final String materialStatus,
                                                                  final UUID caseId, final UUID sjpCorrelationId, final String sjpStatus) {
        final ZonedDateTime createdAt = now();
        return new DvlaDocumentDeliveryEntity(materialId, createdAt, materialStatus, "DELIVERED", "payload/uri", "document/uri",
                caseId, sjpCorrelationId, sjpStatus);
    }
}
