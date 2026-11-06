package uk.gov.moj.cpp.stagingdvla.query.api.accesscontrol;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

/*
 * Was @Disabled with super("COMMAND_API_SESSION"): the query module's kmodule.xml only declares
 * QUERY_API_SESSION, so the session could not be built and no access-control rule here was ever tested.
 * The shared @AfterEach verify is now inside the four original tests, so the CHD-03 tests can use other actions.
 */
public class QueryStagingdvlaApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String QUERY_API_KBASE = "QUERY_API";

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

    /*
     * CHD-03 (BC-20) - Java 17 behaviour: the production kmodule.xml loads all six query rules into the
     * QUERY_API kbase. A kbase that resolves to 0 rules would make every deny test pass vacuously.
     */
    @Test
    public void chd03_shouldLoadAllSixQueryRulesIntoTheQueryApiKbase() {
        final List<String> ruleNames = KieServices.get().getKieClasspathContainer().getKieBase(QUERY_API_KBASE).getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .toList();

        assertThat(ruleNames, containsInAnyOrder(
                "Command - Rule for Query Person",
                "Command - Rule for driver detail Person",
                "Command - Rule for driver image",
                "Query - Rule for Query Driver Audit Details",
                "Query - Rule for Driver Search Audit Reports",
                "Query - Rule for Driver Search Audit Report Download"));
    }

    /*
     * CHD-03 (BC-03 residual guard) - Java 17 behaviour: the driver-search rules are "in a group OR has the
     * Driver Search/View permission", so a user with only the permission is allowed.
     */
    @Test
    public void chd03_shouldAllowDriverSearchByPermissionWhenUserIsInNoAllowedGroup() throws Exception {
        for (final String actionName : List.of("stagingdvla.query.drivernumber", "stagingdvla.query.driverdetails", "stagingdvla.query.driverimage")) {
            action = createActionFor(actionName);
            given(userAndGroupProvider.hasPermission(action, PermissionConstants.getPermissionsForDriverSearch())).willReturn(true);

            assertSuccessfulOutcome(executeRulesWith(action));
        }
    }

    /*
     * CHD-03 - Java 17 behaviour: with neither the group nor the permission, every driver-search action is denied.
     */
    @Test
    public void chd03_shouldDenyDriverSearchWithNeitherGroupNorPermission() throws Exception {
        for (final String actionName : List.of("stagingdvla.query.drivernumber", "stagingdvla.query.driverdetails", "stagingdvla.query.driverimage")) {
            action = createActionFor(actionName);

            assertFailureOutcome(executeRulesWith(action));
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getQueryDriverDetails());
            verify(userAndGroupProvider).hasPermission(action, PermissionConstants.getPermissionsForDriverSearch());
        }
    }

    /*
     * CHD-03 - Java 17 behaviour: the audit actions (records, report list, report download) allow Auditors only.
     */
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

    /*
     * CHD-03 - Java 17 behaviour: an action no rule names is denied (fail closed).
     */
    @Test
    public void chd03_shouldDenyAnActionThatNoRuleNames() {
        action = createActionFor("stagingdvla.query.not-a-real-action");

        assertFailureOutcome(executeRulesWith(action));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
