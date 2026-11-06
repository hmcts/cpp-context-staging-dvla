package uk.gov.moj.cpp.stagingdvla.handler.command.api.accesscontrol;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/*
 * CHD-03: pins the command API access-control rules (stagingdvla-command-api.drl).
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

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return Collections.singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }
}
