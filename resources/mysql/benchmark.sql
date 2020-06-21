CREATE SCHEMA IF NOT EXISTS jmh;

DROP TABLE IF EXISTS jmh.event;

create table jmh.event
(
    id BINARY(16) PRIMARY KEY,
    destination VARCHAR(255),
    payload VARCHAR(4096),
    timestamp BIGINT NOT NULL,
    emitted boolean NOT NULL DEFAULT false
)
;
