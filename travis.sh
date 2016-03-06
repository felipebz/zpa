#!/bin/bash

set -euo pipefail

function configureTravis {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v25 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}
configureTravis

case "$TESTS" in

ci)
  mvn verify -B -e -V
  ;;

ruling)
  mvn package -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true -e -B

  cd its
  mvn -Dsonar.runtimeVersion="$SQ_VERSION" -Pit -Dmaven.test.redirectTestOutputToFile=false verify -e -B -V
  ;;

esac
