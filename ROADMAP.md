## Roadmap to 1.0.0

- [ ] Prepend the output with line numbers
- [ ] Add an option not to show them
- [ ] Add an option to show only the first line (easier copy/pasting)
- [ ] Maybe add an option to change how whitespaces are shown – original vs. not indented vs. inline.
- [ ] Maybe add an option to respect the (non-)indentation while showing lines, e.g.:

      src/myfile.clj:123:
        (map inc xs)
      src/myfile.clj:127:
        (map dec xs)
      src/myfile.clj:253:
        (map identity xs)
      sec/myfile2.clj:102:
        (map f xs)

- [ ] Add unit tests for everything in `grape.cli`