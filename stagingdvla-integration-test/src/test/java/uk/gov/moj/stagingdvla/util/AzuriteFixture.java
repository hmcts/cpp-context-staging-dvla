package uk.gov.moj.stagingdvla.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.google.common.io.Resources;

/**
 * One source of truth for the Azurite (Azure Storage emulator) connection string used by the
 * IT suite's Azure blob assertions (DvlaNotificationIT, QueryDrivingLicencesIT).
 *
 * <p>The raw value lives in {@code azurite.properties} on the test classpath rather than as a
 * Java string literal, so a secret-scanner reviewing a .java diff never sees it inline. The
 * {@code # gitleaks:allow} marker on that property's own line tells this repo's gitleaks-based
 * scanner ({@code .github/workflows/secret-scanning.yml}) it is the publicly documented Azurite
 * emulator key, not a live credential - the same pattern used by
 * {@code cpp-context-listing-courtscheduler}'s {@code docker/.env} + {@code AzuriteFixture}.</p>
 */
public final class AzuriteFixture {

    private static final String PROPERTIES_FILE = "azurite.properties";
    private static final String CONNECTION_STRING_KEY = "AZURITE_CONNECTION_STRING";

    private static final String CONNECTION_STRING = load();

    private AzuriteFixture() {
    }

    public static String connectionString() {
        return CONNECTION_STRING;
    }

    private static String load() {
        try (InputStream in = Resources.getResource(PROPERTIES_FILE).openStream()) {
            final Properties properties = new Properties();
            properties.load(in);

            final String rawValue = properties.getProperty(CONNECTION_STRING_KEY);
            if (rawValue == null || rawValue.isBlank()) {
                throw new IllegalStateException(CONNECTION_STRING_KEY + " missing from " + PROPERTIES_FILE);
            }

            // java.util.Properties only treats '#'/'!' as a comment when it is the first
            // non-whitespace character of a line, so the trailing " # gitleaks:allow" marker
            // survives into the loaded value and has to be stripped manually here.
            final int commentStart = rawValue.indexOf(" #");
            return commentStart >= 0 ? rawValue.substring(0, commentStart).stripTrailing() : rawValue;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + PROPERTIES_FILE + " from the test classpath", e);
        }
    }
}
