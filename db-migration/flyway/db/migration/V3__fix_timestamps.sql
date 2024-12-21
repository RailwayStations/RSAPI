ALTER TABLE inbox ALTER createdAt TYPE timestamptz USING createdAt AT TIME ZONE 'UTC';
ALTER TABLE blocked_usernames ALTER created_at TYPE timestamptz USING created_at AT TIME ZONE 'UTC';
ALTER TABLE photos ALTER createdAt TYPE timestamptz USING createdAt::timestamp at time zone 'UTC';
