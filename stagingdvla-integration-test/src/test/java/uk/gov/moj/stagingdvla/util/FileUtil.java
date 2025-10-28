package uk.gov.moj.stagingdvla.util;

import static org.junit.Assert.fail;

import java.nio.charset.Charset;

import com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);

    public static String getPayload(String path) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(path),
                    Charset.defaultCharset()
            );
        } catch (Exception e) {
            LOGGER.error(String.format("Error consuming file from location {}", path), e);
            fail("Error consuming file from location " + path);
        }
        return request;
    }

}
