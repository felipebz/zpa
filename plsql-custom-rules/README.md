# plsql-custom-rules

This repository contains examples of custom rules for PL/SQL code analysis using SonarQube. The examples are available as both Gradle and Maven projects. The purpose of this README is to provide information on how to customize and use the examples.

## Getting Started

## Customizing the pom.xml (if you're using Maven)

Properties such as `groupId`, `artifactId`, `version`, `name` and `description` can be freely modified.

It is important to note that the `sonar-packaging-maven-plugin` is required and it contains the entry point of the plugin, as provided in the property `pluginClass`. If you refactor the code or rename the class extending `org.sonar.api.SonarPlugin`, you will have to change this configuration.

## Customizing the build.gradle.kts (if you're using Gradle)

Properties such as `group`, `version` and `description` can be freely modified.

It is important to note that you'll may need to update the values in [attributes map](build.gradle.kts#L52) of the `jar` task.

## Dependencies

The plugin requires two dependencies: `sonar-plugin-api` and `sonar-zpa-plugin`. The `sonar-plugin-api` dependency represents the minimum version of SonarQube your plugin will support. Additionally, a dependency on `zpa-checks-testkit` can be added to make testing easier. These dependencies are already included in the `pom.xml` and `build.gradle.kts` files.

## Compatibility with zpa-cli

To make the plugin compatible with [zpa-cli](https://github.com/felipebz/zpa-cli), you need to adjust the content of [extensions.idx](src/main/resources/META-INF/extensions.idx) to match your `CustomPlSqlRulesDefinition` subclass.
