#!/bin/bash
echo Read an ADC
#
JAVA_OPTS=
JAVA_OPTS="$JAVA_OPTIONS -Dadc.verbose=false"
JAVA_OPTS="$JAVA_OPTIONS -Ddisplay.digit=false"
JAVA_OPTS="$JAVA_OPTIONS -Dverbose=false"
CP=./build/libs/ADC-1.0-all.jar
# Channel [0..1] can be passed as prm. Default is 0
sudo java -cp $CP $JAVA_OPTS analogdigitalconverter.sample.MainMCP3002Sample $*
