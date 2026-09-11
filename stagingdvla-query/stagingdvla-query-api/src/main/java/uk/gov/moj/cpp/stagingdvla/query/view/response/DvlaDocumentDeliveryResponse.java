package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.util.List;

public class DvlaDocumentDeliveryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<DvlaDocumentDeliveryView> documentDeliveries;
    private int pageNumber;
    private int pageSize;
    private long totalResults;

    public DvlaDocumentDeliveryResponse() {
    }

    public DvlaDocumentDeliveryResponse(final List<DvlaDocumentDeliveryView> documentDeliveries, final int pageNumber,
                                         final int pageSize, final long totalResults) {
        this.documentDeliveries = documentDeliveries;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalResults = totalResults;
    }

    public List<DvlaDocumentDeliveryView> getDocumentDeliveries() {
        return documentDeliveries;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalResults() {
        return totalResults;
    }
}
