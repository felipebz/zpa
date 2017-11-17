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
  if [ "$TRAVIS_BRANCH" == "master" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    mvn source:jar javadoc:jar deploy --settings=.settings.xml -B -e -V
  else
    mvn verify -B -e -V
  fi
  ;;

it)
  git submodule update --init --recursive

  mvn package -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true -e -B

  cd its
  mvn -Dsonar.runtimeVersion="$SQ_VERSION" -Pit -Dmaven.test.redirectTestOutputToFile=false verify -e -B -V
  ;;

esac
