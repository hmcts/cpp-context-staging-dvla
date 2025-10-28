package uk.gov.moj.cpp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.moj.cpp.persistence.entity.CaseTextEntity;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseTextRepositoryTest {

    private static final UUID CASE_ID = randomUUID();

    @Inject
    private CaseTextRepository caseTextRepository;

    @Test
    public void shouldSaveAndReadACaseTextAndOrderByCreatedDateDesc() {

        CaseTextEntity firstText = new CaseTextEntity(CASE_ID, "A Text", now());
        CaseTextEntity secondText = new CaseTextEntity(CASE_ID, "A Second Text", now().plusMinutes(1));

        caseTextRepository.save(firstText);
        caseTextRepository.save(secondText);

        final List<CaseTextEntity> allCaseText = caseTextRepository.findByCaseIdOrderByCreatedDateTimeDesc(CASE_ID);
        assertThat(allCaseText.size(), equalTo(2));
        verifyCaseText(allCaseText.get(0), secondText);
        verifyCaseText(allCaseText.get(1), firstText);

    }

    private void verifyCaseText(final CaseTextEntity actual, final CaseTextEntity expected) {
        assertThat(actual.getCaseId(), equalTo(CASE_ID));
        assertThat(actual.getText(), equalTo(expected.getText()));
        assertThat(actual.getCreatedDateTime(), equalTo(expected.getCreatedDateTime()));
    }
}