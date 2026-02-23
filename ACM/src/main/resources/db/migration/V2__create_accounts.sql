CREATE TABLE accounts (
                          id BIGSERIAL PRIMARY KEY,
                          account_number VARCHAR(26) NOT NULL,
                          customer_id BIGINT NOT NULL,
                          type VARCHAR(20) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          balance NUMERIC(18,2) NOT NULL DEFAULT 0,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT uk_account_number UNIQUE (account_number),
                          CONSTRAINT fk_account_customer
                              FOREIGN KEY (customer_id)
                                  REFERENCES customers (id)
);
