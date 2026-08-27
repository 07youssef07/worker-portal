-- Run against your workerdb database, e.g.:
--   psql -U workerapp -d workerdb -f schema.sql
-- (if you get "permission denied for schema public", first connect as
--  postgres and run: GRANT ALL ON SCHEMA public TO workerapp;)

CREATE TABLE IF NOT EXISTS app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50) NOT NULL DEFAULT 'VIEWER'
);

CREATE TABLE IF NOT EXISTS worker (
    id             BIGSERIAL PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    date_of_birth  DATE NOT NULL,
    role           VARCHAR(100) NOT NULL
);

-- Sample data
INSERT INTO worker (first_name, last_name, date_of_birth, role) VALUES
    ('Amina',   'Trabelsi', '1990-04-12', 'Software Engineer'),
    ('Karim',   'Ben Salah','1985-11-02', 'Project Manager'),
    ('Sofia',   'Gharbi',   '1993-07-25', 'HR Specialist'),
    ('Youssef', 'Cherif',   '1988-01-30', 'Accountant');

-- Insert a login user manually after generating a password hash with
-- GenerateHashTool (see README.md), e.g.:
-- INSERT INTO app_user (username, password_hash) VALUES ('admin', '<paste-hash-here>');
--
-- Roles: 'ADMIN' (all service operations) or 'VIEWER' (read-only operations).
-- Example:
--   UPDATE app_user SET role='ADMIN' WHERE username='admin';
--   INSERT INTO app_user (username, password_hash, role)
--   VALUES ('viewer', '<paste-hash-here>', 'VIEWER');
