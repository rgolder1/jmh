USE tempdb;
GO

CREATE SCHEMA jmh;
GO

CREATE TABLE jmh.event
(
    id UNIQUEIDENTIFIER PRIMARY KEY,
    destination VARCHAR(255),
    payload VARCHAR(4096),
    timestamp BIGINT NOT NULL,
    emitted BIT NOT NULL DEFAULT 0
);
GO
