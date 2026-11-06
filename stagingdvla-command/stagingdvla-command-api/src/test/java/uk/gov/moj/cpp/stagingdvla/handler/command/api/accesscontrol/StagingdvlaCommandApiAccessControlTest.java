package uk.gov.moj.cpp.stagingdvla.handler.command.api.accesscontrol;

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
import org.mockito.Mock;

/*
 * CHD-03 (BC-20, with the BC-03 residual guard). The command API had no access-control test, so these pin the
 * Java 17 behaviour of stagingdvla-command-api.drl: the kbase loads all three rules, driver notification is
 * allowed for the notify groups, the audit-report commands for Auditors only, and anything else is denied.
 */
public class StagingdvlaCommandApiAccessControlTest extends BaseDroolsAccessControlTest {

    private static final String DRIVER_NOTIFICATION = "stagingdvla.command.driver-notification";
    private static final List<String> AUDIT_REPORT_COMMANDS = List.of(
            "stagingdvla.command.generate-driver-record-search-audit-report",
            "stagingdvla.command.delete-driver-record-search-audit-report");

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public StagingdvlaCommandApiAccessControlTest() {
        super("COMMAND_API_SESSION");
    }

    @Test
    public void chd03_shouldLoadAllThreeCommandRulesIntoTheCommandApiKbase() {
        final List<String> ruleNames = KieServices.get().getKieClasspathContainer().getKieBase("COMMAND_API").getKiePackages().stream()
                .map(KiePackage::getRules)
                .flatMap(rules -> rules.stream().map(Rule::getName))
                .toList();

        assertThat(ruleNames, containsInAnyOrder(
                "Command - API - Action - " + DRIVER_NOTIFICATION,
                "Command - API - Action - " + AUDIT_REPORT_COMMANDS.get(0),
                "Command - API - Action - " + AUDIT_REPORT_COMMANDS.get(1)));
    }

    @Test
    public void chd03_shouldAllowDriverNotificationForTheNotifyGroups() {
        final Action action = createActionFor(DRIVER_NOTIFICATION);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCommandDriverNotify())).willReturn(true);

        assertSuccessfulOutcome(executeRulesWith(action));
    }

    @Test
    public void chd03_shouldDenyDriverNotificationOutsideTheNotifyGroups() {
        final Action action = createActionFor(DRIVER_NOTIFICATION);

        assertFailureOutcome(executeRulesWith(action));
        verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCommandDriverNotify());
    }

    @Test
    public void chd03_shouldAllowAuditReportCommandsForAuditors() {
        for (final String actionName : AUDIT_REPORT_COMMANDS) {
            final Action action = createActionFor(actionName);
            given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCommandDriverSearchAudit())).willReturn(true);

            assertSuccessfulOutcome(executeRulesWith(action));
        }
    }

    @Test
    public void chd03_shouldDenyAuditReportCommandsForNonAuditors() {
        for (final String actionName : AUDIT_REPORT_COMMANDS) {
            final Action action = createActionFor(actionName);

            assertFailureOutcome(executeRulesWith(action));
            verify(userAndGroupProvider).isMemberOfAnyOfTheSuppliedGroups(action, RuleConstants.getCommandDriverSearchAudit());
        }
    }

    @Test
    public void chd03_shouldDenyACommandThatNoRuleNames() {
        assertFailureOutcome(executeRulesWith(createActionFor("stagingdvla.command.not-a-real-command")));
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
