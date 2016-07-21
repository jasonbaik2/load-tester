#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
CONFIG=$1

cd $SCRIPTPATH
mkdir -p $HOME/log

java -cp "$SCRIPTPATH/lib/*" -Xms1024m -Xmx1024m me.jasonbaik.loadtester.tests.MQTTSubscriptionDurabilityTest $CONFIG | tee $HOME/durabilityTest.log