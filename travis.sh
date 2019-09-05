#!/bin/bash

set -euo pipefail

function installTravisTools {
  #mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v45 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

installTravisTools
. ~/.local/bin/installMaven35

echo "Test: $TEST"
case "$TEST" in

ci)
  mvn verify -B -e -V
  ;;

plugin)


  mvn package -T2 -Dsource.skip=true -Denforcer.skip=true -Danimal.sniffer.skip=true -Dmaven.test.skip=true

  cd its/$TEST
  mvn package -B -Dsonar.runtimeVersion="$SQ_VERSION" -DjavaVersion="LATEST_RELEASE" -Dmaven.test.redirectTestOutputToFile=false
  ;;

*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
