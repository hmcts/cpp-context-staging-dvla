package uk.gov.moj.cpp.stagingdvla.query.api.accesscontrol;


import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

@Disabled
public class QueryStagingdvlaApiAccessControlTest extends BaseDroolsAccessControlTest {

    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public QueryStagingdvlaApiAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToQueryDriver() {
        action = createActionFor("stagingdvla.query.drivernumber");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQueryDriver() {
        action = createActionFor("stagingdvla.query.drivernumber");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Random group"))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedUserToQueryDrivers() {
        action = createActionFor("stagingdvla.query.driverdetails");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQueryDrivers() {
        action = createActionFor("stagingdvla.query.driverdetails");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, "Random group"))
                .willReturn(false);

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @AfterEach
    public void tearDown() {
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
    }
}
