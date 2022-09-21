# TODOs and ideas

- reduce amount of integration tests

- package structure:

    - app vs. application vs. core

    - core/model vs. domain

- full (incoming) model validation

- introduce Repository classes to hide JDBI DAOs?

- reintroduce multistage docker build

- harden docker

    - user

    - readonly filesystem

- Parameter Object for `MastodonBot.tootNewPhoto`?

- InboxEntry: change `@Data` to `@Value` annotation

- Is there a better way for Country.providerApps and Station.photos?

- Replace `/<country>/*` endpoints with `/*?country=<country>` parameter, see [api.log](api.log)

- Use generated openApi DTOs as source directly

```
sourceSets.main.java.srcDirs files("${buildDir}/openapi/src/main/java").builtBy('openApiGenerate')
compileJava.dependsOn tasks.openApiGenerate
```

- Generate Dtos with lombok annotations:

```
    configOptions = [
            ...

            additionalModelTypeAnnotations:
                    "@lombok.Builder;" +
                            "@lombok.AllArgsConstructor;" +
                            "@lombok.Data;" +
                            "@lombok.NoArgsConstructor"
    ]
```

## Multiple photos per station

- Support photoId for problem reports related to a photo

- New Station API to support multiple photos per station

    - 4 new endpoints needed:

        - one station with all photos by `country` and `id`

          ~~`/stations2/{country}/{id}`~~

          ~~`/stationPhotos/{country}/{id}`~~

          ~~`/stationsByCountry/{country}/{id}`~~

          ~~`/stationById/{country}/{id}`~~

          `/photoStationById/{country}/{id}` ✅

        - all stations of a `country` with the primary photo (optional with filter of `hasPhoto` and `active`)

          ~~`/stations2/{country}?hasPhoto=&active=`~~

          ~~`/countryStations/{country}?hasPhoto=&active=`~~

          ~~`/stationsByCountry/{country}?hasPhoto=&active=`~~

          `/photoStationsByCountry/{country}?hasPhoto=&isActive=`

        - all stations with photos of one `photographer` (optional with filter by `country`)

          ~~`/photographers/{photographer}/photos?country={country}`~~

          ~~`/stationPhotosByUser/{photographer}?country={country}`~~

          ~~`/stationsByPhotographer/{photographer}?country={country}`~~

          `/photoStationsByPhotographer/{photographer}?country={country}`

        - all stations with a photo which was recently imported, with filter by `importedSince` (max one month)

          ~~`/stationPhotos?importedSince={importedSince}`~~

          ~~`/stationsWithPhotosImportedSince?importedSince={importedSince}`~~

          ~~`/stationsByRecentPhotoImports?importedSince={importedSince}`~~

          `/photoStationsByRecentPhotoImports?importedSince={importedSince}`

    - **Or** one for all:

      ~~`/stations2?country=&id=&photographer=&importedSince&hasPhoto=&active=`~~

      valid combinations:

        - `country`, `id`

        - `country`, (`hasPhoto`), (`active`)

        - `photographer`, (`country`)

        - `createdSince`
