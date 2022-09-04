[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b9882fcf1221409680f36afe2c85fcba)](https://www.codacy.com/gh/RailwayStations/RSAPI?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=RailwayStations/RSAPI&amp;utm_campaign=Badge_Grade) [![Coverage Status](https://coveralls.io/repos/github/RailwayStations/RSAPI/badge.svg?branch=master)](https://coveralls.io/github/RailwayStations/RSAPI?branch=master) 

# Railway Stations API
Backend Service for the https://map.railway-stations.org website and the Mobile Apps for [Android](https://github.com/RailwayStations/RSAndroidApp) and [iOS](https://github.com/RailwayStations/Bahnhofsfotos) of the [Bahnhofsfotos opendata Project](https://github.com/RailwayStations).

This API is hosted at https://api.railway-stations.org or at the Deutsche Bahn developer site: https://developer.deutschebahn.com/store/apis/list where you can also find an online and executable version of the OpenAPI documentation.

## build
To build the project, you need Java 17.

Run on Unix like systems:
```./gradlew build bootJar```

Run on Windows:

```./gradlew.bat build bootJar```

Build docker image:

```docker build . -t railwaystations/rsapi:latest```

## Working Directory

The API uses `/var/rsapi` as working directory. This can be changed in the `application.yaml` or via Docker volume, see below.

The following subdirectories are being used:

- `photos`
  - `<countryCode>`: photos for the country identified by the country code
- `inbox`: all uploads are collected here
  - `toprocess`: uploaded photos are sent to VsionAI for image processing
  - `processed`: processed photos from VsionAI
  - `done`: imported (unprocessed) photos
  - `<countryCode>/import`: old import directories for batch imports

## Run

The API can be started via `docker compose up -d` locally. It starts two Docker container: a local Maria DB and the API. The API is then available via `http://localhost:8080`.

## Maria DB

For local testing and debugging purpose you can start the MariaDB container standalone with `docker compose up mariadb -d`.

Enter mariadb CLI:
`docker exec -it mariadb mysql -ursapi -prsapi rsapi --default-character-set=utf8mb4`

## Useage

Point your browser to `http://localhost:8080/{country}/stations`, where `country` can be "de", "ch", "fi", "uk", ...

With the following query parameter:
- `hasPhoto`: boolean, indicates if only trainstations with or without a photo should be selected
- `photographer`: (nick)name of a photographer, select only trainstations with photos from her
- `country`: select trainstations from a given country, this parameter is an alternative to the `{country}` path

A more detailed API documentation can be found in the [OpenAPI](src/main/resources/static/openapi.yaml) file or online at [developer.deutschebahn.com](https://developer.deutschebahn.com/store/apis/list).

### Examples
- all supported countries: https://api.railway-stations.org/countries
- all german trainstations: https://api.railway-stations.org/stations?country=de
- german trainstations without photo: https://api.railway-stations.org/stations?country=de&hasPhoto=false
- austrian trainsations from photographer @pokipsie: https://api.railway-stations.org/stations?country=ch&photographer=@pokipsie
- all photographers with count of photos: https://api.railway-stations.org/photographers
- german photographers: https://api.railway-stations.org/photographers?country=de
- statistic per country (de): https://api.railway-stations.org/stats?country=de
