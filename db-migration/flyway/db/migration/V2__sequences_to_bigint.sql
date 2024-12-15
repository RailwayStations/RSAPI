ALTER TABLE users ALTER COLUMN id TYPE bigint;

ALTER TABLE blocked_usernames ALTER COLUMN id TYPE bigint;

ALTER TABLE inbox ALTER COLUMN id TYPE bigint;
ALTER TABLE inbox ALTER COLUMN photoId TYPE bigint;
ALTER TABLE inbox ALTER COLUMN photographerId TYPE bigint;

ALTER TABLE photos ALTER COLUMN id TYPE bigint;
ALTER TABLE photos ALTER COLUMN photographerId TYPE bigint;
