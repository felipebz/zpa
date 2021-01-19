#!/bin/bash

set -euo pipefail

# Set JDK 11 as default
JAVA_HOME=$JAVA_HOME_11_X64

case "$TESTS" in

ci)
  if [ "$BUILD_SOURCEBRANCH" == "refs/heads/main" ] && [ "$SYSTEM_PULLREQUEST_ISFORK" == "False" ]; then
    ./mvnw $MAVEN_OPTS source:jar dokka:javadocJar deploy --settings=.settings.xml -B -e -V
  else
    ./mvnw $MAVEN_OPTS verify -B -e -V
  fi
  ;;

it)
  mkdir -p $MAVEN_CACHE_FOLDER
  wget https://docs.oracle.com/en/database/oracle/oracle-database/19/zip/oracle-database_19.zip -O $MAVEN_CACHE_FOLDER/oracle-database_19.zip

  git submodule update --init --recursive

  ./mvnw install -Dmaven.test.skip=true -e -B

  ./mvnw -f plsql-custom-rules/pom.xml package -e -B

  cd its

  if [ "$SQ_VERSION" == "LATEST_RELEASE[6.7]" ]; then
    # To run the integration tests with SQ 6.7 we'll to use JDK 8
    JAVA_HOME=$JAVA_HOME_8_X64
  fi
  ../mvnw $MAVEN_OPTS -Dsonar.runtimeVersion="$SQ_VERSION" -DoracleDocs="$MAVEN_CACHE_FOLDER/oracle-database_19.zip" -Pit verify -e -B -V
  ;;

esac

