#!/bin/bash
#
# Logger / Runner / Kayak, etc.
# This file is to be invoked from /etc/rc.local
#
YES=
if [[ "$1" == "-y" ]]
then
  YES=1
fi
if [[ "$1" == "-n" ]]
then
  YES=0
fi
# >>> Change directory below as needed. <<<
# cd raspberry-coffee/NMEA-multiplexer
a=
if [[ "${YES}" == "1" ]]
then
  a=y
elif [[ "${YES}" == "0" ]]
then
  a=n
else
  if [[ -f ./data.nmea ]] || [[ -d logged/* ]]
  then
    echo -en "Remove previous logged data ? y|n > "
    read a
  else
    a=n
  fi
fi
#
# Following lines are specially relevant if there is a forwarder defined like this:
#
# forward.01.type=file
# forward.01.filename=./data.nmea
# forward.01.append=true
#
if [[ "${a}" = "y" ]]
then
  echo -e "Removing previous log file(s)"
  sudo rm -rf logged/*
  sudo rm data.nmea
  sudo rm nohup.out
else
  # Rename existing ones
	if [[ -f ./data.nmea ]]
  then
    # If data.nmea exits, rename it
    now=`date +%Y-%m-%d.%H:%M:%S`
    echo -e "Renaming previous data file to ${now}_data.nmea"
    sudo mv data.nmea ${now}_data.nmea
  fi
  if [[ -f ./nohup.out ]]
  then
    # If nohup.out exits, rename it
    now=`date +%Y-%m-%d.%H:%M:%S`
    echo -e "Renaming previous log file to ${now}_nohup.out"
    sudo mv nohup.out ${now}_nohup.out
  fi
fi
#
# Script parameters
#
NO_DATE=false
RMC_TIME_OK=true
SUN_FLOWER=false
START_IN_BACKGROUND=true
PROP_FILE="nmea.mux.gps.log.properties"
JAVA_OPTIONS=
#
for ARG in "$@"
do
	echo -e "Managing prm ${ARG}"
  if [[ "${ARG}" == "-p" ]] || [[ "${ARG}" == "--proxy" ]]
  then
    USE_PROXY=true
  elif [[ "${ARG}" == "--no-date" ]]
  then
    NO_DATE=true
  elif [[ "${ARG}" == "--no-rmc-time" ]]
  then
    RMC_TIME_OK=false
  elif [[ "${ARG}" == "--no-background" ]]
  then
    START_IN_BACKGROUND=false
  elif [[ ${ARG} == -m:* ]] || [[ ${ARG} == --mux:* ]] # !! No quotes !!
  then
    PROP_FILE=${ARG#*:}
    echo -e "Detected properties file $PROP_FILE"
  fi
done
#
if [[ "$NO_DATE" == "true" ]]
then
	# To use when re-playing GPS data. Those dates will not go in the cache.
	JAVA_OPTIONS="${JAVA_OPTIONS} -Ddo.not.use.GGA.date.time=true"
	JAVA_OPTIONS="${JAVA_OPTIONS} -Ddo.not.use.GLL.date.time=true"
fi
#
if [[ "$RMC_TIME_OK" == "false" ]]
then
	# To use when re-playing GPS data. Those dates will not go in the cache.
	JAVA_OPTIONS="${JAVA_OPTIONS} -Drmc.time.ok=false"
fi
#
echo -e "JAVA_OPTIONS in to.mux.sh: ${JAVA_OPTIONS}"
#
echo On its way!
MY_IP=$(hostname -I | awk '{ print $1 }')
echo "Reach http://${MY_IP}:9999/zip/index.html"
echo "  or  http://${MY_IP}:9999/zip/small-screens/small.console.02.html"
date=`date`
echo "System date is $date"
#
if [[ "$START_IN_BACKGROUND" == "true" ]]
then
  # The script below uses ${JAVA_OPTIONS}
  nohup ./mux.sh ${PROP_FILE} &
  # ./mux.sh $PROP_FILE &
else
  ./mux.sh ${PROP_FILE}
fi
