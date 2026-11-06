package uk.gov.moj.cpp.stagingdvla.material.client;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Staging-DVLA-owned copy of material's URL-building helper (decouples staging-dvla from the
 * material-client JAR - PEG-3448). Builds material download URL strings; no HTTP call is made here.
 */
@ApplicationScoped
public class MaterialUrlGenerator {

    private static final String BASE_URI = "http://localhost:8080/material-query-api/query/api/rest/material";
    private static final String MATERIAL_REQUEST_PATH = "/material/";
    private static final String MATERIAL_STREAM_PDF_PARAMETERS = "?stream=true&requestPdf=true";

    public String pdfFileStreamUrlFor(final UUID materialId) {
        return BASE_URI + MATERIAL_REQUEST_PATH + materialId + MATERIAL_STREAM_PDF_PARAMETERS;
    }

}
