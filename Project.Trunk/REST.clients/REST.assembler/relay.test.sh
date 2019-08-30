#!/bin/bash
echo -e "Test the relay, manually"
#
JAVA_OPTS=
JAVA_OPTS="$JAVA_OPTS -Drelay.verbose=true"
CP=./build/libs/REST.assembler-1.0-all.jar
#
sudo java -cp $CP $JAVA_OPTS relay.RelayManager $*
#
echo Done.
