#!/bin/bash
CP=./build/libs/I2C.SPI-1.0.jar
echo -e "Ctrl+C to stop the demo"
sudo java -cp $CP -Dverbose=false spi.lcd.nokia.samples.Nokia5110Sample02
