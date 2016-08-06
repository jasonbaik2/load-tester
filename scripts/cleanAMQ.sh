#!/bin/bash

AMQ_DIR=/home/ec2-user/amq

rm -rf $AMQ_DIR/data/log/*
rm -rf $AMQ_DIR/data/tmp/*
rm -rf $AMQ_DIR/data/amq/kahadb/*