CREATE TABLE `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) DEFAULT NULL,
  `normalizedName` varchar(50) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `ownPhotos` bit(1) DEFAULT NULL,
  `anonymous` bit(1) DEFAULT NULL,
  `license` varchar(50) DEFAULT NULL,
  `key` varchar(1024) DEFAULT NULL,
  `admin` bit(1) DEFAULT NULL,
  `emailVerification` varchar(100) DEFAULT NULL,
  `sendNotifications` bit(1) DEFAULT b'1',
  `locale` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`),
  UNIQUE KEY `normalizedName` (`normalizedName`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=1238 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `blocked_usernames` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `normalizedName` varchar(30) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `normalizedName` (`normalizedName`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `countries` (
  `id` char(2) NOT NULL,
  `name` varchar(255) NOT NULL,
  `timetableUrlTemplate` varchar(1024) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `overrideLicense` varchar(100) DEFAULT NULL,
  `active` bit(1) DEFAULT b'1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `providerApps` (
  `countryCode` char(2) NOT NULL,
  `type` varchar(10) NOT NULL,
  `name` varchar(100) NOT NULL,
  `url` varchar(255) NOT NULL,
  PRIMARY KEY (`countryCode`,`type`,`name`),
  CONSTRAINT `fk_country` FOREIGN KEY (`countryCode`) REFERENCES `countries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `inbox` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `photographerId` int(11) NOT NULL,
  `countryCode` char(2) DEFAULT NULL,
  `stationId` varchar(30) DEFAULT NULL,
  `title` varchar(100) DEFAULT NULL,
  `lat` double DEFAULT NULL,
  `lon` double DEFAULT NULL,
  `extension` char(3) DEFAULT NULL,
  `comment` varchar(1024) DEFAULT NULL,
  `rejectReason` varchar(1024) DEFAULT NULL,
  `done` bit(1) DEFAULT b'0',
  `problemReportType` varchar(30) DEFAULT NULL,
  `active` bit(1) DEFAULT NULL,
  `crc32` mediumtext DEFAULT NULL,
  `notified` bit(1) DEFAULT b'0',
  `createdAt` datetime DEFAULT NULL,
  `photoId` bigint(20) DEFAULT NULL,
  `posted` bit(1) DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `fk_inbox_user` (`photographerId`),
  KEY `idx_station` (`countryCode`,`stationId`),
  CONSTRAINT `fk_inbox_user` FOREIGN KEY (`photographerId`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16433 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `oauth2_authorization` (
  `id` varchar(100) NOT NULL,
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorization_grant_type` varchar(100) NOT NULL,
  `authorized_scopes` varchar(1000) DEFAULT NULL,
  `attributes` text DEFAULT NULL,
  `state` varchar(500) DEFAULT NULL,
  `authorization_code_value` text DEFAULT NULL,
  `authorization_code_issued_at` timestamp NULL DEFAULT NULL,
  `authorization_code_expires_at` timestamp NULL DEFAULT NULL,
  `authorization_code_metadata` text DEFAULT NULL,
  `access_token_value` text DEFAULT NULL,
  `access_token_issued_at` timestamp NULL DEFAULT NULL,
  `access_token_expires_at` timestamp NULL DEFAULT NULL,
  `access_token_metadata` text DEFAULT NULL,
  `access_token_type` varchar(100) DEFAULT NULL,
  `access_token_scopes` varchar(1000) DEFAULT NULL,
  `oidc_id_token_value` text DEFAULT NULL,
  `oidc_id_token_issued_at` timestamp NULL DEFAULT NULL,
  `oidc_id_token_expires_at` timestamp NULL DEFAULT NULL,
  `oidc_id_token_metadata` text DEFAULT NULL,
  `refresh_token_value` text DEFAULT NULL,
  `refresh_token_issued_at` timestamp NULL DEFAULT NULL,
  `refresh_token_expires_at` timestamp NULL DEFAULT NULL,
  `refresh_token_metadata` text DEFAULT NULL,
  `user_code_value` text DEFAULT NULL,
  `user_code_issued_at` timestamp NULL DEFAULT NULL,
  `user_code_expires_at` timestamp NULL DEFAULT NULL,
  `user_code_metadata` text DEFAULT NULL,
  `device_code_value` text DEFAULT NULL,
  `device_code_issued_at` timestamp NULL DEFAULT NULL,
  `device_code_expires_at` timestamp NULL DEFAULT NULL,
  `device_code_metadata` text DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `oauth2_authorization_consent` (
  `registered_client_id` varchar(100) NOT NULL,
  `principal_name` varchar(200) NOT NULL,
  `authorities` varchar(1000) NOT NULL,
  PRIMARY KEY (`registered_client_id`,`principal_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `oauth2_registered_client` (
  `id` varchar(100) NOT NULL,
  `client_id` varchar(100) NOT NULL,
  `client_id_issued_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `client_secret` varchar(1000) DEFAULT NULL,
  `client_secret_expires_at` timestamp NULL DEFAULT NULL,
  `client_name` varchar(200) NOT NULL,
  `client_authentication_methods` varchar(1000) NOT NULL,
  `authorization_grant_types` varchar(1000) NOT NULL,
  `redirect_uris` varchar(1000) DEFAULT NULL,
  `scopes` varchar(1000) NOT NULL,
  `client_settings` varchar(2000) NOT NULL,
  `token_settings` varchar(2000) NOT NULL,
  `post_logout_redirect_uris` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `stations` (
  `countryCode` char(2) NOT NULL,
  `id` varchar(30) NOT NULL,
  `uicibnr` int(11) DEFAULT NULL,
  `dbibnr` int(11) DEFAULT NULL,
  `DS100` varchar(30) DEFAULT NULL,
  `title` varchar(100) NOT NULL,
  `lat` double NOT NULL,
  `lon` double NOT NULL,
  `eva_id` int(11) DEFAULT NULL,
  `active` bit(1) DEFAULT b'1',
  PRIMARY KEY (`countryCode`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `photos` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `countryCode` char(2) NOT NULL,
  `stationId` varchar(30) NOT NULL,
  `primary` bit(1) NOT NULL DEFAULT b'1',
  `outdated` bit(1) NOT NULL DEFAULT b'0',
  `urlPath` varchar(255) NOT NULL,
  `license` varchar(100) NOT NULL,
  `photographerId` int(11) NOT NULL,
  `createdAt` mediumtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_photo_user` (`photographerId`),
  KEY `fk_photo_station` (`countryCode`,`stationId`),
  CONSTRAINT `fk_photo_station` FOREIGN KEY (`countryCode`, `stationId`) REFERENCES `stations` (`countryCode`, `id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_photo_user` FOREIGN KEY (`photographerId`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22109 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

