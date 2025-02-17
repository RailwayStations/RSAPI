ALTER TABLE country ALTER COLUMN active SET NOT NULL;
ALTER TABLE country ALTER COLUMN active DROP DEFAULT;

ALTER TABLE blocked_username ALTER COLUMN name SET NOT NULL;
ALTER TABLE blocked_username ALTER COLUMN name DROP DEFAULT;

ALTER TABLE blocked_username ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE blocked_username ALTER COLUMN created_at DROP DEFAULT;

ALTER TABLE "user" ALTER COLUMN sendnotifications SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN sendnotifications DROP DEFAULT;

UPDATE "user" SET admin = false WHERE admin IS NULL;
ALTER TABLE "user" ALTER COLUMN admin SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN admin DROP DEFAULT;

UPDATE "user" SET locale = 'en' WHERE locale IS NULL;
ALTER TABLE "user" ALTER COLUMN locale SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN locale DROP DEFAULT;

ALTER TABLE "user" ALTER COLUMN ownphotos SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN ownphotos DROP DEFAULT;

ALTER TABLE "user" ALTER COLUMN anonymous SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN anonymous DROP DEFAULT;

UPDATE "user" SET license = 'UNKNOWN' WHERE license IS NULL;
ALTER TABLE "user" ALTER COLUMN license SET NOT NULL;
ALTER TABLE "user" ALTER COLUMN license DROP DEFAULT;
