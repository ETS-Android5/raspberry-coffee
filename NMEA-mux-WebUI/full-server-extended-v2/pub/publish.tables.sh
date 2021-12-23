#!/bin/bash
echo ----------------------------
echo T900 publisher
echo ----------------------------
#
export SCRIPT_DIR=`dirname $0`
echo moving to ${SCRIPT_DIR}
cd ${SCRIPT_DIR}
#
export HOME=..
#
export CP=${CP}:../../build/libs/RESTNavServer-1.0-all.jar
export CP=${CP}:${HOME}/libs/orai18n-collation.jar
export CP=${CP}:${HOME}/libs/orai18n-mapping.jar
export CP=${CP}:${HOME}/libs/fnd2.zip
export CP=${CP}:${HOME}/libs/xdo-0301.jar
#
XSL_STYLESHEET=
PRM_OPTION="-docconf ./config.cfg"
LOOP=true
while [ "${LOOP}" == "true" ]
do
	clear
	echo -e "+-------------------------------+"
	echo -e "| Publication - Tables 900      |"
	echo -e "+-------------------------------+"
	echo -e "| 0. FOP Processor help         |"
	echo -e "| 1. Tables de Dieumegard (pdf) |"
	echo -e "| 2. Tables de Bataille (pdf)   |"
	echo -e "| 3. Tables de Dieumegard (rtf) |"
	echo -e "| 4. Tables de Bataille (rtf)   |"
	echo -e "+-------------------------------+"
	echo -e "| 5. Dieumegard in HTML (demo)  |"
	echo -e "+-------------------------------+"
	echo -e "| Q. Quit                       |"
	echo -e "+-------------------------------+"
	echo -en "You choose > "
	read resp
	case "${resp}" in
    "Q" | "q")
      LOOP=false
      printf "You're done.\n   Please come back soon!\n"
      ;;
    "0")
      # Doc at https://docs.oracle.com/cd/B24289_01/current/acrobat/115xdoug.pdf
			COMMAND="java -Xms256m -Xmx1536m -classpath ${CP} oracle.apps.xdo.template.FOProcessor -h"
			${COMMAND}
			echo "Hit Return"
			read a
      ;;
    "1")
			echo Publishing, please be patient...
			#
			java -classpath ${CP} tables.Dieumegard > dieumegard.xml
			#
			COMMAND="java -Xms256m -Xmx1536m -classpath ${CP} oracle.apps.xdo.template.FOProcessor ${PRM_OPTION} -xml dieumegard.xml -xsl ./dieumegard-fo.xsl -pdf dieumegard.pdf"
			echo Running from ${PWD}: ${COMMAND}
			${COMMAND}
			echo Done transforming, document is ready.
			echo "Hit Return"
			read a
      ;;
    "2")
			echo Publishing, please be patient...
			#
			java -classpath ${CP} tables.Bataille > bataille.xml
			#
			COMMAND="java -Xms256m -Xmx1536m -classpath ${CP} oracle.apps.xdo.template.FOProcessor ${PRM_OPTION} -xml bataille.xml -xsl ./bataille-fo.xsl -pdf bataille.pdf"
			echo Running from ${PWD}: ${COMMAND}
			${COMMAND}
			echo Done transforming, document is ready.
			echo "Hit Return"
			read a
      ;;
    "3")
			echo Publishing, please be patient...
			#
			java -classpath ${CP} tables.Dieumegard > dieumegard.xml
			#
			COMMAND="java -Xms256m -Xmx1536m -classpath ${CP} oracle.apps.xdo.template.FOProcessor ${PRM_OPTION} -xml dieumegard.xml -xsl ./dieumegard-fo.xsl -rtf dieumegard.rtf"
			echo Running from ${PWD}: ${COMMAND}
			${COMMAND}
			echo Done transforming, document is ready.
			echo "Hit Return"
			read a
      ;;
    "4")
			echo Publishing, please be patient...
			#
			java -classpath ${CP} tables.Bataille > bataille.xml
			#
			COMMAND="java -Xms256m -Xmx1536m -classpath ${CP} oracle.apps.xdo.template.FOProcessor ${PRM_OPTION} -xml bataille.xml -xsl ./bataille-fo.xsl -rtf bataille.rtf"
			echo Running from $PWD: ${COMMAND}
			${COMMAND}
			echo Done transforming, document is ready.
			echo "Hit Return"
			read a
      ;;
     "5")
			java -classpath ${CP} tables.Dieumegard > dieumegard.xml
			#
      java -classpath ${CP} oracle.xml.parser.v2.oraxsl -s html.xsl -l dieumegard.xml -o . -r html
      #
      open dieumegard.xml.html
      #
			echo "Hit Return"
			read a
      ;;
     *)
      echo -e "Unknown command [$resp]"
			echo "Hit Return"
			read a
      ;;
	esac
done
#
