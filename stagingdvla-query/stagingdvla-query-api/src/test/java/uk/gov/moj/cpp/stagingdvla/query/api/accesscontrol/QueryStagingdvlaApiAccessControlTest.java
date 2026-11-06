package uk.gov.moj.cpp.stagingdvla.query.api.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

/*
 * Was @Disabled: it used COMMAND_API_SESSION, which the query kmodule.xml does not declare.
 */
public class QueryStagingdvlaApiAccessControlTest extends BaseDroolsAccessControlTest {

    private Action action;

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public QueryStagingdvlaApiAccessControlTest() {
        super("QUERY_API_SESSION");
    }

    @Test
    public void shouldAllowAuthorisedUserToQueryDriver() {
        action = createActionFor("stagingdvla.query.drivernumber");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQueryDriver() {
        action = createActionFor("stagingdvla.query.drivernumber");

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
    }

    @Test
    public void shouldAllowAuthorisedUserToQueryDrivers() {
        action = createActionFor("stagingdvla.query.driverdetails");
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails()))
                .willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
    }

    @Test
    public void shouldNotAllowUnauthorisedUserToQueryDrivers() {
        action = createActionFor("stagingdvla.query.driverdetails");

        final ExecutionResults results = executeRulesWith(action);
        assertFailureOutcome(results);
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
    }

    // CHD-03: the driver search permission alone is enough.
    @Test
    public void chd03_shouldAllowDriverSearchByPermissionWhenUserIsInNoAllowedGroup() throws Exception {
        for (final String actionName : List.of("stagingdvla.query.drivernumber", "stagingdvla.query.driverdetails", "stagingdvla.query.driverimage")) {
            action = createActionFor(actionName);
            given(userAndGroupProvider.hasPermission(action, PermissionConstants.getPermissionsForDriverSearch())).willReturn(true);

            assertSuccessfulOutcome(executeRulesWith(action));
        }
    }

    // CHD-03: driver image is denied with neither the group nor the permission (the other two actions are covered above).
    @Test
    public void chd03_shouldNotAllowUnauthorisedUserToQueryDriverImage() throws Exception {
        action = createActionFor("stagingdvla.query.driverimage");

        assertFailureOutcome(executeRulesWith(action));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
        verify(userAndGroupProvider).hasPermission(action, PermissionConstants.getPermissionsForDriverSearch());
    }

    // CHD-03: audit actions are for Auditors only.
    @Test
    public void chd03_shouldAllowAuditActionsForAuditors() {
        for (final String actionName : List.of("stagingdvla.query.driver-audit-records", "stagingdvla.query.driver-search-audit-reports",
                "stagingdvla.query.driver-search-audit-report-content")) {
            action = createActionFor(actionName);
            given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverSearchAuditDetails())).willReturn(true);

            assertSuccessfulOutcome(executeRulesWith(action));
        }
    }

    @Test
    public void chd03_shouldDenyAuditActionsForNonAuditors() {
        for (final String actionName : List.of("stagingdvla.query.driver-audit-records", "stagingdvla.query.driver-search-audit-reports",
                "stagingdvla.query.driver-search-audit-report-content")) {
            action = createActionFor(actionName);

            assertFailureOutcome(executeRulesWith(action));
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverSearchAuditDetails());
        }
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
