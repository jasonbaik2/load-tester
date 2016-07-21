#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
CONFIG=$1
ENV=$2

cd $SCRIPTPATH
mkdir -p $HOME/log

if [ -e "$HOME/controller.log" ];then
	mv $HOME/controller.log $HOME/log/controller.$(date +%s).log  
fi

java -cp "$SCRIPTPATH/lib/*" -Xms1024m -Xmx1024m me.jasonbaik.loadtester.BrokerLoadTestController $CONFIG $ENV | tee $HOME/controller.log