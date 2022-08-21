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

- Parameter Object for MastodonBot.tootNewPhoto?

- InboxEntry: change @Data to @Value annotation

- Replace `/<country>/*` endpoints with `/*?country=<country>` parameter
  See [api.log](api.log)

- Multiple Photos per Station
  - Support photoId for problem reports related to a photo
  - New Station API to support multiple photos per station 