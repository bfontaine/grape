## Roadmap to 1.0.0

- [ ] ~~Maybe change `$&` to `$@`, which I find easier to remember~~: not feasible; `$@` is not a valid symbol and is
      parsed as `$ deref` by Parcera.

### CLI

- [x] make it faster with GraalVM
- [x] Support `-v`/`--version`
- [x] Remove `-r`/`--recursive` and always assume it’s recursive
- [ ] `ack`-like output by default: show line numbers and filenames (if run on multiple files). Add options to change
      that behavior (also maybe add an option to change how whitespaces are shown – original vs. not indented vs. inline).
- [ ] `-c` or similar to get the count since piping to `wc -l` doesn’t work

### Build

- [ ] automate `lein deploy clojars` with a GitHub workflow
- [ ] automate the various binaries builds (see how clj-kondo does it, [here][1] and [here][2])

[1]: https://github.com/borkdude/clj-kondo/blob/15ce36ad616bbc4a86a256719f37145c76372e38/.circleci/config.yml
[2]: https://github.com/borkdude/clj-kondo/blob/e62eb04bc8bdb754a368ca8e7b0e76d8d568253e/.circleci/script/release