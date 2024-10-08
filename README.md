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

The API can be started via IntelliJ. A local Maria DB can be started via `docker compose up -d` locally.

The API is then available via `http://localhost:8080`.

## Maria DB

For local testing and debugging you can enter mariadb via CLI:

`docker exec -it mariadb mysql -ursapi -prsapi rsapi --default-character-set=utf8mb4`

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
