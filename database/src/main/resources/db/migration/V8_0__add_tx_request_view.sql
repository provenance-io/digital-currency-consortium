UPDATE coin_mint SET status = 'QUEUED' WHERE status = 'INSERTED';
UPDATE coin_mint SET status = 'PENDING' WHERE status LIKE 'PENDING%';
UPDATE coin_mint SET status = 'ACTION_COMPLETE' WHERE status = 'COMPLETE';

UPDATE coin_burn SET status = 'QUEUED' WHERE status = 'INSERTED';
UPDATE coin_burn SET status = 'PENDING' WHERE status LIKE 'PENDING%';
UPDATE coin_burn SET status = 'TXN_COMPLETE' WHERE status = 'COMPLETE';

UPDATE coin_redemption SET status = 'QUEUED' WHERE status = 'INSERTED';
UPDATE coin_redemption SET status = 'PENDING' WHERE status LIKE 'PENDING%';
UPDATE coin_redemption SET status = 'ACTION_COMPLETE' WHERE status = 'COMPLETE';

UPDATE address_registration SET status = 'QUEUED' WHERE status = 'INSERTED';
UPDATE address_registration SET status = 'PENDING' WHERE status LIKE 'PENDING%';
UPDATE address_registration SET status = 'TXN_COMPLETE' WHERE status = 'COMPLETE';

UPDATE address_dereg SET status = 'QUEUED' WHERE status = 'INSERTED';
UPDATE address_dereg SET status = 'PENDING' WHERE status LIKE 'PENDING%';
UPDATE address_dereg SET status = 'TXN_COMPLETE' WHERE status = 'COMPLETE';

DROP INDEX IF EXISTS marker_transfer_txhash_idx;
ALTER TABLE tx_request ADD tx_hash TEXT;
ALTER TABLE tx_request ADD status TEXT;
ALTER TABLE tx_request ADD timeout_height BIGINT;
ALTER TABLE address_registration ADD timeout_height BIGINT;
ALTER TABLE address_registration ADD updated TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc');
ALTER TABLE address_dereg ADD timeout_height BIGINT;
ALTER TABLE address_dereg ADD updated TIMESTAMPTZ NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc');

UPDATE coin_mint
SET tx_hash = tx_status.tx_hash, updated = tx_status.created
FROM tx_status
WHERE tx_status.tx_request_uuid = coin_mint.uuid
    AND tx_status.type = 'MINT_CONTRACT' AND coin_mint.tx_hash IS NULL;

UPDATE coin_burn
SET tx_hash = tx_status.tx_hash, updated = tx_status.created
FROM tx_status
WHERE tx_status.tx_request_uuid = coin_burn.uuid
    AND tx_status.type = 'BURN_CONTRACT' AND coin_burn.tx_hash IS NULL;

UPDATE coin_redemption
SET tx_hash = tx_status.tx_hash, updated = tx_status.created
FROM tx_status
WHERE tx_status.tx_request_uuid = coin_redemption.uuid
    AND tx_status.type = 'REDEEM_CONTRACT' AND coin_redemption.tx_hash IS NULL;

CREATE TABLE coin_redeem_burn() INHERITS (tx_request);

CREATE VIEW tx_request_view AS
SELECT DISTINCT
    uuid,
    'MINT' AS type,
    tx_hash,
    status,
    timeout_height,
    created,
    updated
FROM coin_mint
UNION ALL
SELECT DISTINCT
    uuid,
    'REDEEM_BURN' AS type,
    tx_hash,
    status,
    timeout_height,
    created,
    updated
FROM coin_redeem_burn
UNION ALL
SELECT
    uuid,
    'TAG' AS type,
    tx_hash,
    status,
    timeout_height,
    created,
    created
FROM address_registration
UNION ALL
SELECT
    uuid,
    'DETAG' AS type,
    tx_hash,
    status,
    timeout_height,
    created,
    created
FROM address_dereg;

