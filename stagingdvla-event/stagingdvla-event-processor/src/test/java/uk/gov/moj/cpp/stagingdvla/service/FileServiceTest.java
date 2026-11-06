package uk.gov.moj.cpp.stagingdvla.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.fileservice.common.file.ContentTypeDetector;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    @Mock
    private FileStorer fileStorer;

    private final JsonObject payload = Json.createObjectBuilder().add("key", "value").build();

    @Test
    void shouldStoreThePayloadAndReturnTheFileServiceId() throws Exception {
        final UUID fileId = UUID.randomUUID();
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class))).thenReturn(fileId);

        final UUID result = fileService.storePayload(payload, "notification.pdf", "templateName");

        assertThat(result, is(fileId));
    }

    @Test
    void shouldWrapAFileServiceExceptionInARuntimeException() throws Exception {
        when(fileStorer.store(any(JsonObject.class), any(InputStream.class)))
                .thenThrow(new FileServiceException("store failed"));

        assertThrows(RuntimeException.class,
                () -> fileService.storePayload(payload, "notification.pdf", "templateName"));
    }

    /*
     * CHD-07 (BC-22) - Java 17 behaviour (inferred from Tika 1.28's detection rules): the framework file store
     * (MetadataUpdater -> ContentTypeDetector -> Tika) records the mediaType of what FileService stores, which is
     * the document's JSON payload. With no filename and no JSON magic, Tika detects it as text/plain.
     */
    @Test
    void chd07_shouldStoreAPayloadThatTheFileStoreDetectsAsTextPlain() throws Exception {
        final JsonObject documentPayload = Json.createObjectBuilder()
                .add("nowContent", Json.createObjectBuilder().add("orderName", "Driving disqualification").add("caseUrn", "TFL12345"))
                .add("orderingCourt", Json.createObjectBuilder().add("name", "Liverpool Crown Court"))
                .build();
        final ArgumentCaptor<InputStream> stored = ArgumentCaptor.forClass(InputStream.class);
        when(fileStorer.store(any(JsonObject.class), stored.capture())).thenReturn(UUID.randomUUID());

        fileService.storePayload(documentPayload, "notification.pdf", "templateName");

        verify(fileStorer).store(any(JsonObject.class), any(InputStream.class));
        final String detected = new ContentTypeDetector().detectContentTypeOf(new BufferedInputStream(stored.getValue())).toString();
        assertThat(detected, is("text/plain"));
    }
}
