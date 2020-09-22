#!/bin/bash
#
# Running the Serial sample, communication between RPi and Arduino.
# Needs to be run as root (Gradle may have problem with that...)
# Requires the ArduinoSerialEvent.ino (provided) to be running on the Arduino.
# The baud rate in the Arduino sketch must be the same as the one
# provided below in the System variable "baud.rate".
#
# For libRxTx on Mac, see http://jlog.org/rxtx-mac.html
#
CP=./build/libs/Serial-IO-1.0-all.jar
#
JAVA_OPTIONS=
#
# JAVA_OPTIONS="$JAVA_OPTIONS -Djava.library.path=/usr/lib/jni" # for Raspberry Pi
#
# For Mac
JAVA_OPTIONS="$JAVA_OPTIONS -Djava.library.path=/Library/Java/Extensions"
SERIAL_PRMS=
# SERIAL_PRMS="$SERIAL_PRMS -Dserial.port=/dev/cu.usbmodem1411"
SERIAL_PRMS="$SERIAL_PRMS -Dserial.port=/dev/cu.usbmodem1421"
# SERIAL_PRMS="$SERIAL_PRMS -Dserial.port=/dev/ttyUSB0"
#
SERIAL_PRMS="$SERIAL_PRMS -Dbaud.rate=115200"
#
echo Make sure you have uploaded the right sketch on the Arduino, and connected it through its USB cable.
sudo java ${JAVA_OPTIONS} ${SERIAL_PRMS} -cp ${CP} sample.ArduinoEchoClient
#
