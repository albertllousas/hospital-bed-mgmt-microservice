CREATE TABLE hospital_beds
(
    id                  UUID        NOT NULL,
    room_id             TEXT        NOT NULL,
    ward                TEXT        NOT NULL,
    status              TEXT        NOT NULL,
    occupied_patient_id TEXT,
    occupied_from       TIMESTAMPTZ,
    occupied_to         TIMESTAMPTZ,
    features            TEXT[]      NOT NULL,
    created             TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    version             BIGINT      NOT NULL,
    CONSTRAINT pk_hospital_bed PRIMARY KEY (id)
);


CREATE TABLE outbox
(
    id                UUID PRIMARY KEY,
    aggregateid       UUID                     NOT NULL,
    aggregatetype     TEXT                     NOT NULL,
    event_type        TEXT                     NOT NULL,
    aggregate_version BIGINT                   NOT NULL,
    payload           BYTEA                    NOT NULL,
    occurred_on       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
