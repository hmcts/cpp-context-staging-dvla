package uk.gov.moj.cpp.stagingdvla.notify.azure;

import uk.gov.justice.services.common.configuration.Value;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class DvlaApimConfig {

    @Inject
    @Value(key = "dvlaEnquiryApimUrl")
    private String dvlaEnquiryApimUrl;

    @Inject
    @Value(key = "stagingdvla.driving.conviction.max.retry", defaultValue = "10")
    private String drivingConvictionMaxRetry;

    @Inject
    @Value(key = "stagingdvla.driving.conviction.retry.interval.in.minutes", defaultValue = "60")
    private String drivingConvictionRetryIntervalInMinutes;

    @Inject
    @Value(key = "stagingdvla.driving.conviction.retry.max.record.count", defaultValue = "0")
    private String drivingConvictionRetryMaxRecordCount;

    @Inject
    @Value(key = "mireportdata.caseFilter.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    public String getDvlaEnquiryApimUrl() {
        return dvlaEnquiryApimUrl;
    }

    public String getDrivingConvictionMaxRetry() {
        return drivingConvictionMaxRetry;
    }

    public String getDrivingConvictionRetryIntervalInMinutes() {
        return drivingConvictionRetryIntervalInMinutes;
    }

    public String getDrivingConvictionRetryMaxRecordCount() {
        return drivingConvictionRetryMaxRecordCount;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }
}
