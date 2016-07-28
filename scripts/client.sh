#!/bin/bash

SCRIPT=$(readlink -f "$0")
SCRIPTPATH=$(dirname "$SCRIPT")
ENV=$1

cd $SCRIPTPATH
mkdir -p $HOME/log

if [ -e "$HOME/client.log" ];then
	mv $HOME/client.log $HOME/log/client.$(date +%s).log  
fi

java -cp "$SCRIPTPATH/lib/*" -Xms2048m -Xmx4086m -XX:+PrintGCDetails -XX:+PrintGCDateStamps me.jasonbaik.loadtester.Client $ENV > >(tee $HOME/client.log) 2> >(tee $HOME/client.log >&2)