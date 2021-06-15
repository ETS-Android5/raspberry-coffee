#!/bin/bash
#
echo -e "Use K1 and K3 (or Joystick Up and Down) to scroll through screens"
echo -e "Use K2 to exit"
#
FONT_SIZE=16
if [[ "$1" != "" ]]
then
  FONT_SIZE=$1
fi
CP=./build/libs/I2C-SPI-1.0-all.jar
JAVA_OPTS="-Dwaveshare.1in3.verbose=false"
JAVA_OPTS="${JAVA_OPTS} -Dfont.size=$FONT_SIZE"
JAVA_OPTS="${JAVA_OPTS} -Drotation=180"     # Change at will
sudo java -cp ${CP} ${JAVA_OPTS} spi.lcd.waveshare.samples.InteractiveScreenSample
