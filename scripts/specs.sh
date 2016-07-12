#!/bin/bash

{
	echo "=================== Linux Standard Base"
	lsb_release -sd 2>&1
	echo
	echo "=================== Memory"
	free -m 2>&1
	echo
	echo "=================== Processor"
	cat /proc/cpuinfo 2>&1
	echo
	echo "=================== Java"
	java -version 2>&1
} > specs.txt