package com.shopverse.shared.persistence;

import com.shopverse.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class BaseEntityAuditingIntegrationTest {

    @Autowired
    private AuditingTestEntityRepository repository;

    @Test
    void shouldGenerateIdAndPopulateAuditFields() {
        AuditingTestEntity entity = new AuditingTestEntity("Initial name");

        AuditingTestEntity savedEntity = repository.saveAndFlush(entity);

        assertThat(savedEntity.getId()).isNotNull();
        assertThat(savedEntity.getCreatedAt()).isNotNull();
        assertThat(savedEntity.getUpdatedAt()).isNotNull();
        assertThat(savedEntity.getVersion()).isZero();
    }

    @Test
    void shouldPreserveCreationTimeAndUpdateModificationTimeAndVersion() {
        AuditingTestEntity entity =
            repository.saveAndFlush(new AuditingTestEntity("Initial name"));

        Instant originalCreatedAt = entity.getCreatedAt();
        Instant originalUpdatedAt = entity.getUpdatedAt();
        long originalVersion = entity.getVersion();

        entity.rename("Updated name");

        AuditingTestEntity updatedEntity = repository.saveAndFlush(entity);

        assertThat(updatedEntity.getCreatedAt())
            .isEqualTo(originalCreatedAt);

        assertThat(updatedEntity.getUpdatedAt())
            .isAfterOrEqualTo(originalUpdatedAt);

        assertThat(updatedEntity.getVersion())
            .isGreaterThan(originalVersion);
    }
}
