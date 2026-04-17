package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.ACP;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.APPRO;

import java.util.List;

import org.junit.jupiter.api.Test;

class AggregateConstantsTest {

    @Test
    void testApplicationTypeACPValues() {
        assertEquals("4e281610-96aa-3711-aecf-59df86b6c6bb", ACP.id);
        assertEquals("Application within criminal proceedings", ACP.appType);
    }

    @Test
    void testApplicationTypeAPPROValues() {
        assertEquals("44c238d9-3bc2-3cf3-a2eb-a7d1437b8383", APPRO.id);
        assertEquals("Application to reopen case", APPRO.appType);
    }

    @Test
    void testApplicationTypeEnumValueOf() {
        AggregateConstants.ApplicationType acp = AggregateConstants.ApplicationType.valueOf("ACP");
        assertNotNull(acp);
        assertEquals("4e281610-96aa-3711-aecf-59df86b6c6bb", acp.id);

        AggregateConstants.ApplicationType appro = AggregateConstants.ApplicationType.valueOf("APPRO");
        assertNotNull(appro);
        assertEquals("44c238d9-3bc2-3cf3-a2eb-a7d1437b8383", appro.id);
    }

    @Test
    void testGetDistinctPromptReferences() {
        List<String> distinctPromptReferences = AggregateConstants.getDistinctPromptReferences();

        assertNotNull(distinctPromptReferences);
        assertEquals(5, distinctPromptReferences.size());
        assertTrue(distinctPromptReferences.contains(AggregateConstants.DEFENDANT_DRIVING_LICENCE_NUMBER));
        assertTrue(distinctPromptReferences.contains(AggregateConstants.LICENCE_PRODUCED_IN_COURT));
        assertTrue(distinctPromptReferences.contains(AggregateConstants.LICENCE_ISSUE_NUMBER));
        assertTrue(distinctPromptReferences.contains(AggregateConstants.DATE_OF_CONVICTION));
        assertTrue(distinctPromptReferences.contains(AggregateConstants.CONVICTING_COURT));
    }

    @Test
    void testResultTypeEnumValues() {
        assertEquals("177f29cd-49e9-41d1-aafb-5730d7414dd4", AggregateConstants.ResultType.AACA.id);
        assertEquals("548bf6b7-d152-4f08-8c9e-a079bf377e9b", AggregateConstants.ResultType.AACD.id);
        assertEquals("d861586a-df88-440d-98d4-63cc2a680ae1", AggregateConstants.ResultType.AASA.id);
    }
}
