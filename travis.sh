#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir -p ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v38 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

case "$TESTS" in

ci)
  pre_args="jacoco:prepare-agent"
  extra_args="jacoco:report org.eluder.coveralls:coveralls-maven-plugin:report"
  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    mvn $pre_args source:jar javadoc:jar deploy $extra_args --settings=.settings.xml -B -e -V
  else
    mvn $pre_args verify $extra_args -B -e -V
  fi
  ;;

it)
  git submodule update --init --recursive

  mvn package -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true -e -B

  cd its
  mvn -Dsonar.runtimeVersion="$SQ_VERSION" -Pit verify -e -B -V
  ;;

esac
