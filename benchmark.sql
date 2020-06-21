CREATE SCHEMA IF NOT EXISTS test;

DROP TABLE IF EXISTS test.event;

create table test.event
(
    id uuid NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    destination VARCHAR(255),
    payload VARCHAR(4096),
    timestamp BIGINT NOT NULL,
    emitted boolean NOT NULL DEFAULT false
)
;
