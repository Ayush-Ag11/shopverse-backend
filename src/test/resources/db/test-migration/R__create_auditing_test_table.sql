CREATE TABLE IF NOT EXISTS shopverse.auditing_test_entity
(
    id         UUID                     NOT NULL,
    name       VARCHAR(100)             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                             version    BIGINT                   NOT NULL,

                             CONSTRAINT pk_auditing_test_entity PRIMARY KEY (id)
    );
