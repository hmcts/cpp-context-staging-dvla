package uk.gov.moj.cpp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.persistence.entity.DrivingConvictionRetryEntity;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class DrivingConvictionRetryRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingdvla-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private DrivingConvictionRetryRepository drivingConvictionRetryRepository;

    @BeforeEach
    void openEntityManagerAndCreateRepository() {
        drivingConvictionRetryRepository = new DrivingConvictionRetryRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(drivingConvictionRetryRepository);
    }

    @Test
    public void shouldSaveAndFindById() {
        final DrivingConvictionRetryEntity saved = drivingConvictionRetryRepository.save(anEntity());

        assertThat(drivingConvictionRetryRepository.findBy(saved.getConvictionId()), is(saved));
    }

    @Test
    public void shouldFindAll() {
        drivingConvictionRetryRepository.save(anEntity());
        drivingConvictionRetryRepository.save(anEntity());

        assertThat(drivingConvictionRetryRepository.findAll().size(), is(2));
    }

    @Test
    public void shouldFindAllWithPagination() {
        drivingConvictionRetryRepository.save(anEntity());
        drivingConvictionRetryRepository.save(anEntity());
        drivingConvictionRetryRepository.save(anEntity());

        final List<DrivingConvictionRetryEntity> firstPage = drivingConvictionRetryRepository.findAll(0, 2);

        assertThat(firstPage.size(), is(2));
    }

    @Test
    public void shouldRemove() {
        final DrivingConvictionRetryEntity saved = drivingConvictionRetryRepository.save(anEntity());

        drivingConvictionRetryRepository.remove(saved);

        assertThat(drivingConvictionRetryRepository.findAll().size(), is(0));
    }

    @Test
    public void shouldRemoveADetachedEntityByMergingItFirst() {
        final DrivingConvictionRetryEntity saved = drivingConvictionRetryRepository.save(anEntity());
        // a fresh instance with the same id is not managed, so remove() takes the merge branch
        final DrivingConvictionRetryEntity detached = new DrivingConvictionRetryEntity(saved.getConvictionId(), randomUUID(), now());

        drivingConvictionRetryRepository.remove(detached);

        assertThat(drivingConvictionRetryRepository.findAll().size(), is(0));
    }

    private DrivingConvictionRetryEntity anEntity() {
        return new DrivingConvictionRetryEntity(randomUUID(), randomUUID(), now());
    }
}
