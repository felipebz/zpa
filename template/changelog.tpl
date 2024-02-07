## SonarQube compatibility

This version is compatible with SonarQube 9.9 LTS and above, but it is only tested up to SonarQube 10.4 (the current latest release).

Please be aware that ZPA is only tested with SonarQube Community Edition. It may work with the commercial editions of SonarQube, but you won't be able to use ZPA and the embedded PL/SQL plugin from SonarSource to analyze the code simultaneously. If you need this scenario, please use [zpa-cli](https://github.com/felipebz/zpa-cli) instead.

## Highlights

* The property `sonar.zpa.file.suffixes` was renamed to `sonar.plsqlopen.file.suffixes` due to changes in SonarQube 10.4.
* To support the ["Analyzers loading optimization"](https://docs.sonarsource.com/sonarqube/latest/instance-administration/improving-performance/#optimizing-the-loading-of-analyzers) feature from SonarQube 10.4, custom plugins must set the `Plugin-RequiredForLanguages`to `plsqlopen`. See the [Gradle example here](https://github.com/felipebz/zpa/commit/139994305485b47c0ba510946cb5f71bb2d8e8ef#diff-52b816ab2412e08a7c93600c54a68f08bd474bd2a75583a99bd2fea39b92b56b) and the [Maven example here](https://github.com/felipebz/zpa/commit/139994305485b47c0ba510946cb5f71bb2d8e8ef#diff-ed6c67ecd6bc69e0ef1f0a62ce7a8c35602ab8ae3af32dff5acee39e1324c486).

## Install/update instructions

* Download the {{#f_release_download_url}}sonar-zpa-plugin-{{projectVersion}}.jar{{/f_release_download_url}} and copy it to the SONARQUBE_HOME/extensions/plugins.
* Remove the older plugin from that directory.
* Restart the SonarQube instance.

## For custom plugin developers

Binary compatibility is not guaranteed either in between any version number change. Custom plugins should be recompiled against the same version of `sonar-zpa-plugin`.

Download the {{#f_release_download_url}}zpa-toolkit-{{projectVersion}}.jar{{/f_release_download_url}} (requires Java 11+) to test the parser and view the AST.

## Full changelog

{{changelogChanges}}
