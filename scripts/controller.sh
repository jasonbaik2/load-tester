#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
ENV=$1

cd $SCRIPTPATH
mkdir -p $HOME/log

if [ -e "$HOME/controller.log" ];then
	mv $HOME/controller.log $HOME/log/controller.$(date +%s).log  
fi

java -cp "$SCRIPTPATH/lib/*" -Xms8096m -Xmx8096m me.jasonbaik.loadtester.BrokerLoadTestController $ENV > >(tee $HOME/controller.log) 2> >(tee $HOME/controller.log >&2)