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

--CREATE INDEX idx_emitted_on_event ON test.event (emitted);
CREATE INDEX idx_timestamp_on_event ON test.event (timestamp);
--CREATE INDEX idx_emitted_and_timestamp_on_event ON test.event (emitted, timestamp);


--DROP INDEX test.idx_emitted_on_event
--DROP INDEX test.idx_timestamp_on_event
--DROP INDEX test.idx_emitted_and_timestamp_on_event
