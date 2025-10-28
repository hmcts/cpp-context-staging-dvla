package uk.gov.moj.cpp.stagingdvla.helper;

public class DriverSearchAuditHelper {
    public static final String FILE_NAME = "fileName";
    public static final String MATERIAL_ID = "materialId";
    public static final String CONVERSION_FORMAT = "conversionFormat";
    public static final String ORIGINATING_SOURCE = "originatingSource";
    public static final String TEMPLATE_NAME = "templateName";
    public static final String TEMPLATE_IDENTIFIER = "templateIdentifier";
    public static final String NUMBER_OF_PAGES = "numberOfPages";
    public static final String FILE_SIZE = "fileSize";
    public static final String SOURCE_CORRELATION_ID = "sourceCorrelationId";
    public static final String PAYLOAD_FILE_SERVICE_ID = "payloadFileServiceId";
    public static final String CSV = "csv";
    public static final String DVLA_AUDIT_RECORDS = "DvlaAuditRecords";

    private DriverSearchAuditHelper() {
    }

    public static boolean isForDriverAuditReportDocument(final String originatingSource) {
        return DVLA_AUDIT_RECORDS.equalsIgnoreCase(originatingSource);
    }
}
