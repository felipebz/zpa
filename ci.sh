#!/bin/bash

set -euo pipefail

case "$TESTS" in

ci)
  pre_args="jacoco:prepare-agent"
  extra_args="jacoco:report org.eluder.coveralls:coveralls-maven-plugin:report -DrepoToken=$COVERALLS_TOKEN"
  if [ "$BUILD_SOURCEBRANCH" == "refs/heads/master" ] && [ "$SYSTEM_PULLREQUEST_ISFORK" == "False" ]; then
    ./mvnw $pre_args source:jar javadoc:jar deploy $extra_args --settings=.settings.xml -B -e -V
  else
    ./mvnw $pre_args verify $extra_args -B -e -V
  fi
  ;;

it)
  git submodule update --init --recursive

  ./mvnw install -Dmaven.test.skip=true -e -B

  ./mvnw -f plsql-custom-rules/pom.xml package -e -B

  cd its
  ../mvnw -Dsonar.runtimeVersion="$SQ_VERSION" -Pit verify -e -B -V
  ;;

esac

