# Railway Stations API

Backend Service for the https://map.railway-stations.org website and the Mobile Apps
for [Android](https://github.com/RailwayStations/RSAndroidApp)
and [iOS](https://github.com/RailwayStations/Bahnhofsfotos) of
the [Bahnhofsfotos opendata Project](https://github.com/RailwayStations).

This API is hosted at https://api.railway-stations.org.

## build

To build the project, you need Java 21.

Run on Unix like systems:

```./gradlew :service:bootJar```

Run on Windows:

```./gradlew.bat :service:bootJar```

Build docker image:

```docker build . -t railwaystations/rsapi:latest```

## IntelliJ

After importing this project for the first time, you might need to specify the folder `service/build/openapi` as
source
root folder.

## Working Directory

The API uses `/var/rsapi` as working directory. This can be changed in the `application.yaml` or via Docker volume, see
below.

The following subdirectories are being used:

- `photos`

    - `{country}`: photos for the country identified by the country code

- `inbox`: all uploads are collected here

    - `toprocess`: uploaded photos are sent to VsionAI for image processing

    - `processed`: processed photos from VsionAI

    - `done`: imported (unprocessed) photos

    - `rejected`: rejected photos

## Run

The API can be started via IntelliJ. A local PostgreSQL can be started via `docker compose up -d` locally.

The API is then available via `http://localhost:8080`.

## Usage

Point your browser to `http://localhost:8080/photoStationsByCountry/de`, where the last path segment is the `country`
selector and can be "de", "ch", "fi", "uk", ...

With the following query parameter:

- `hasPhoto`: boolean, indicates if only railwaystations with or without a photo should be selected

- `isActive`: select only railwaystations which are active

A more detailed API documentation can be found in the [OpenAPI](openapi/src/main/resources/static/openapi.yaml) file or
online
at [developer.deutschebahn.com](https://developer.deutschebahn.com/store/apis/list).

### Examples

- all supported countries: https://api.railway-stations.org/countries

- all german railwaystations: https://api.railway-stations.org/photoStationsByCountry/de

- german railwaystations without photo: https://api.railway-stations.org/photoStationsByCountry/de?hasPhoto=false

- one railwaystation with all its photos: https://api.railway-stations.org/photoStationById/de/1973

- austrian trainsations from photographer @pokipsie:
  https://api.railway-stations.org/photoStationsByPhotographer/@pokipsie?country=ch

- all photographers with count of photos: https://api.railway-stations.org/photographers

- german photographers: https://api.railway-stations.org/photographers?country=de

- statistic per country (de): https://api.railway-stations.org/stats?country=de

## History

Some historic milestones of the app.

### 11./12.12.2015

Project founded on the
[3rd DB-Hackathon](https://www.eventbrite.de/e/3rd-dbhackathon-commit-open-data-tickets-19270040209)
as "Deutschlands Bahnhöfe".

Railway-station data of germany where imported into an Elasticsearch DB.
Photos could be sent via eMail or a Dropbox folder. Photos were manually imported into a Drupal CMS.

### March 2016

Start as a Dropwizard service to provide the railway-station data from the Elasticsearch database as GPX files:
https://github.com/RailwayStations/RSAPI/commit/f8dcefd950e776266b4463d0d414599a23f17d94

### May 2016

Add Json and text endpoints of the railway-station data:
https://github.com/RailwayStations/RSAPI/commit/f102eb475e8bc29751cfb85e2773376c088817d2

### August 2016

The app serves the railway-station data as Json for the Android app.

### May 2017

Photos could be uploaded via API Endpoint:
https://github.com/RailwayStations/RSAPI/commit/a6171ce5a420fce11b6975a41397ab92e3cb266a

### April 2018

Replacement of the Drupal CMS. Mass import of photos from filesystem via Slack command:
https://github.com/RailwayStations/RSAPI/commit/d7b2baf48a39911b1913706b8e0b9d6f6dcf958a

### November 2018

Migration from Elasticsearch to Mariadb:
https://github.com/RailwayStations/RSAPI/commit/12519f916ba31447b002b63d24e01a31c3821207

Account registration via API Endpoint.

### January 2020

Admin Endpoints to import or reject uploaded photos:
https://github.com/RailwayStations/RSAPI/commit/d0fea732d24b0362f10f0992e63e301c42a0d04c

### December 2021

Migration from Dropwizard to Springboot:
https://github.com/RailwayStations/RSAPI/commit/a327d815dec4c31965ebd4d4e8a805ea8fa7dfb0

Included Maven to Gradle migration.

### February 2022

Refactor towards hexagonal architecture:
https://github.com/RailwayStations/RSAPI/commit/3e01a36c1b9e3f9bba4e60ec648211ba07c1ac09

### April 2022

Added OpenApi generation:
https://github.com/RailwayStations/RSAPI/commit/4fe7b29495ffa4eed4422102815ed312b7e5c73d

### February 2023

Spring authorization server integration:
https://github.com/RailwayStations/RSAPI/commit/24e1ffdc28db75f0cea5db3c26e915957321fba4

Apps and webclient can now authorize users via OAuth2.

### February 2024

Java to Kotlin migration:
https://github.com/RailwayStations/RSAPI/commit/a55a5ab02a3dd47f2bb14d3cdce5d55a0ef361f9

### May 2024

Liquibase to Flyway migration:
https://github.com/RailwayStations/RSAPI/commit/c713f70c564c4d764614c39cdf2815f98abfa917

Split into multi modules:
https://github.com/RailwayStations/RSAPI/commit/7c94f5464fe3fe21cba3eefe58a73e98c5d1ab68

### November 2024

Migration from MariaDB 10.3 to PostgreSQL 17
