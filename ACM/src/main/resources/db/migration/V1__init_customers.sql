CREATE TABLE customers (
                           id BIGSERIAL PRIMARY KEY,
                           first_name   VARCHAR(100) NOT NULL,
                           last_name    VARCHAR(100) NOT NULL,
                           username     VARCHAR(100) NOT NULL UNIQUE,
                           password     VARCHAR(255) NOT NULL,
                           national_id  VARCHAR(20)  NOT NULL UNIQUE,
                           role         VARCHAR(20)  NOT NULL,
                           kyc_level    VARCHAR(20)  NOT NULL DEFAULT 'BASIC',
                           status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                           created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_customer_national_id
    ON customers (national_id);
