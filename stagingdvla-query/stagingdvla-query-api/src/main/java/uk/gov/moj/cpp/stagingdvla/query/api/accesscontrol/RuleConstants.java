package uk.gov.moj.cpp.stagingdvla.query.api.accesscontrol;

@SuppressWarnings("WeakerAccess")
public class RuleConstants {

    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_MAGISTRATES = "Magistrates";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";
    private static final String GROUP_SYSTEM_USERS = "System Users";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_FIXED_PENALTY_ADMINISTRATORS = "Fixed Penalty Administrators";
    private static final String GROUP_RECORDERS = "Recorders";
    private static final String GROUP_DJMC = "DJMC";
    private static final String GROUP_DEPUTIES = "Deputies";
    private static final String GROUP_JUDGE = "Judge";
    private static final String GROUP_DISTRICT_JUDGE = "District Judge";
    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_SECOND_LINE_SUPPORT = "Second Line Support";
    private static final String GROUP_AUDITORS = "Auditors";

    private RuleConstants() {
    }

    public static String[] getQueryDriverDetails() {
        return new String[]{GROUP_COURT_CLERKS, GROUP_SYSTEM_USERS, GROUP_COURT_ASSOCIATE,
                GROUP_LEGAL_ADVISERS, GROUP_FIXED_PENALTY_ADMINISTRATORS, GROUP_RECORDERS,
                GROUP_DJMC, GROUP_DEPUTIES, GROUP_COURT_ADMINISTRATORS, GROUP_JUDGE,
                GROUP_DISTRICT_JUDGE, GROUP_CROWN_COURT_ADMIN, GROUP_SECOND_LINE_SUPPORT, GROUP_MAGISTRATES};
    }

    public static String[] getQueryDriverSearchAuditDetails() {
        return new String[]{GROUP_AUDITORS};
    }

}
