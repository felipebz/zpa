Service | Status
--- | ---
**Azure Pipelines** | [![Build Status](https://dev.azure.com/felipebz/z-plsql-analyzer/_apis/build/status/Build?branchName=master)](https://dev.azure.com/felipebz/z-plsql-analyzer/_build/latest?definitionId=3&branchName=master)
**Coveralls** (test coverage) | [![Coverage Status](https://coveralls.io/repos/github/felipebz/zpa/badge.svg?branch=master)](https://coveralls.io/github/felipebz/zpa?branch=master)

## Z PL/SQL Analyzer (previously known as SonarQube PL/SQL Community plugin)
This plugin adds PL/SQL and Oracle SQL support to the [SonarQube](https://www.sonarqube.org). SonarQube is an open platform to manage code quality. This project supports SonarQube 6.7.x and newer.

## Installation
- Download the [latest sonar-plsql-open-plugin stable release](https://github.com/felipebz/zpa/releases) or the [latest sonar-plsql-open-plugin development version (not final)](https://dev.azure.com/felipebz/z-plsql-analyzer/_build/latest?definitionId=3&branchName=master&view=results) (from the button "Artifacts") into the SONARQUBE_HOME/extensions/plugins directory;
- Restart your SonarQube server;
- Navigate to the Marketplace (SONARQUBE_URL/marketplace?filter=installed). It should list "PL/SQL (Community)" or "Z PL/SQL Analyzer" on the tab "Installed Plugins";
- Run an analysis with [SonarQube Scanner](https://docs.sonarqube.org/display/SCAN/Analyzing+with+SonarQube+Scanner).

## Contribute
You are welcome to contribute. PL/SQL and Oracle SQL are big languages and there are many pieces missing in the [grammar](https://github.com/felipebz/zpa/blob/master/plsql-frontend/src/main/java/org/sonar/plugins/plsqlopen/api/PlSqlGrammar.java).

### Running the integration tests

There are two sets of integration tests:

- [plugin](https://github.com/felipebz/zpa/tree/master/its/plugin): checks if the metrics are imported correctly in SonarQube
- [ruling](https://github.com/felipebz/zpa/tree/master/its/ruling): checks the quality of parser and rules against real-world code

To run the integrations tests, update the submodules:

    git submodule update --init --recursive
    
Build the plugin:

    ./mvnw clean install
    ./mvnw -f plsql-custom-rules/pom.xml package
    
Then run the tests:

    ./mvnw test -Pit

You can also specify the SonarQube version using the property `sonar.runtimeVersion`: 

    ./mvnw test -Pit -Dsonar.runtimeVersion=7.6

## Alternatives:
If you're looking for PL/SQL support in SonarQube, there is also 
the [commercial plugin from SonarSource](http://www.sonarsource.com/products/plugins/languages/plsql/).
