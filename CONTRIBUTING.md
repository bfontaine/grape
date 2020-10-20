## Contributing to `grape`

### Cutting a version

1. Change the version in [`project.clj`](./project.clj)
2. Change the version in [`resources/GRAPE_VERSION`](./resources/GRAPE_VERSION)
3. Update the [`CHANGELOG.md`](./CHANGELOG.md)

#### Deploy to Clojars

    lein deploy

#### Building the jar

    lein do clean, uberjar

#### Building a standalone binary

You need GraalVM for Java 11. See [clj-kondo’s guide][ckg] to install it.

Then run `bin/compile.sh`. If everything’s fine, it should generate a top-level `grape` standalone binary.

[ckg]: https://github.com/borkdude/clj-kondo/blob/e62eb04bc8bdb754a368ca8e7b0e76d8d568253e/doc/build.md#building-from-source