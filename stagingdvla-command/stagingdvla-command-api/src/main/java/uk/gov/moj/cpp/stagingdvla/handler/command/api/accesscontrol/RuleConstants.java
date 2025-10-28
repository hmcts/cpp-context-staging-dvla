package uk.gov.moj.cpp.stagingdvla.handler.command.api.accesscontrol;

public class RuleConstants {

    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";
    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_MAGISTRATES = "Magistrates";
    private static final String GROUP_AUDITORS = "Auditors";

    private RuleConstants() {
    }

    public static String[] getCommandDriverNotify() {
        return new String[]{GROUP_COURT_CLERKS, GROUP_SYSTEM_USERS, GROUP_COURT_ASSOCIATE,
                GROUP_LEGAL_ADVISERS, GROUP_MAGISTRATES};
    }

    public static String[] getCommandDriverSearchAudit() {
        return new String[]{GROUP_AUDITORS};
    }


}
