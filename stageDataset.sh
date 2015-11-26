#!/usr/bin/env bash

ARGS=$@
APPHOME=home
. apphome.sh

mvn exec:java \
  -Dapp.home=$APPHOME \
  -Dlogback.configurationFile=$APPHOME/cfg/logback.xml \
  -Dexec.args="$ARGS"
