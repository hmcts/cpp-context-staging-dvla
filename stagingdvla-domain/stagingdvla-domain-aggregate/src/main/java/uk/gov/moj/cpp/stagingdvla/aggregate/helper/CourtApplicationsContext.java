package uk.gov.moj.cpp.stagingdvla.aggregate.helper;

import static java.util.Objects.isNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.APPRO;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.AggregateConstants.ApplicationType.STDECSJP;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isApplicationContainsOtherTypes;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isCaseReopen;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isCriminalProceedingAppGranted;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isStdecGranted;
import static uk.gov.moj.cpp.stagingdvla.aggregate.helper.OffenceUtil.isSuspendDisqualificationPendingAppealAppGranted;

import uk.gov.justice.cpp.stagingdvla.event.ApplicationTypes;
import uk.gov.justice.cpp.stagingdvla.event.CourtApplications;

import java.util.Arrays;
import java.util.List;

public class CourtApplicationsContext {

    private final List<CourtApplications> courtApplications;
    private final List<ApplicationTypes> sjpCaseToCcReferredApplications;

    private Boolean hasAppealResultOrGranted;
    private Boolean isCaseReopened;
    private Boolean isStatDec;
    private Boolean isCriminalProceeding;
    private Boolean isSuspendDisqualificationPendingAppeal;
    private Boolean isOtherApplication;

    public List<CourtApplications> getCourtApplications() {
        return courtApplications;
    }

    public List<ApplicationTypes> getSjpCaseToCcReferredApplications() {
        return sjpCaseToCcReferredApplications;
    }

    public CourtApplicationsContext(final List<CourtApplications> courtApplications, final List<ApplicationTypes> sjpCaseToCcReferredApplications) {
        this.courtApplications = courtApplications;
        this.sjpCaseToCcReferredApplications = sjpCaseToCcReferredApplications;
    }

    public boolean isStatDec() {
        if (isNull(isStatDec)) {
            isStatDec = isStdecGranted(courtApplications) || isSjpCaseReferredStDec();
        }
        return isStatDec;
    }


    public boolean isCaseReopened() {
        if (isNull(isCaseReopened)) {
            isCaseReopened = isCaseReopen(courtApplications) || isSjpCaseReferredReopen();
        }
        return isCaseReopened;
    }

    public boolean hasAppealResultOrGranted() {
        if (isNull(hasAppealResultOrGranted)) {
            hasAppealResultOrGranted = OffenceUtil.hasAppealResultOrGranted(courtApplications);
        }
        return hasAppealResultOrGranted;
    }

    public boolean isCriminalProceeding() {
        if (isNull(isCriminalProceeding)) {
            isCriminalProceeding = isCriminalProceedingAppGranted(courtApplications);
        }
        return isCriminalProceeding;
    }

    public boolean isSuspendDisqualificationPendingAppeal() {
        if (isNull(isSuspendDisqualificationPendingAppeal)) {
            isSuspendDisqualificationPendingAppeal = isSuspendDisqualificationPendingAppealAppGranted(courtApplications);
        }
        return isSuspendDisqualificationPendingAppeal;
    }

    public boolean isContextApplication() {
        return (hasAppealResultOrGranted() || isCaseReopened() || isStatDec() || isCriminalProceeding() || isSuspendDisqualificationPendingAppeal() || isSjpCaseReferred()) && !isOtherApplication();
    }

    public boolean isOtherApplication() {
        if (isNull(isOtherApplication)) {
            isOtherApplication = isApplicationContainsOtherTypes(courtApplications) || isSjpCaseReferredOtherApplications();
        }
        return isOtherApplication;
    }

    public boolean isSjpCaseReferred() {
        return isNotEmpty(sjpCaseToCcReferredApplications) &&
                sjpCaseToCcReferredApplications.stream()
                        .anyMatch(applicationTypes -> Arrays.stream(AggregateConstants.ApplicationType.values())
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
