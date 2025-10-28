package uk.gov.moj.cpp.stagingdvla.query.view.converter;

import static java.util.Objects.isNull;

import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;
import uk.gov.moj.cpp.stagingdvla.domain.DrivingConvictionRetry;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"squid:S1168"})
public class DrivingConvictionRetryConverter {

    public List<DrivingConvictionRetry> convert(List<DrivingConvictionRetryEntity> entities) {
        if (isNull(entities)) {
            return null;
        }
        final List<DrivingConvictionRetry> drivingConvictionRetries = new ArrayList<>();
        entities.stream()
                .forEach(entity -> {
                            final DrivingConvictionRetry drivingConvictionRetry = convertToDrivingConvictionRetry(entity);
                            drivingConvictionRetries.add(drivingConvictionRetry);
                        }
                );

        return drivingConvictionRetries;
    }

    public DrivingConvictionRetry convertToDrivingConvictionRetry(DrivingConvictionRetryEntity entity) {
        return DrivingConvictionRetry.drivingConvictionRetry()
                .withConvictionId(entity.getConvictionId())
                .withMasterDefendantId(entity.getMasterDefendantId())
                .build();
    }

}
