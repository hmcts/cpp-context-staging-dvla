package uk.gov.moj.cpp.stagingdvla.service;

import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentGeneratorService {

    public static final String DVLA_DOCUMENT_TEMPLATE_NAME = "EDT_DriverOutNotification";
    public static final String DVLA_DOCUMENT_ORDER = "DVLADocumentOrder";
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ERROR_MESSAGE = "Error while uploading document generation or upload ";


    private final SystemDocGeneratorService systemDocGeneratorService;

    private final ObjectToJsonObjectConverter objectToJsonObjectConverter;

    private final FileService fileService;

    @Inject
    public DocumentGeneratorService(
            final FileService fileService,
            final SystemDocGeneratorService systemDocGeneratorService,
            final ObjectToJsonObjectConverter objectToJsonObjectConverter
    ) {
        this.fileService = fileService;
        this.systemDocGeneratorService = systemDocGeneratorService;
        this.objectToJsonObjectConverter = objectToJsonObjectConverter;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void generateDvlaDocument(final JsonEnvelope originatingEnvelope, final UUID userId, final DriverNotified driverNotified) {
        try {

            final JsonObject nowsDocumentOrderJson = objectToJsonObjectConverter.convert(driverNotified);

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("generate D20 for DriverNotified event {}", driverNotified.getIdentifier());
            }

            final String fileName = getTimeStampAmendedFileName(DVLA_DOCUMENT_ORDER);

            final UUID fileId = fileService.storePayload(nowsDocumentOrderJson, fileName, DVLA_DOCUMENT_TEMPLATE_NAME);
            final DocumentGenerationRequest documentGenerationRequest = new DocumentGenerationRequest(
                    DVLA_DOCUMENT_ORDER,
                    DVLA_DOCUMENT_TEMPLATE_NAME,
                    ConversionFormat.PDF,
                    userId.toString(),
                    fileId);
            systemDocGeneratorService.generateDocument(documentGenerationRequest, originatingEnvelope);

        } catch (RuntimeException e) {
            LOGGER.error(ERROR_MESSAGE, e);
        }
    }

    private String getTimeStampAmendedFileName(final String fileName) {
        return String.format("%s_%s.pdf", fileName, ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
    }
}