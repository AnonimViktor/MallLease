BEGIN;

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE role (
    role_id     SERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL
);

CREATE TABLE users (
    user_id       SERIAL PRIMARY KEY,
    role_id       INT          NOT NULL REFERENCES role(role_id),
    login         VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(128) NOT NULL UNIQUE,
    phone         VARCHAR(32),
    full_name     VARCHAR(256) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_users_role ON users(role_id);

CREATE TABLE client (
    client_id      SERIAL PRIMARY KEY,
    user_id        INT          NOT NULL UNIQUE REFERENCES users(user_id),
    company_name   VARCHAR(256) NOT NULL,
    legal_address  VARCHAR(512),
    bank_details   VARCHAR(512),
    status         VARCHAR(32)  NOT NULL DEFAULT 'active'
);

CREATE TABLE shopping_center (
    shopping_center_id SERIAL PRIMARY KEY,
    name               VARCHAR(256) NOT NULL,
    address            VARCHAR(512) NOT NULL,
    image_url          VARCHAR(512),
    map_path           VARCHAR(512)
);

CREATE TABLE trade_point (
    trade_point_id      SERIAL PRIMARY KEY,
    shopping_center_id  INT            NOT NULL REFERENCES shopping_center(shopping_center_id),
    point_code          VARCHAR(32)    NOT NULL,
    floor               INT            NOT NULL,
    area_m2             NUMERIC(10, 2) NOT NULL,
    has_air_conditioner BOOLEAN        NOT NULL DEFAULT FALSE,
    current_daily_rate  NUMERIC(12, 2) NOT NULL,
    is_active           BOOLEAN        NOT NULL DEFAULT TRUE,
    image_url           VARCHAR(512)
);

CREATE INDEX idx_trade_point_center ON trade_point(shopping_center_id);
CREATE INDEX idx_trade_point_floor  ON trade_point(shopping_center_id, floor);

CREATE TABLE showing (
    showing_id      SERIAL PRIMARY KEY,
    client_id       INT          NOT NULL REFERENCES client(client_id),
    manager_user_id INT          NOT NULL REFERENCES users(user_id),
    shown_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    result          VARCHAR(64),
    comment         TEXT
);

CREATE INDEX idx_showing_client  ON showing(client_id);
CREATE INDEX idx_showing_manager ON showing(manager_user_id);

CREATE UNIQUE INDEX showing_manager_slot_uniq
    ON showing (manager_user_id, shown_at)
    WHERE result IS NULL OR result <> 'refused';

CREATE TABLE showing_point (
    showing_id INT NOT NULL REFERENCES showing(showing_id) ON DELETE CASCADE,
    point_id   INT NOT NULL REFERENCES trade_point(trade_point_id),
    PRIMARY KEY (showing_id, point_id)
);

CREATE TABLE contract (
    contract_id     SERIAL PRIMARY KEY,
    client_id       INT          NOT NULL REFERENCES client(client_id),
    contract_no     VARCHAR(64)  NOT NULL UNIQUE,
    signed_at       DATE         NOT NULL DEFAULT CURRENT_DATE,
    status          VARCHAR(32)  NOT NULL DEFAULT 'active',
    comment         TEXT
);

CREATE INDEX idx_contract_client ON contract(client_id);

CREATE TABLE contract_rental (
    contract_id      INT            NOT NULL REFERENCES contract(contract_id) ON DELETE CASCADE,
    point_id         INT            NOT NULL REFERENCES trade_point(trade_point_id),
    date_from        DATE           NOT NULL,
    date_to          DATE           NOT NULL,
    daily_rate_fixed NUMERIC(12, 2) NOT NULL,
    status           VARCHAR(32)    NOT NULL DEFAULT 'active',
    PRIMARY KEY (contract_id, point_id),
    CONSTRAINT chk_rental_dates CHECK (date_from <= date_to),

    CONSTRAINT contract_rental_no_overlap EXCLUDE USING gist (
        point_id WITH =,
        daterange(date_from, date_to, '[]') WITH &&
    ) WHERE (status = 'active')
);

CREATE TABLE monthly_charges (
    charge_id   SERIAL         PRIMARY KEY,
    contract_id INT            NOT NULL,
    point_id    INT            NOT NULL,
    month       DATE           NOT NULL,
    amount_due  NUMERIC(14, 2) NOT NULL,
    status      VARCHAR(32)    NOT NULL DEFAULT 'unpaid',
    CONSTRAINT fk_charge_rental FOREIGN KEY (contract_id, point_id)
        REFERENCES contract_rental(contract_id, point_id) ON DELETE CASCADE,
    CONSTRAINT chk_charge_amount_positive CHECK (amount_due > 0),
    CONSTRAINT charge_period_uniq UNIQUE (contract_id, point_id, month)
);

CREATE INDEX idx_charge_rental ON monthly_charges(contract_id, point_id);

CREATE TABLE payment (
    payment_id  SERIAL         PRIMARY KEY,
    charge_id   INT            NOT NULL REFERENCES monthly_charges(charge_id) ON DELETE CASCADE,
    paid_at     DATE           NOT NULL DEFAULT CURRENT_DATE,
    amount      NUMERIC(14, 2) NOT NULL,
    document_no VARCHAR(64),
    comment     TEXT,
    CONSTRAINT chk_payment_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payment_charge ON payment(charge_id);

COMMIT;
