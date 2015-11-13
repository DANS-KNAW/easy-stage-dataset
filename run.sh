#!/usr/bin/env bash

APPHOME=home
. apphome.sh

mvn exec:java -Dapp.home=$APPHOME \
              -Dconfig.file=$APPHOME/cfg/application.conf \
              -Dlogback.configurationFile=$APPHOME/cfg/logback.xml \
              -Dexec.args="$@"
