## SonarQube compatibility

This version supports SonarQube 8.9 LTS and newer.

Please be aware that ZPA is only tested with SonarQube Community Edition. It may work with the commercial editions of SonarQube, but you won't be able to use ZPA and the embedded PL/SQL plugin from SonarSource to analyze the code simultaneously. If you need this scenario, please use [zpa-cli](https://github.com/felipebz/zpa-cli) instead.

## Install/update instructions

* Download the {{#f_release_download_url}}sonar-zpa-plugin-{{projectVersion}}.jar{{/f_release_download_url}} and copy it to the SONARQUBE_HOME/extensions/plugins.
* Remove the older plugin from that directory.
* Restart the SonarQube instance.

## For custom plugin developers

Binary compatibility is not guaranteed either in between any version number change. Custom plugins should be recompiled against the same version of `sonar-zpa-plugin`.

Download the {{#f_release_download_url}}zpa-toolkit-{{projectVersion}}.jar{{/f_release_download_url}} (requires Java 11+) to test the parser and view the AST.

## Changelog

## 🚀 Features
- 972cbd7 **grammar**: Support table expression ("table(...)")
- b20d9f0 **grammar**: Support constraints on custom datatypes (closes #158)
- 7be7f84 **grammar**: Add EXCEPTION_DECLARATION and classify the exceptions as PlSqlType.EXCEPTION
- 6e339b6 **grammar**: Add an EXCEPTION_HANDLERS node to group all exception handlers
- b31cb1b **grammar**: Group parameter declarations under a new PARAMETER_DECLARATIONS node
- 36bee18 **grammar**: Support extended iterators from Oracle 21c
- 6aab1e4 **grammar**: Add support for qualified expressions from Oracle 21c
- 94c43b4 **toolkit**: Add a new tab on zpa-toolkit to show scopes and symbols identified
- 504df79 **toolkit**: Change zpa-toolkit look and feel
- 25d0d10 **toolkit**: Implement the "click-to-highlight" feature on the new "symbol tree" on zpa-toolkit
- fee4525 **toolkit**: Add "Statistics" tab on zpa-toolkit
- 73d0fbc Convert some methods to properties on Scope/Symbol
- eda8399 Improve symbol usage tracker to handle qualified variables (#125)
- 12ff703 Add Symbol.innerScope to simplify the access to the symbols declared inside program units and cursors

## 🐛 Fixes
- b631d88 **grammar**: Use MEMBER_EXPRESSION instead of CUSTOM_DATATYPE in REF/ANCHORED_DATATYPE
- 70b1add **grammar**: Fix CHARACTER_DATATYPE_CONSTRAINT
- 83cada8 Reduce usage of RegexPunctuatorChannel on PlSqlLexer to avoid unnecessary parsing with regexes

## 🧰 Tasks
- 1e28a5f Bump minimum SonarQube version to 8.9
