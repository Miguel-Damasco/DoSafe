CREATE TABLE users (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    username       VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    email          VARCHAR(255),
    role           VARCHAR(50)  NOT NULL,
    email_verified TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);
