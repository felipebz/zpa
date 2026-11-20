# ZPA

[![Latest release](https://img.shields.io/github/release/felipebz/zpa.svg) ](https://github.com/felipebz/zpa/releases/latest)
[![Build](https://github.com/felipebz/zpa/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/felipebz/zpa/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarqube.felipebz.com/api/project_badges/measure?project=com.felipebz.zpa%3Azpa&metric=alert_status)](https://sonarqube.felipebz.com/dashboard?id=com.felipebz.zpa%3Azpa)

ZPA is an open-source parser and static analyzer for PL/SQL and Oracle SQL. It finds bugs, security issues and
maintainability problems with [more than 70 rules](https://zpa.felipebz.com/docs/category/rules/), from unhandled
exceptions and SQL injection in dynamic SQL to queries that silently return no rows.

ZPA analyzes a project as a whole, not file by file: a package body is checked against its specification in another
file, and calls are resolved across the project. It can also read the tables and columns of an Oracle database, so
that rules know the types and the nullability of the columns your code uses.

Documentation, guides and the rule catalog are at **[zpa.felipebz.com](https://zpa.felipebz.com/docs/)**. See it in
action in our [SonarQube instance](https://sonarqube.felipebz.com/projects?languages=plsqlopen).

## Ways to use it

- **SonarQube plugin**: install `sonar-zpa-plugin` on SonarQube Server or SonarQube Community Build and analyze your
  projects with SonarScanner. Start with the [getting started guide](https://zpa.felipebz.com/docs/getting-started/),
  and check which SonarQube versions each release supports in the
  [compatibility matrix](https://zpa.felipebz.com/docs/sonarqube-plugin/compatibility/).
- **Command line**: [zpa-cli](https://github.com/felipebz/zpa-cli) runs the same analysis on your machine or in CI,
  without a SonarQube server, and can export its results for SonarQube Cloud and other tools. See the
  [CLI guide](https://zpa.felipebz.com/docs/cli/).
- **ZPA Toolkit**: a desktop tool to explore the AST (abstract syntax tree) and the semantic information ZPA builds
  for a piece of code, useful when writing rules. Download it from the
  [releases page](https://github.com/felipebz/zpa/releases/latest).

## Highlights

- [Oracle database metadata](https://zpa.felipebz.com/docs/sonarqube-plugin/database-metadata/): read the tables,
  views and columns of your schemas, and tell which schema each file compiles in.
- [Conditional compilation](https://zpa.felipebz.com/docs/sonarqube-plugin/scanning-code/): analyze the code Oracle
  compiles for your `PLSQL_CCFLAGS` and database version.
- [Oracle Forms](https://zpa.felipebz.com/docs/sonarqube-plugin/oracle-forms-support/): check references to blocks,
  items, alerts and lists of values, and type the host variables of a form.
- [utPLSQL](https://zpa.felipebz.com/docs/sonarqube-plugin/importing-utplsql-reports/): import test and coverage reports
  into SonarQube, and check the annotations of your test packages.
- [Custom rules](https://zpa.felipebz.com/docs/sonarqube-plugin/adding-rules/): write your own rules as a plugin that
  works in SonarQube and in zpa-cli.

Release notes are published on the [blog](https://zpa.felipebz.com/blog/) and on the
[releases page](https://github.com/felipebz/zpa/releases).

## Contributing

Bug reports and rule ideas are welcome in the [issues](https://github.com/felipebz/zpa/issues). Please read the
[contribution guidelines](CONTRIBUTING.md) before opening a pull request; they also explain how to build the project
and run the integration tests.

The code is divided in these modules:

- `zpa-core` - The heart of the project: the lexer, the parser and the semantic analysis of PL/SQL and Oracle SQL code.
- `zpa-checks` - The built-in rules.
- `zpa-checks-testkit` - Test helpers for rules, also usable to test custom rules.
- `zpa-oracle-metadata` - Optional reader of Oracle dictionary metadata (see its [README](zpa-oracle-metadata/README.md)).
- `sonar-zpa-plugin` - The SonarQube plugin: everything needed to integrate with the SonarQube platform.
- `zpa-toolkit` - The ZPA Toolkit.
- `plsql-custom-rules` - An example project of custom rules.

Supporting modules (`build-logic`, `project-api-consumer-test`) hold build conventions and tests of the public API.

The API for custom rules lives in the package `com.felipebz.zpa.api` of the sources. In the plugin JAR it is relocated
to `org.sonar.plugins.plsqlopen.api`, the package custom rules compile against (a requirement of the SonarQube server).
Classes outside the API are not meant for external use and may change without notice.

## License

ZPA is licensed under the [GNU Lesser General Public License v3.0](LICENSE).
