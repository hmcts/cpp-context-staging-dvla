package uk.gov.moj.cpp.stagingdvla.query.view.request;

public class DvlaDocumentDeliveryQueryParameters {

    private String materialId;
    private String caseId;
    private String materialStatus;
    private String pageNumber;
    private String pageSize;

    public DvlaDocumentDeliveryQueryParameters() {
    }

    public DvlaDocumentDeliveryQueryParameters(final String materialId, final String caseId, final String materialStatus) {
        this(materialId, caseId, materialStatus, null, null);
    }

    public DvlaDocumentDeliveryQueryParameters(final String materialId, final String caseId, final String materialStatus,
                                                final String pageNumber, final String pageSize) {
        this.materialId = materialId;
        this.caseId = caseId;
        this.materialStatus = materialStatus;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
    }

    public String getMaterialId() {
        return materialId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getMaterialStatus() {
        return materialStatus;
    }

    public String getPageNumber() {
        return pageNumber;
    }

    public String getPageSize() {
        return pageSize;
    }

    public static class Builder {
        private String materialId;
        private String caseId;
        private String materialStatus;
        private String pageNumber;
        private String pageSize;

        public Builder materialId(final String materialId) {
            this.materialId = materialId;
            return this;
        }

        public Builder caseId(final String caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder materialStatus(final String materialStatus) {
            this.materialStatus = materialStatus;
            return this;
        }

        public Builder pageNumber(final String pageNumber) {
            this.pageNumber = pageNumber;
            return this;
        }

        public Builder pageSize(final String pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public DvlaDocumentDeliveryQueryParameters build() {
            return new DvlaDocumentDeliveryQueryParameters(materialId, caseId, materialStatus, pageNumber, pageSize);
        }
    }

    @Override
    public String toString() {
        return "DvlaDocumentDeliveryQueryParameters{" +
                "materialId='" + materialId + '\'' +
                ", caseId='" + caseId + '\'' +
                ", materialStatus='" + materialStatus + '\'' +
                ", pageNumber='" + pageNumber + '\'' +
                ", pageSize='" + pageSize + '\'' +
                '}';
    }
}
