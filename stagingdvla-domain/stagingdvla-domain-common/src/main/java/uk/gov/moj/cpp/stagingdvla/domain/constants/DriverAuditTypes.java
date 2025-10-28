package uk.gov.moj.cpp.stagingdvla.domain.constants;

public enum DriverAuditTypes {
    CE("Case Enquiry"),
    FPE("Fixed Penalty Enquiry"),
    ACE("Auto Case Enquiry");
    private String reasonType;

    DriverAuditTypes(final String reasonType) {
        this.reasonType = reasonType;
    }

    public String getReasonType() {
        return reasonType;
    }
}
