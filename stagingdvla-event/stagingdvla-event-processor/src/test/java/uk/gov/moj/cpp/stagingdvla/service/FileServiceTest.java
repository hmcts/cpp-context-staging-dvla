package uk.gov.moj.cpp.stagingdvla.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.InputStream;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
