package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Arrays.asList;

import java.util.List;

public class AggregateConstants {

    public static final String DEFENDANT_DRIVING_LICENCE_NUMBER = "defendantDrivingLicenceNumber";
    public static final String LICENCE_PRODUCED_IN_COURT = "licenceProducedInCourt";
    public static final String LICENCE_ISSUE_NUMBER = "licenceIssueNumber";
    public static final String DATE_OF_CONVICTION = "dateOfConviction";
    public static final String CONVICTING_COURT = "convictingCourt";
    public static final String DVLACODE_FOR_OFFENCE = "dVLACodeForOffence";

    public static final String DISQUALIFICATION_PERIOD = "disqualificationPeriod";
    public static final String STARTING_FROM_DATE_DATE_OF_INTERIM_DISQUALIFICATION = "startingFromDateDateOfInterimDisqualification";
    public static final String DATE_DISQUALIFICATION_ENDS = "dateDisqualificationEnds";

    public static final String CONVICTED = "Convicted";
    public static final String DEFAULT_DVLA_CODE = "NE98";
    public static final String CROWN_COURT_CODE_PREFIX = "0";
    public static final String C_FOR_CROWN = "C";

    public static final String TT99 = "TT99";
    public static final String AOF = "AOF";
    public static final String PENALTY_POINTS = "PENPT";
    public static final String FINE = "FINE";
    public static final String NOTIONAL_PENALTY_POINTS = "notionalPenaltyPoints";
    public static final String ALCOHOL_DRUG_MAX_LEVEL = "999";

    public static final String NO_DURATION_VALUE = "000";
    public static final String PERIOD_00 = "00";
    public static final String PERIOD_LIFETIME = "999999";
    public static final String POINTS_DISQUALIFICATION_CODE = "TT99";

    public static final String UPDATED = "UPDATED";
    public static final String REMOVED = "REMOVED";

    public enum EndorsementStatus {
        UPDATE_MERGE, UPDATE_NOMERGE, REMOVE
    }

    public enum ResultType {
        AACA("177f29cd-49e9-41d1-aafb-5730d7414dd4"),
        AACD("548bf6b7-d152-4f08-8c9e-a079bf377e9b"),
        AASA("d861586a-df88-440d-98d4-63cc2a680ae1"),
        AASD("573b195d-5795-43f1-92bb-204de1305b8b"),
        ACSD("3b1f0a20-15cf-4795-98b1-ea87ebab2ec6"),
        APA("48b8ff83-2d5d-4891-bab1-b0f5edcd3822"),
        ASV("a2640f68-104b-4ef6-9758-56956bd61825"),
        AW("453539d1-c1a0-475d-9a02-16a659e6bc34"),
        SV("55f15ecf-ea80-40f4-848d-29e7b8d73ae2"),
        DDDTL("77aeba67-1191-4cd6-9a05-ad2e737c80ec"),
        DDOTEL("d1783a6d-c598-4bf1-add8-e5a45cb915ea"),
        DDPL("651cbf5b-b7f0-4921-ba01-e18ad2ac763b"),
        DDPTEL("1ab51418-7f44-4296-aa0f-d032d581ebeb"),
        DDRAL("2d1e7af9-e6c4-446f-b235-1eeafb8ca698"),
        DDRNL("df0c3533-e4ce-4e10-b63f-8e81bd9c4242"),
        DDRVL("94abdf77-0fff-4040-ab38-93a29043012e"),
        DDRE("4ad82d44-41cc-4e47-a2f5-e0dd147320ee"),
        DDRI("4414bffb-c62e-442c-841a-2ebed0e688b6"),
        DSPA("238fc228-f948-430e-b0a1-7c9bdcafac46"),
        DSPAS("d3ca848c-ab33-4650-a0e6-36f1b86998a2"),
        DDDL("a67b959b-b2e6-4741-b758-d7ef27f973a7"),
        LPIC1("c3a39c2f-b056-442f-8dfc-604b5434f956"),
        LPIC2("4b850aa9-984c-4a15-a1ae-b2f861cfcbe8"),
        LPIC3("c83def9d-9a92-4ea0-8923-87fdd7601077"),
        LPIC4("b3c7a167-72fa-46d2-b59c-8cda04fc9729"),
        LPIC5("5c5a693f-c26c-4352-bbb3-ac72dd141e88"),
        RFSD("d3902789-4cc8-4753-a15f-7e26dd39f6ae"),
        COV("4a026ff3-e1aa-407e-aafa-015723c1dbd0"),
        G("2b3f7c20-8fc1-4fad-9076-df196c24b27e"),
        TEXT("98138ec8-5dd3-11e8-9c2d-fa7ae01bbebc"),
        ADJ("d278650c-e429-11e8-9f32-f2801f1b9fd1"),
        ERR("c36fed37-b635-4303-b514-2273e9e2594d"),
        REMUB("d076bd4a-17d5-4720-899a-1c6f96e3b35f");

        public final String id;

        private ResultType(String id) {
            this.id = id;
        }
    }

    public enum ApplicationType {
        ACP("4e281610-96aa-3711-aecf-59df86b6c6bb","Application within criminal proceedings"),
        APPRO("44c238d9-3bc2-3cf3-a2eb-a7d1437b8383", "Application to reopen case");

        public final String id;
        public final String appType;

        ApplicationType(final String id, final String appType) {
            this.id = id;
            this.appType = appType;
        }
    }

    private AggregateConstants() {
    }

    public static List<String> getDistinctPromptReferences() {
        return asList(DEFENDANT_DRIVING_LICENCE_NUMBER, LICENCE_PRODUCED_IN_COURT, LICENCE_ISSUE_NUMBER, DATE_OF_CONVICTION, CONVICTING_COURT);
    }
}
