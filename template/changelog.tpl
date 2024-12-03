## SonarQube compatibility

This release is compatible with SonarQube 9.9 Community Build and newer. However, comprehensive testing and validation have been conducted only on SonarQube 9.9, 10.7, and 24.12. While compatibility is expected for other versions within this range, the full extent of functionality has been assured only for the tested versions.

Additionally, please note that while this version may work with SonarQube’s commercial builds, the compatibility was not tested and simultaneous analysis using ZPA and SonarSource’s embedded PL/SQL plugin is not supported. If you require this scenario, consider using [zpa-cli](https://github.com/felipebz/zpa-cli) instead.

## Install/update instructions

* Download the {{#f_release_download_url}}sonar-zpa-plugin-{{projectVersion}}.jar{{/f_release_download_url}} and copy it to the SONARQUBE_HOME/extensions/plugins.
* Remove the older plugin from that directory.
* Restart the SonarQube instance.

## For custom plugin developers

Binary compatibility is not guaranteed either in between any version number change. Custom plugins should be recompiled against the same version of `sonar-zpa-plugin`.

Download the {{#f_release_download_url}}zpa-toolkit-{{projectVersion}}.jar{{/f_release_download_url}} (requires Java 11+) to test the parser and view the AST.

## Full changelog

{{changelogChanges}}
