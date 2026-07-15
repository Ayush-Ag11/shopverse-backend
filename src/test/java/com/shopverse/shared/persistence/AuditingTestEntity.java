package com.shopverse.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "auditing_test_entity", schema = "shopverse")
public class AuditingTestEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    protected AuditingTestEntity() {
    }

    public AuditingTestEntity(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
