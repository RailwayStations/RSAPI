CREATE SEQUENCE users_seq;

CREATE TABLE users (
  id INTEGER DEFAULT nextval('users_seq') PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT,
  url TEXT,
  ownPhotos BOOLEAN NOT NULL,
  anonymous BOOLEAN NOT NULL,
  license TEXT,
  "key" TEXT,
  admin BOOLEAN NOT NULL,
  emailVerification TEXT,
  sendNotifications BOOLEAN DEFAULT true,
  locale TEXT,
  UNIQUE (name),
  UNIQUE (email)
);

CREATE SEQUENCE blocked_usernames_seq;

CREATE TABLE blocked_usernames (
  id INTEGER DEFAULT nextval('blocked_usernames_seq') PRIMARY KEY,
  name TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (name)
);

CREATE TABLE countries (
  id TEXT NOT NULL PRIMARY KEY,
  name TEXT NOT NULL,
  timetableUrlTemplate TEXT,
  email TEXT,
  overrideLicense TEXT,
  active BOOLEAN DEFAULT true
);

CREATE TABLE providerApps (
  countryCode TEXT NOT NULL,
  type TEXT NOT NULL,
  name TEXT NOT NULL,
  url TEXT NOT NULL,
  PRIMARY KEY (countryCode,type,name),
  CONSTRAINT fk_country FOREIGN KEY (countryCode) REFERENCES countries (id)
);

CREATE SEQUENCE inbox_seq;

CREATE TABLE inbox (
  id INTEGER DEFAULT nextval('inbox_seq') PRIMARY KEY,
  photographerId INTEGER NOT NULL,
  countryCode TEXT,
  stationId TEXT,
  title TEXT,
  lat DOUBLE PRECISION,
  lon DOUBLE PRECISION,
  extension TEXT,
  comment TEXT,
  rejectReason TEXT,
  done BOOLEAN DEFAULT false,
  problemReportType TEXT,
  active BOOLEAN,
  crc32 TEXT,
  notified BOOLEAN DEFAULT false,
  createdAt TIMESTAMP NOT NULL,
  photoId INTEGER,
  posted BOOLEAN DEFAULT false,
  CONSTRAINT fk_inbox_user FOREIGN KEY (photographerId) REFERENCES users (id)
);
CREATE INDEX fk_inbox_user ON inbox(photographerId);
CREATE INDEX idx_station ON inbox(countryCode,stationId);

CREATE TABLE oauth2_authorization (
  id TEXT NOT NULL PRIMARY KEY,
  registered_client_id TEXT NOT NULL,
  principal_name TEXT NOT NULL,
  authorization_grant_type TEXT NOT NULL,
  authorized_scopes TEXT,
  attributes text,
  state TEXT,
  authorization_code_value text,
  authorization_code_issued_at TIMESTAMP NULL,
  authorization_code_expires_at TIMESTAMP NULL,
  authorization_code_metadata text,
  access_token_value text,
  access_token_issued_at TIMESTAMP NULL,
  access_token_expires_at TIMESTAMP NULL,
  access_token_metadata text,
  access_token_type TEXT,
  access_token_scopes TEXT,
  oidc_id_token_value text,
  oidc_id_token_issued_at TIMESTAMP NULL,
  oidc_id_token_expires_at TIMESTAMP NULL,
  oidc_id_token_metadata text,
  refresh_token_value text,
  refresh_token_issued_at TIMESTAMP NULL,
  refresh_token_expires_at TIMESTAMP NULL,
  refresh_token_metadata text,
  user_code_value text,
  user_code_issued_at TIMESTAMP NULL,
  user_code_expires_at TIMESTAMP NULL,
  user_code_metadata text,
  device_code_value text,
  device_code_issued_at TIMESTAMP NULL,
  device_code_expires_at TIMESTAMP NULL,
  device_code_metadata text
);

CREATE TABLE oauth2_authorization_consent (
  registered_client_id TEXT NOT NULL,
  principal_name TEXT NOT NULL,
  authorities TEXT NOT NULL,
  PRIMARY KEY (registered_client_id,principal_name)
);

CREATE TABLE oauth2_registered_client (
  id TEXT NOT NULL PRIMARY KEY,
  client_id TEXT NOT NULL,
  client_id_issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  client_secret TEXT,
  client_secret_expires_at TIMESTAMP NULL,
  client_name TEXT NOT NULL,
  client_authentication_methods TEXT NOT NULL,
  authorization_grant_types TEXT NOT NULL,
  redirect_uris TEXT,
  scopes TEXT NOT NULL,
  client_settings TEXT NOT NULL,
  token_settings TEXT NOT NULL,
  post_logout_redirect_uris TEXT
);

CREATE TABLE stations (
  countryCode TEXT NOT NULL,
  id TEXT NOT NULL,
  uicibnr INTEGER,
  dbibnr INTEGER,
  DS100 TEXT,
  title TEXT NOT NULL,
  lat DOUBLE PRECISION NOT NULL,
  lon DOUBLE PRECISION NOT NULL,
  eva_id INTEGER,
  active BOOLEAN DEFAULT true,
  PRIMARY KEY (countryCode,id)
);

CREATE SEQUENCE photos_seq;

CREATE TABLE photos (
  id INTEGER DEFAULT nextval('photos_seq') PRIMARY KEY,
  countryCode TEXT NOT NULL,
  stationId TEXT NOT NULL,
  "primary" BOOLEAN NOT NULL DEFAULT true,
  outdated BOOLEAN NOT NULL DEFAULT false,
  urlPath TEXT NOT NULL,
  license TEXT NOT NULL,
  photographerId INTEGER NOT NULL,
  createdAt TIMESTAMP NOT NULL,
  CONSTRAINT fk_photo_station FOREIGN KEY (countryCode, stationId) REFERENCES stations (countryCode, id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_photo_user FOREIGN KEY (photographerId) REFERENCES users (id)
);
CREATE INDEX fk_photo_user ON photos(photographerId);
CREATE INDEX fk_photo_station ON photos(countryCode,stationId);
