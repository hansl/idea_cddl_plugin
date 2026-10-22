# CDDL support for IntelliJ IDEA

An IntelliJ Platform plugin for [CDDL](https://www.rfc-editor.org/rfc/rfc8610) (Concise Data Definition Language,
RFC 8610 as updated by RFC 9682), the schema language for CBOR and JSON data.

## Features

- **Highlighting**: lexical highlighting plus semantic colors for type and group rules, references, prelude types,
  generic parameters, member keys, control operators, tags and string escapes. All colors are configurable under
  *Settings | Editor | Color Scheme | CDDL*.
- **Completion**: rule names, generic parameters, imported names, prelude types and names from other CDDL files;
  generic rules insert `<>`. Control operators complete after typing ` .`. Typing a new rule name suggests
  names that are used but not yet defined.
- **Navigation**: Go to Declaration (including into the standard prelude), Find Usages, Go to Symbol, gutter icons
  linking a rule to its `/=` and `//=` extensions, structure view, breadcrumbs, and navigation from
  `;# import` / `;# include` directives to the module file.
- **Refactoring**: in-place rename of rules and generic parameters; renaming a rule renames all its definitions and
  keeps namespace prefixes of imported names (`cose.label`). Prelude rules cannot be renamed.
- **Inspections** (*Settings | Editor | Inspections | CDDL*):
  - unresolved references, with *Create rule* and *Insert space before control operator* (`tstr.size` is a
    single name) quick fixes; undefined sockets (`$name`, `$$name`) are legal and not reported;
  - unused rules, with a *Remove rule* quick fix (the root rule and sockets are exempt);
  - names defined twice with `=`, and `/=` / `//=` used on the wrong kind of rule;
  - unknown control operators (anything not in the IANA registry);
  - generic rules used with the wrong number of arguments;
  - invalid string escapes (RFC 9682) and unterminated strings.
- **Quick documentation** for rules (definition, extensions, and the comment above them), prelude types, control
  operators, tags and major types (`#6.32`, `#7.25`, ...).
- **Regular expressions**: strings controlled by `.regexp` (XML Schema dialect) and `.pcre` get RegExp
  highlighting, validation and editing support.
- **Editing**: formatter with code style settings, folding, brace matching and auto-closing, line commenting with
  `;`, quote handling, spell checking, and a *File | New | CDDL File* action.

### Name resolution

A name resolves, in order, to: a generic parameter of the enclosing rule; rules of the same file; rules made visible
by module directives (`;# import rfc9052 as cose`, `;# include a, b from lib`; see
[draft-ietf-cbor-cddl-modules](https://datatracker.ietf.org/doc/draft-ietf-cbor-cddl-modules/)), looked up as
`<module>.cddl` next to the file or anywhere in the project; the standard prelude; and finally rules of the other CDDL
files of the project, for files that do not use directives (specifications are often split into files that are
concatenated by tooling).

## Development

Requirements: a JDK 17+ to run Gradle. The plugin targets IntelliJ IDEA 2026.2 (build 262), which needs Java 25; the
Gradle toolchain is provisioned automatically.

| Command                   | What it does                                                    |
|---------------------------|-----------------------------------------------------------------|
| `./gradlew runIde`        | Starts a sandbox IDE with the plugin installed                  |
| `./gradlew test`          | Runs the tests                                                  |
| `./gradlew buildPlugin`   | Builds `build/distributions/idea-cddl-<version>.zip`            |
| `./gradlew verifyPlugin`  | Runs the IntelliJ Plugin Verifier against the targeted IDE      |

To install a build, use *Settings | Plugins | ⚙ | Install Plugin from Disk* with the ZIP file.

The lexer and parser are generated at build time from `src/main/grammar/Cddl.flex` (JFlex) and
`src/main/grammar/Cddl.bnf` (Grammar-Kit). The grammar's comments explain where it deviates from the RFC's ABNF to
suit a PEG parser and error recovery.

### Testing against real specifications

Two opt-in tests parse, highlight and reformat every `.cddl` file below a directory, failing on syntax errors,
exceptions, or a reformatting that changes anything but whitespace:

```shell
CDDL_CORPUS=/path/to/specs ./gradlew test --tests '*Corpus*'
```

[cddlc](https://github.com/cabo/cddlc) (`data/`) and [cddl-rs](https://github.com/anweiss/cddl) (`tests/fixtures/`)
are good sources of real-world CDDL.

### Layout

```
src/main/grammar/            JFlex lexer and Grammar-Kit parser definitions
src/main/kotlin/.../cddl/
  psi/                       PSI helpers, mixins for generated PSI, element factory
  resolve/                   references, name resolution, prelude, rule index, module directives
  highlighting/              syntax highlighter, annotator, color settings page
  completion/                completion contributor
  inspections/               inspections and quick fixes
  documentation/             quick documentation
  formatting/                formatter and code style settings
  navigation/                Go to Symbol, gutter icons, breadcrumbs
  structure/                 structure view
  refactoring/               find usages, rename
  editor/                    brace matcher, commenter, quote handler, folding
  injection/                 RegExp injection
src/main/resources/cddl/prelude.cddl   the standard prelude (RFC 8610, Appendix D)
src/test/                    tests and test data
```
