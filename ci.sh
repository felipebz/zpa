#!/bin/bash

set -euo pipefail

# Set JDK 11 as default
JAVA_HOME=$JAVA_HOME_11_X64

case "$TESTS" in

ci)
  if [ "$BUILD_SOURCEBRANCH" == "refs/heads/main" ] && [ "$SYSTEM_PULLREQUEST_ISFORK" == "False" ]; then
    ./gradlew --build-cache build publish
  else
    ./gradlew --build-cache build
  fi
  ;;

it)
  git submodule update --init --recursive

  ./gradlew --build-cache publishToMavenLocal -x test

  mvn -f plsql-custom-rules/pom.xml package -e -B

  if [ "$SQ_VERSION" == "7.6" ]; then
    # To run the integration tests with SQ 6.7 we'll to use JDK 8
    JAVA_HOME=$JAVA_HOME_8_X64
  fi
  ./gradlew --build-cache test -Dsonar.runtimeVersion="$SQ_VERSION" -Pit
  ;;

esac

./gradlew --stop
