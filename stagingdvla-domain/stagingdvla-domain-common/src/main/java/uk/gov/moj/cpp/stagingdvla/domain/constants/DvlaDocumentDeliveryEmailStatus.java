package uk.gov.moj.cpp.stagingdvla.domain.constants;

public enum DvlaDocumentDeliveryEmailStatus {
    NOT_REQUIRED,
    PENDING,
    SUCCESS,
    FAILED,
    // the gateway rejected the command itself (payload validation failure) before ever attempting
    // the send - a different failure point from FAILED (GOV.UK Notify rejecting an accepted,
    // well-formed request), kept distinct so the two can be triaged/alerted differently later
    VALIDATION_FAILED
}
