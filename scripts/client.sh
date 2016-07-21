#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
CONFIG=$1
ENV=$2

cd $SCRIPTPATH
mkdir -p $HOME/log

if [ -e "$HOME/client.log" ];then
	mv $HOME/client.log $HOME/log/client_$HOSTNAME.$(date +%s).log  
fi

java -cp "$SCRIPTPATH/lib/*" -Xms2048m -Xmx2048m -XX:+PrintGCDetails -XX:+PrintGCDateStamps me.jasonbaik.loadtester.Client $CONFIG $ENV | tee $HOME/client_$HOSTNAME.log