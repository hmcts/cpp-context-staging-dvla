package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.APPRO;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.STDECSJP;

import uk.gov.justice.cpp.stagingdvla.event.ApplicationTypes;

import java.util.Arrays;
import java.util.List;

public record ApplicationContext(boolean isAppeal,
                                 boolean isCaseReopened,
                                 boolean isStatDec,
                                 boolean isCriminalProceeding,
                                 boolean isSuspendDisqualificationPendingAppeal,
                                 boolean isOtherApplication,
                                 List<ApplicationTypes> sjpCaseToCcReferredApplications) {

    @Override
    public boolean isStatDec() {
        return isStatDec || isSjpCaseReferredStDec();
    }

    @Override
    public boolean isCaseReopened() {
        return isCaseReopened || isSjpCaseReferredReopen();
    }

    public boolean isContextApplication() {
        return (isAppeal || isCaseReopened || isStatDec || isCriminalProceeding || isSuspendDisqualificationPendingAppeal || isSjpCaseReferred()) && !isOtherApplication;
    }

    @Override
    public boolean isOtherApplication() {
        return isOtherApplication || isSjpCaseReferredOtherApplications();
    }

    public boolean isSjpCaseReferred() {
        return isNotEmpty( sjpCaseToCcReferredApplications) &&
                sjpCaseToCcReferredApplications.stream()
                        .anyMatch(applicationTypes-> Arrays.stream(AggregateConstants.ApplicationType.values())
                                .anyMatch(applicationType -> applicationType.id.equals(applicationTypes.getId()) ||
                                        applicationType.appType.equalsIgnoreCase(applicationTypes.getName())));
    }

    private boolean isSjpCaseReferredStDec() {
        return isNotEmpty(sjpCaseToCcReferredApplications) &&
                sjpCaseToCcReferredApplications.stream()
                        .anyMatch(applicationType -> STDECSJP.id.equals(applicationType.getId()) || STDECSJP.appType.equalsIgnoreCase(applicationType.getName()));
    }

    private boolean isSjpCaseReferredReopen() {
        return isNotEmpty(sjpCaseToCcReferredApplications) &&
                sjpCaseToCcReferredApplications.stream()
                        .anyMatch(applicationType -> APPRO.id.equals(applicationType.getId()) || APPRO.appType.equalsIgnoreCase(applicationType.getName()));
    }

    private boolean isSjpCaseReferredOtherApplications() {
        return isNotEmpty(sjpCaseToCcReferredApplications) &&
                sjpCaseToCcReferredApplications.stream()
                        .noneMatch(sjpApplicationType -> Arrays.stream(AggregateConstants.ApplicationType.values())
                                .anyMatch(applicationType -> sjpApplicationType.getName().equals(applicationType.appType)
                                        || sjpApplicationType.getId().equals(applicationType.id)));
    }

}
