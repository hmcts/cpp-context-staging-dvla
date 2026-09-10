package uk.gov.moj.cpp.stagingdvla.service;

import static java.lang.String.format;

import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.stagingdvla.exception.UserNotFoundException;
import uk.gov.moj.cpp.systemidmapper.client.AdditionResponse;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMap;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapperClient;
import uk.gov.moj.cpp.systemidmapper.client.SystemIdMapping;

import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Maps the notification id staging dvla mints for an email send to the materialId of the D20
 * request that caused it, so the notification result events can be correlated back.
 *
 * <p>The result events (
 * {@code public.notificationnotify.events.notification-sent} / {@code ...notification-failed})
 * carry only {@code notificationId} and {@code clientContext}: no case, material or request id, and
 * both are {@code additionalProperties: false} so nothing can be added to them. The notification id
 * must stay unique per notification - a material can be notified more than once - so it cannot
 * simply be the materialId, and this mapping is what closes the gap.
 *
 * <p>This mirrors the equivalent wrapper in cpp-context-correspondence, which maps its own
 * notification ids to caseIds against the same shared platform service.
 */
@ApplicationScoped
public class SystemIdMapperService {

    protected static final String NOTIFICATION_SOURCE_TYPE = "STAGINGDVLA_NOTIFICATION_ID";
    protected static final String MATERIAL_TARGET_TYPE = "MATERIAL_ID";

    @Inject
    private SystemUserProvider systemUserProvider;

    @Inject
    private SystemIdMapperClient systemIdMapperClient;

    /**
     * Looks up the materialId a notification was sent for.
     *
     * @param notificationId the id carried on the inbound notification result event
     * @return the mapping, or empty if this notification was not sent by staging dvla or the
     *         mapping has since gone
     */
    public Optional<SystemIdMapping> getMaterialIdForNotificationId(final String notificationId) {
        return systemIdMapperClient.findBy(notificationId, NOTIFICATION_SOURCE_TYPE, MATERIAL_TARGET_TYPE, getSystemUserId());
    }

    /**
     * Records the mapping. Must be called before the send, so the result event can never arrive
     * ahead of the mapping it needs.
     *
     * @param materialId     the D20 request's correlation id
     * @param notificationId the id being put on the outbound send
     * @throws IllegalStateException if the mapping could not be recorded, which deliberately stops
     *                               the send rather than leaving an uncorrelatable notification
     */
    public void mapNotificationIdToMaterialId(final UUID materialId, final UUID notificationId) {

        final SystemIdMap systemIdMap = new SystemIdMap(notificationId.toString(), NOTIFICATION_SOURCE_TYPE,
                materialId, MATERIAL_TARGET_TYPE);

        final AdditionResponse response = systemIdMapperClient.add(systemIdMap, getSystemUserId());

        if (!response.isSuccess()) {
            throw new IllegalStateException(format("Failed to map materialId: %s to notification id %s",
                    materialId, notificationId));
        }
    }

    private UUID getSystemUserId() {
        return systemUserProvider.getContextSystemUserId().orElseThrow(
                () -> new UserNotFoundException("Context system user id is not available for system id mapping"));
    }
}
