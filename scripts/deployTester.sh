#!/bin/bash

SERVERS=( "172.31.5.252" "172.31.13.122" "172.31.14.98" "172.31.3.235" "172.31.13.160" "172.31.13.158" "172.31.14.72" "172.31.4.14" "172.31.5.227" )
BIN=load-tester-0.0.1-SNAPSHOT-bin.zip

for server in "${SERVERS[@]}"
do
	scp -i jasonbaik.pem $BIN ec2-user@$server:/home/ec2-user
	ssh -i jasonbaik.pem ec2-user@$server unzip -o "/home/ec2-user/$BIN"
done
