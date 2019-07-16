#!/bin/bash

set -euo pipefail

case "$TESTS" in

ci)
  if [ "$BUILD_SOURCEBRANCH" == "refs/heads/master" ] && [ "$SYSTEM_PULLREQUEST_ISFORK" == "False" ]; then
    ./mvnw source:jar dokka:javadocJar deploy --settings=.settings.xml -B -e -V
  else
    ./mvnw verify -B -e -V
  fi
  ;;

it)
  # Set JDK 11 as default
  JAVA_HOME=$JAVA_HOME_11_X64

  git submodule update --init --recursive

  ./mvnw install -Dmaven.test.skip=true -e -B

  ./mvnw -f plsql-custom-rules/pom.xml package -e -B

  cd its

  if [ "$SQ_VERSION" == "LATEST_RELEASE[6.7]" ]; then
    # To run the integration tests with SQ 6.7 we'll to use JDK 8
    JAVA_HOME=$JAVA_HOME_8_X64
  fi
  ../mvnw -Dsonar.runtimeVersion="$SQ_VERSION" -Pit verify -e -B -V
  ;;

esac

