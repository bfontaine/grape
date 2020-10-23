## Roadmap to 1.0.0

- [ ] Add unit tests for everything in `grape.cli`

### CLI

- [x] make it faster with GraalVM
- [x] Support `-v`/`--version`
- [x] Remove `-r`/`--recursive` and always assume it’s recursive
- [ ] `ack`-like output by default: show line numbers and filenames (if run on multiple files). Add options to change
      that behavior
- [ ] Maybe add an option to change how whitespaces are shown – original vs. not indented vs. inline.
- [x] `-c` or similar to get the count since piping to `wc -l` doesn’t work

### Build

- [x] automate `lein deploy clojars` with a GitHub workflow
- [x] automate the Linux binary build