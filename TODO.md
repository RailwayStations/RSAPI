# TODOs and ideas

- reduce amount of integration tests

- restructure testdata database migrations vs. TestFixtures

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
