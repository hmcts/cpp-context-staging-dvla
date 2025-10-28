package uk.gov.moj.cpp.stagingdvla.notify.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class FileUtils {

    public static String readFile(String fileName) throws URISyntaxException, IOException {
       return Files.lines(Paths.get(ClassLoader.getSystemClassLoader().getResource(fileName).toURI()))
                .parallel()
                .collect(Collectors.joining());
    }
}
