package com.shopverse.shared.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditingTestEntityRepository extends JpaRepository<AuditingTestEntity, UUID> {
}
