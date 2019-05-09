#!/usr/bin/env bash
#
# to be invoked from /etc/rc.local
#
if [ "$1" == "-w" ] # To wait for everything to start?
then
	echo -e ""
	echo -e "+-------------------------------+"
	echo -e "| Giving Multiplexer some slack |"
	echo -e "+-------------------------------+"
	sleep 20
	echo -e ""
	echo -e "+--------------------------+"
	echo -e "| Now starting Multiplexer |"
	echo -e "+--------------------------+"
fi
#
# cd ~pi/NMEADist
if [ -f "to.mux.sh" ]
then
  printf "+----------------------+\n"
  printf "| Starting Multiplexer |\n"
  printf "+----------------------+\n"
  # See the script for option details
  ./to.mux.sh -n --no-date
else
  printf "No to.mux.sh found\n"
fi
# cd -
#
