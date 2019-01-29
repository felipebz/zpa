#!/bin/bash

set -euo pipefail

case "$TESTS" in

ci)
  pre_args="jacoco:prepare-agent"
  extra_args="jacoco:report org.eluder.coveralls:coveralls-maven-plugin:report"
  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    ./mvnw $pre_args source:jar javadoc:jar deploy $extra_args --settings=.settings.xml -B -e -V
  else
    ./mvnw $pre_args verify $extra_args -B -e -V
  fi
  ;;

it)
  git submodule update --init --recursive

  ./mvnw package -Dmaven.test.skip=true -e -B

  cd its
  ../mvnw -Dsonar.runtimeVersion="$SQ_VERSION" -Pit verify -e -B -V
  ;;

esac
