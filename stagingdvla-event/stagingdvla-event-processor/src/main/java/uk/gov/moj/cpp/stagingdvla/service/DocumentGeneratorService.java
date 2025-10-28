package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.Material;
import uk.gov.justice.core.courts.notification.EmailChannel;

import uk.gov.justice.cpp.stagingdvla.CaseDocument;
import uk.gov.justice.cpp.stagingdvla.DocumentCategory;
import uk.gov.justice.cpp.stagingdvla.event.Cases;
import uk.gov.justice.cpp.stagingdvla.event.DriverNotified;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.stagingdvla.SjpDocumentTypes;
import uk.gov.moj.cpp.stagingdvla.service.exception.FileUploadException;
import uk.gov.moj.cpp.stagingdvla.service.exception.NowsTemplateNameNotFoundException;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
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