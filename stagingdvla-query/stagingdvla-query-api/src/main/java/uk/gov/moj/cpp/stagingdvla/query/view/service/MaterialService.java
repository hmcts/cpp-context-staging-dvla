package uk.gov.moj.cpp.stagingdvla.query.view.service;

import static java.util.UUID.fromString;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.material.client.MaterialClient;
import uk.gov.moj.cpp.stagingdvla.query.api.error.SystemUserIdNotAvailableException;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

public class MaterialService {

    private static final Logger LOGGER = getLogger(MaterialService.class);

    @Inject
    private MaterialClient materialClient;

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    public Response getMaterialResource(final String materialId) {

        final UUID systemUserId = serviceContextSystemUserProvider.getContextSystemUserId().orElseThrow(SystemUserIdNotAvailableException::new);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MaterialService - System User Id is {} ", systemUserId);
        }
        return getMaterial(materialId, systemUserId);

    }

    private Response getMaterial(final String materialId, final UUID userId) {
        return materialClient.getMaterial(fromString(materialId), userId);
    }

}
