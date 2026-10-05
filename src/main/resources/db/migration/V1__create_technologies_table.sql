-- Faithful replica of the `technologies` table that already existed in
-- personaldb (created outside Flyway) before this service existed - see
-- ADR-0005. Against personaldb itself, Flyway baselines this version
-- instead of executing it (see spring.flyway.baseline-* in
-- application.yaml); this file only actually runs against a fresh/empty
-- database (e.g. the Testcontainers-backed repository test), where it must
-- reproduce the real schema exactly.
CREATE TYPE technology_category AS ENUM ('stack', 'in_progress');

CREATE TABLE technologies
(
    id         SERIAL PRIMARY KEY,
    name       TEXT                 NOT NULL,
    category   technology_category  NOT NULL,
    confidence SMALLINT             NOT NULL CHECK (confidence BETWEEN 1 AND 5),
    CONSTRAINT technologies_name_category_key UNIQUE (name, category)
);
