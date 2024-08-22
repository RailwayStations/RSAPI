ALTER TABLE users DROP COLUMN normalizedName;
ALTER TABLE blocked_usernames RENAME COLUMN normalizedName TO name;