CREATE TABLE documents (
    id                BINARY(16)   NOT NULL,
    type              VARCHAR(50)  NOT NULL,
    status            VARCHAR(50)  NOT NULL,
    created_at        DATETIME(6),
    expire_at         DATE,
    s3_key            VARCHAR(255),
    original_filename VARCHAR(255),
    id_user           BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_documents_user FOREIGN KEY (id_user) REFERENCES users (id)
);
