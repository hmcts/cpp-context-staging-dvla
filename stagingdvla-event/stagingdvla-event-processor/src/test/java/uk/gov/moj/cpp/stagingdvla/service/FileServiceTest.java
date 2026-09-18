package uk.gov.moj.cpp.stagingdvla.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FileServiceTest {

    @Mock
    private FileStorer fileStorer;

    private final FileService fileService = new FileService();

    @BeforeEach
    public void setUp() {
        setField(fileService, "fileStorer", fileStorer);
    }

    @Test
    public void shouldStorePayloadAndReturnTheFileServiceIdFromTheFileStorer() throws FileServiceException {
        final UUID expectedFileServiceId = randomUUID();
        final JsonObject payload = createObjectBuilder().add("driverAuditRecords", createObjectBuilder().build()).build();

        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenReturn(expectedFileServiceId);

        final UUID fileServiceId = fileService.storePayload(payload, "DriverAuditReport_20260916.csv", "DvlaAuditRecords", ConversionFormat.CSV);

        assertThat(fileServiceId, is(expectedFileServiceId));
    }

    @Test
    public void shouldBuildMetadataWithFileNameConversionFormatTemplateNameAndFileSize() throws FileServiceException {
        final JsonObject payload = createObjectBuilder().add("key", "value").build();
        final int expectedFileSize = payload.toString().getBytes(StandardCharsets.UTF_8).length;

        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenReturn(randomUUID());

        fileService.storePayload(payload, "DriverAuditReport_20260916.csv", "DvlaAuditRecords", ConversionFormat.CSV);

        final ArgumentCaptor<JsonObject> metadataCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(fileStorer).store(metadataCaptor.capture(), any(ByteArrayInputStream.class));

        final JsonObject metadata = metadataCaptor.getValue();
        assertThat(metadata.getString("fileName"), is("DriverAuditReport_20260916.csv"));
        assertThat(metadata.getString("conversionFormat"), is(ConversionFormat.CSV.toString()));
        assertThat(metadata.getString("templateName"), is("DvlaAuditRecords"));
        assertThat(metadata.getInt("numberOfPages"), is(1));
        assertThat(metadata.getInt("fileSize"), is(expectedFileSize));
    }

    @Test
    public void shouldUsePdfConversionFormatWhenRequested() throws FileServiceException {
        final JsonObject payload = createObjectBuilder().build();

        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenReturn(randomUUID());

        fileService.storePayload(payload, "DVLADocumentOrder_20260916.pdf", "EDT_DriverOutNotification", ConversionFormat.PDF);

        final ArgumentCaptor<JsonObject> metadataCaptor = ArgumentCaptor.forClass(JsonObject.class);
        verify(fileStorer).store(metadataCaptor.capture(), any(ByteArrayInputStream.class));

        assertThat(metadataCaptor.getValue().getString("conversionFormat"), is(ConversionFormat.PDF.toString()));
    }

    @Test
    public void shouldWrapFileServiceExceptionInARuntimeExceptionWithTheSameMessage() throws FileServiceException {
        final JsonObject payload = createObjectBuilder().build();
        final FileServiceException cause = new FileServiceException("file service unavailable");

        when(fileStorer.store(any(JsonObject.class), any(ByteArrayInputStream.class))).thenThrow(cause);

        final RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileService.storePayload(payload, "fileName.csv", "templateName", ConversionFormat.CSV));

        assertThat(exception.getMessage(), is(cause.getMessage()));
    }
}
