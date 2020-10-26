## Contributing to `grape`

### Development

Development happens on the `devel` branch.

### Releasing a version

1. Change the version in [`project.clj`](./project.clj)
2. Change the version in [`resources/GRAPE_VERSION`](./resources/GRAPE_VERSION)
3. Update the [`CHANGELOG.md`](./CHANGELOG.md)
4. Commit and tag the release
5. Merge `devel` into `master`, and push (including tags)
6. Check the CI job "Release": it should build the standalone jar and a Linux binary, and add them to a new draft
   release
7. Build the macOS binary by yourself (The CI step to install GraalVM doesn’t support macOS), and add it to the draft
   release
8. On `devel`, bump the version to the next snapshot
9. Commit and push

### Building the standalone jar

    lein do clean, uberjar

### Building a standalone binary

You need GraalVM for Java 11. See [clj-kondo’s guide][ckg] to install it.

[Build the standalone jar](#building-the-standalone-jar), then run `bin/compile.sh`.
If everything’s fine, it should generate a top-level `grape` standalone binary.

To add a standalone binary to the release, zip it using the version and OS/architecture name, e.g.:

    $ zip grape-macos-amd64.zip grape

This reduces the size by ~70%.

[ckg]: https://github.com/borkdude/clj-kondo/blob/e62eb04bc8bdb754a368ca8e7b0e76d8d568253e/doc/build.md#building-from-source