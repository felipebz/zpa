## SonarQube compatibility

This release supports **SonarQube Community Build 26.2 and newer** and has been tested with versions **26.2 through 26.9**.

SonarQube commercial builds are not supported. If you need to analyze projects without installing a SonarQube plugin, consider using [zpa-cli](https://github.com/felipebz/zpa-cli).

## Install or update

1. Download {{#f_release_download_url}}sonar-zpa-plugin-{{projectVersion}}.jar{{/f_release_download_url}}.
2. Copy it to `SONARQUBE_HOME/extensions/plugins`, replacing the previous ZPA version.
3. Restart SonarQube.

## What's changed

{{changelogChanges}}

## ZPA Toolkit

Download {{#f_release_download_url}}zpa-toolkit-{{projectVersion}}.jar{{/f_release_download_url}} to inspect the parser AST and symbol table.

The toolkit requires Java 21 or newer.

<details>
<summary>Developing custom ZPA plugins?</summary>

Binary compatibility is not guaranteed between ZPA versions. Custom plugins should be recompiled against the same version of `sonar-zpa-plugin` used by the SonarQube instance.

</details>
