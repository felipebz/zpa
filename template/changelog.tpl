## SonarQube compatibility

This version supports SonarQube 9.9 LTS and newer.

Please be aware that ZPA is only tested with SonarQube Community Edition. It may work with the commercial editions of SonarQube, but you won't be able to use ZPA and the embedded PL/SQL plugin from SonarSource to analyze the code simultaneously. If you need this scenario, please use [zpa-cli](https://github.com/felipebz/zpa-cli) instead.

## Install/update instructions

* Download the {{#f_release_download_url}}sonar-zpa-plugin-{{projectVersion}}.jar{{/f_release_download_url}} and copy it to the SONARQUBE_HOME/extensions/plugins.
* Remove the older plugin from that directory.
* Restart the SonarQube instance.

## For custom plugin developers

Binary compatibility is not guaranteed either in between any version number change. Custom plugins should be recompiled against the same version of `sonar-zpa-plugin`.

Download the {{#f_release_download_url}}zpa-toolkit-{{projectVersion}}.jar{{/f_release_download_url}} (requires Java 11+) to test the parser and view the AST.

## Changelog

{{changelogChanges}}
