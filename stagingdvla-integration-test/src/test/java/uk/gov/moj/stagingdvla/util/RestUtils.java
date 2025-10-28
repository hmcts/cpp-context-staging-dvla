package uk.gov.moj.stagingdvla.util;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

import java.util.concurrent.TimeUnit;

public class RestUtils {

    private static final int DEFAULT_POLL_TIMEOUT_IN_SEC = 30;

    public static RestPoller poll(final RequestParams requestParams) {
        return RestPoller.poll(requestParams).timeout(DEFAULT_POLL_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
    }

    public static RestPoller poll(final RequestParamsBuilder requestParamsBuilder) {
        return RestPoller.poll(requestParamsBuilder).timeout(DEFAULT_POLL_TIMEOUT_IN_SEC, TimeUnit.SECONDS);
    }
}
