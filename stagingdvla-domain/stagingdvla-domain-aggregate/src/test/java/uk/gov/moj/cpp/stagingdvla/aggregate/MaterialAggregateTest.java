package uk.gov.moj.cpp.stagingdvla.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;

import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialAggregateTest {

    @InjectMocks
    private MaterialAggregate aggregate;

    @BeforeEach
    public void setUp() {
        aggregate = new MaterialAggregate();
    }

    @Test
    public void shouldCreate() {
        final UUID applicationId = randomUUID();
        final UUID materialId = randomUUID();
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withApplicationId(applicationId)
                .withMaterialId(materialId)
                .build();
        aggregate.apply(materialDetails);
        final List<Object> eventStream = aggregate.create(materialDetails).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(NowsMaterialRequestRecorded.class)));
        assertThat(eventStream.get(0).getClass(), is(CoreMatchers.equalTo(NowsMaterialRequestRecorded.class)));
    }

}
