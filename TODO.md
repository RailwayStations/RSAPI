# TODOs and ideas

- reduce amount of integration tests

- test outgoing http adapters with Wiremock
  - MastodonBotHttpClient
  - MatrixMonitor
  - WebDavSyncTask

- introduce Lombok or Java records where useful

- package structure:
  - app vs. application vs. core
  - core/mode vs. domain

- full (incoming) model validation

- introduce Repository classes to hide JDBI DAOs?

- reintroduce multistage docker build

- harden docker
  - user
  - readonly filesystem
