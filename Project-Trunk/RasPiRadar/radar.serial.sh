#!/bin/bash
CP=./build/libs/RasPiRadar-1.0-all.jar
#
JAVA_OPTIONS=
# JAVA_OPTIONS="$JAVA_OPTIONS -Dverbose=true"
# For remote debugging:
# JAVA_OPTIONS="$JAVA_OPTIONS -client -agentlib:jdwp=transport=dt_socket,server=y,address=4000"
# For remote JVM Monitoring
# JAVA_OPTIONS="$JAVA_OPTIONS -Dcom.sun.management.jmxremote.port=1234 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Djava.rmi.server.hostname=raspberrypi-boat"
#
JAVA_OPTIONS=
JAVA_OPTIONS="$JAVA_OPTIONS -Dradar.verbose=false"
#
# Physical pin numbers, delay.
PRMS=
PRMS="$PRMS --servo-port:15"
PRMS="$PRMS --delay:20"
PRMS="$PRMS --trigger-pin:16" # GPIO_04
PRMS="$PRMS --echo-pin:18"    # GPIO_05
#
# PRMS="$PRMS --just-reset"
# PRMS="$PRMS --just-one-loop" # For position calibration & tuning
#
# JAVA_OPTIONS="$JAVA_OPTIONS -Dserial.port=/dev/ttyAMA0"
JAVA_OPTIONS="$JAVA_OPTIONS -Dserial.port=/dev/ttyS0"
JAVA_OPTIONS="$JAVA_OPTIONS -Dbaud.rate=9600"
#
# PRMS="$PRMS --help"
#
echo -e "Running... 📡, prms are $PRMS"
sudo java $JAVA_OPTIONS -cp $CP raspiradar.RasPiSerialRadar $PRMS
