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

- Replace `/<country>/*` endpoints with `/*?country=<country>` parameter

  See [api.log](api.log)

## Multiple photos per station

- Support photoId for problem reports related to a photo

- New Station API to support multiple photos per station

  - 4 new endpoints needed:

    - one station with all photos by `country` and `id`

      `/stations2/{country}/{id}`

      `/stationPhotos/{country}/{id}`

    - all stations of a `country` with the primary photo (optional with filter of `hasPhoto` and `isActive`)

      `/stations2/{country}?hasPhoto=&isActive=`

      `/countryStations/{country}?hasPhoto=&isActive=`

      `/stationsByCountry/{country}?hasPhoto=&isActive=`

    - all stations with photos of one `photographer` (optional with filter by `country`)

      `/photographers/{name}/photos`

      `/stationPhotosByUser/{photographer}`

    - all stations with a photo which was recently imported, with filter by `importedSince` (max one month)

      `/stationPhotos?importedSince={importedSince}?`

      `/stationsWithPhotosImportedSince?importedSince={importedSince}?`

  - **Or** one for all:

    `/stations2?country=&id=&photographer=&importedSince&hasPhoto=&isActive=`

    valid combinations:

      - `country`, `id`

      - `country`, (`hasPhoto`), (`isActive`)

      - `photographer`, (`country`)

      - `createdSince`
