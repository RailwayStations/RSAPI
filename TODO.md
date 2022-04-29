# TODOs and ideas

- reduce amount of integration tests

- introduce Lombok or Java records where useful

- separate outgoing Station with a DTO
  - use OpenApi Generator
    - issue: DS100 field duplication in json: https://github.com/OpenAPITools/openapi-generator/issues/5705

- package structure:
  - app vs. application vs. core
  - core/mode vs. domain

- full (incoming) model validation

- introduce Repository classes to hide JDBI DAOs?

- reintroduce multistage docker build

- harden docker
  - user
  - readonly filesystem
