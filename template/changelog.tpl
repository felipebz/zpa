## SonarQube compatibility

This release is compatible with SonarQube Community Build 25.4 and newer and it has been tested on versions 25.4 to 25.7.

SonarQube commercial builds are not supported. For that, consider using [zpa-cli](https://github.com/felipebz/zpa-cli) instead.

## Install/update instructions

* Download the {{#f_release_download_url}}sonar-zpa-plugin-{{projectVersion}}.jar{{/f_release_download_url}} and copy it to the SONARQUBE_HOME/extensions/plugins.
* Remove the older plugin from that directory.
* Restart the SonarQube instance.

## For custom plugin developers

Binary compatibility is not guaranteed either in between any version number change. Custom plugins should be recompiled against the same version of `sonar-zpa-plugin`.

Download the {{#f_release_download_url}}zpa-toolkit-{{projectVersion}}.jar{{/f_release_download_url}} (requires Java 17+) to test the parser and view the AST.

## Full changelog

{{changelogChanges}}
