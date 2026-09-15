package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import uk.gov.moj.cpp.persistence.entity.DvlaDocumentDeliveryEntity;
import uk.gov.moj.cpp.persistence.repository.DvlaDocumentDeliveryRepository;
import uk.gov.moj.cpp.stagingdvla.query.view.request.DvlaDocumentDeliveryQueryParameters;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DvlaDocumentDeliveryResponse;
import uk.gov.moj.cpp.stagingdvla.query.view.response.DvlaDocumentDeliveryView;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.apache.deltaspike.data.api.QueryResult;

/**
 * Queries {@code dvla_document_delivery}. At least one of materialId/caseId/materialStatus must
 * be supplied - there is no "return every record" mode. materialId is the table's primary key so
 * it can only ever match zero or one row; caseId and materialStatus can each match many rows, so
 * those two are paginated with pageNumber/pageSize.
 */
@ApplicationScoped
public class DvlaDocumentDeliveryService {

    static final int DEFAULT_PAGE_NUMBER = 1;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    @Inject
    private DvlaDocumentDeliveryRepository dvlaDocumentDeliveryRepository;

    public DvlaDocumentDeliveryResponse findDocumentDeliveries(final DvlaDocumentDeliveryQueryParameters queryParameters) {

        final UUID materialId = parseUuid("materialId", queryParameters.getMaterialId());
        final UUID caseId = parseUuid("caseId", queryParameters.getCaseId());
        final String materialStatus = isNotBlank(queryParameters.getMaterialStatus()) ? queryParameters.getMaterialStatus() : null;

        if (materialId == null && caseId == null && materialStatus == null) {
            throw new BadRequestException("At least one of materialId, caseId or materialStatus must be provided");
        }

        final int pageNumber = parsePositiveInt("pageNumber", queryParameters.getPageNumber(), DEFAULT_PAGE_NUMBER, Integer.MAX_VALUE);
        final int pageSize = parsePositiveInt("pageSize", queryParameters.getPageSize(), DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);

        return materialId != null
                ? findByMaterialId(materialId, caseId, materialStatus, pageNumber, pageSize)
                : findPaged(caseId, materialStatus, pageNumber, pageSize);
    }

    private DvlaDocumentDeliveryResponse findByMaterialId(final UUID materialId, final UUID caseId, final String materialStatus,
                                                           final int pageNumber, final int pageSize) {
        final List<DvlaDocumentDeliveryView> views = dvlaDocumentDeliveryRepository.findOptionalBy(materialId)
                .filter(delivery -> caseId == null || caseId.equals(delivery.getCaseId()))
                .filter(delivery -> materialStatus == null || materialStatus.equals(delivery.getMaterialStatus()))
                .map(delivery -> singletonList(toView(delivery)))
                .orElse(emptyList());

        return new DvlaDocumentDeliveryResponse(views, pageNumber, pageSize, views.size());
    }

    private DvlaDocumentDeliveryResponse findPaged(final UUID caseId, final String materialStatus,
                                                    final int pageNumber, final int pageSize) {
        final int firstResult = (pageNumber - 1) * pageSize;

        final QueryResult<DvlaDocumentDeliveryEntity> queryResult = queryFor(caseId, materialStatus)
                .orderAsc("createdAt")
                .firstResult(firstResult)
                .maxResults(pageSize);

        final List<DvlaDocumentDeliveryView> views = queryResult.getResultList().stream()
                .map(this::toView)
                .collect(toList());

        return new DvlaDocumentDeliveryResponse(views, pageNumber, pageSize, queryResult.count());
    }

    private QueryResult<DvlaDocumentDeliveryEntity> queryFor(final UUID caseId, final String materialStatus) {
        if (caseId != null && materialStatus != null) {
            return dvlaDocumentDeliveryRepository.findByCaseIdAndMaterialStatus(caseId, materialStatus);
        }
        if (caseId != null) {
            return dvlaDocumentDeliveryRepository.findByCaseId(caseId);
        }
        return dvlaDocumentDeliveryRepository.findByMaterialStatus(materialStatus);
    }

    private DvlaDocumentDeliveryView toView(final DvlaDocumentDeliveryEntity delivery) {
        return new DvlaDocumentDeliveryView(delivery.getMaterialId(), delivery.getCreatedAt(), delivery.getMaterialStatus(),
                delivery.getPayloadBlobUri(), delivery.getDocumentBlobUri(),
                delivery.getCaseId(), delivery.getSjpCorrelationId(), delivery.getSjpStatus());
    }

    private UUID parseUuid(final String paramName, final String value) {
        if (isNotBlank(value)) {
            try {
                return fromString(value);
            } catch (final IllegalArgumentException e) {
                throw new BadRequestException(paramName + " is invalid");
            }
        }
        return null;
    }

    private int parsePositiveInt(final String paramName, final String value, final int defaultValue, final int maxValue) {
        if (!isNotBlank(value)) {
            return defaultValue;
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            throw new BadRequestException(paramName + " is invalid");
        }
        if (parsed < 1 || parsed > maxValue) {
            throw new BadRequestException(paramName + " must be between 1 and " + maxValue);
        }
        return parsed;
    }
}
