CREATE TABLE ledger_accounts (
                                 id BIGSERIAL PRIMARY KEY,
                                 code VARCHAR(50) NOT NULL,
                                 name VARCHAR(255) NOT NULL,
                                 type VARCHAR(20) NOT NULL,

                                 CONSTRAINT uk_ledger_account_code UNIQUE (code)
);
