#!/bin/bash
#
# Build and run a docker image
#
OK=false
DOCKER_FILE=
IMAGE_NAME=
RUN_CMD=
EXTRA_PRM=
MESSAGE="Bye! ✋\n"
#
# Make sure docker is available
DOCKER=`which docker`
if [ "$DOCKER" == "" ]
then
  echo -e "Docker not available on this machine, exiting."
  echo -e "To install Docker, see https://store.docker.com/search?type=edition&offering=community"
  exit 1
fi
#
while [ "$OK" = "false" ]
do
  # Menu
  echo -e "+-------------- D O C K E R   I M A G E   B U I L D E R ---------------+"
  echo -e "+----------------- Build 🏗️  and run 🏃 a docker image. ----------------+"
  echo -e "|  1. Nav Server, Debian                                               |"
  echo -e "| 1p. Nav Server, Debian, with proxy (as an example)                   |"
  echo -e "|  2. Web Components, Debian                                           |"
  echo -e "|  3. To run on a Raspberry Pi, Java, Raspberry Coffee, Web Components |"
  echo -e "|  4. Node PI, to run on a Raspberry Pi                                |"
  echo -e "|  5. Node PI, to run on Debian                                        |"
  echo -e "|  6. GPS-mux, to run on a Raspberry Pi (logger)                       |"
  echo -e "|  7. Golang, basics                                                   |"
  echo -e "|  8. Raspberry Pi, MATE, with java, node, web comps, VNC              |"
  echo -e "|  9. Debian, Java, Scala, Spark                                       |"
  echo -e "| 10. Ubuntu MATE, TensorFlow, Keras, Python3, Jupyter, PyCharm, VNC   |"
  echo -e "+----------------------------------------------------------------------+"
  echo -e "| Q. Oops, nothing, thanks, let me out.                                |"
  echo -e "+----------------------------------------------------------------------+"
  echo -en "== You choose => "
  read a
  #
  case "$a" in
    "Q" | "q")
      OK=true
      printf "You're done.\n   Please come back soon!\n"
      ;;
    "1")
      OK=true
      DOCKER_FILE=navserver.Dockerfile
      IMAGE_NAME=oliv-nav
			RUN_CMD="docker run -p 8080:9999 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside (this machine)
			#
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://localhost:8080/web/index.html from your browser.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "1p")
      OK=true
      DOCKER_FILE=navserver.Dockerfile
      IMAGE_NAME=oliv-nav
			RUN_CMD="docker run -p 8080:9999 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside (this machine)
			#
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://localhost:8080/web/index.html from your browser.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      #
      EXTRA_PRM="--build-arg http_proxy=http://www-proxy-hqdc.us.oracle.com:80"
      EXTRA_PRM="$EXTRA_PRM --build-arg https_proxy=http://www-proxy-hqdc.us.oracle.com:80"
      EXTRA_PRM="$EXTRA_PRM --build-arg no_proxy=localhost,127.0.0.1,orahub.oraclecorp.com,artifactory-slc.oraclecorp.com"
      ;;
    "2")
      OK=true
      DOCKER_FILE=webcomponents.Dockerfile
      IMAGE_NAME=oliv-webcomp
			RUN_CMD="docker run -p 9999:9999 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside (this machine)
			#
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://localhost:9999/index.html from your browser.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "3")
      OK=true
      DOCKER_FILE=rpi.Dockerfile
      IMAGE_NAME=oliv-rpi
			RUN_CMD="docker run -p 8081:8080 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside (this machine)
			#
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://localhost:8081/oliv-components/index.html from your browser.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "4")
      OK=true
      DOCKER_FILE=node-pi.Dockerfile
      IMAGE_NAME=oliv-nodepi
			# RUN_CMD="docker run -p 9876:9876 -t -i --device=/dev/ttyUSB0 $IMAGE_NAME:latest /bin/bash"
			RUN_CMD="docker run -p 9876:9876 -t -i --privileged -v /dev/ttyUSB0:/dev/ttyUSB0 -d $IMAGE_NAME:latest"
			#                      |    |            |             |             |
			#                      |    |            |             |             Device IN the docker image
			#                      |    |            |             Device name in the host (RPi) machine
			#                      |    |            sudo access to the Serial Port
			#                      |    tcp port IN the docker image
			#                      tcp port as seen from outside (this machine)
			#
      # MESSAGE="See doc at https://github.com/OlivierLD/node.pi/blob/master/README.md"
			IP_ADDR=`ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{ print $2 }'`
			if [ "$IP_ADDR" = "" ]
			then
			  IP_ADDR="localhost"
			fi
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://$IP_ADDR:9876/data/demos/gps.demo.html in your browser.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "5")
      OK=true
      DOCKER_FILE=node-debian.Dockerfile
      IMAGE_NAME=oliv-nodedebian
			# RUN_CMD="docker run -p 9876:9876 --privileged -v /dev/tty.usbserial:/dev/ttyUSB0 -d $IMAGE_NAME:latest"
			RUN_CMD="docker run -p 9876:9876 -d $IMAGE_NAME:latest"
			#                      |    |
			#                      |    tcp port used in the image
			#                      tcp port as seen from outside (this machine)
			#
			IP_ADDR=`ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{ print $2 }'`
			if [ "$IP_ADDR" = "" ]
			then
			  IP_ADDR="localhost"
			fi
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://$IP_ADDR:9876/data/demos/gps.demo.html in your browser.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "6")
      OK=true
      DOCKER_FILE=rpi.mux.Dockerfile
      IMAGE_NAME=oliv-nmea-mux
			# RUN_CMD="docker run -p 9876:9876 -t -i --device=/dev/ttyUSB0 $IMAGE_NAME:latest /bin/bash"
			RUN_CMD="docker run -p 9999:9999 -t -i --privileged -v /dev/ttyUSB0:/dev/ttyUSB0 -d $IMAGE_NAME:latest"
			#                      |    |            |             |             |
			#                      |    |            |             |             Device IN the docker image
			#                      |    |            |             Device name in the host (RPi) machine
			#                      |    |            sudo access to the Serial Port
			#                      |    tcp port IN the docker image
			#                      tcp port as seen from outside (this machine)
			#
      # MESSAGE="See doc at https://github.com/OlivierLD/node.pi/blob/master/README.md"
			IP_ADDR=`ifconfig | grep 'inet ' | grep -v '127.0.0.1' | awk '{ print $2 }'`
			if [ "$IP_ADDR" = "" ]
			then
			  IP_ADDR="localhost"
			fi
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Reach http://$IP_ADDR:9999/web/index.html in your browser.\n"
      MESSAGE="${MESSAGE}REST operations available: http://localhost:9999/mux/oplist.\n"
      MESSAGE="${MESSAGE}You can also log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "7")
      OK=true
      DOCKER_FILE=golang.Dockerfile
      IMAGE_NAME=oliv-go
      RUN_CMD="docker run -d $IMAGE_NAME:latest"
      #
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Log in using: docker run -it $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "8")
      OK=true
      DOCKER_FILE=rpidesktop.Dockerfile
      IMAGE_NAME=oliv-pi-vnc
      RUN_CMD="docker run -d $IMAGE_NAME:latest"
      #
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Log in using: docker run -it --rm -p 5901:5901 -p 8080:8080 -e USER=root $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}- then run 'vncserver :1 -geometry 1280x800 (or 1440x900, 1680x1050, etc) -depth 24'\n"
      MESSAGE="${MESSAGE}- then use a vncviewer on localhost:1, password is 'mate'\n"
      MESSAGE="${MESSAGE}- then 'node server.js', and reach http://localhost:8080/oliv-components/index.html ...\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "9")
      OK=true
      DOCKER_FILE=spark-debian.Dockerfile
      IMAGE_NAME=oliv-spark
      RUN_CMD="docker run -d $IMAGE_NAME:latest"
      #
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}Log in using: docker run -it --rm -e USER=root $IMAGE_NAME:latest /bin/bash\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    "10")
      OK=true
      DOCKER_FILE=tensorflow.Dockerfile
      IMAGE_NAME=oliv-tf-vnc
      RUN_CMD="docker run -d $IMAGE_NAME:latest"
      #
      MESSAGE="---------------------------------------------------\n"
      MESSAGE="${MESSAGE}You can log in using: docker run --interactive --tty --rm --publish 5901:5901 --publish 8888:8888 [--env USER=root] [--volume tensorflow:/root/workdir/shared] $IMAGE_NAME:latest /bin/bash \n"
      MESSAGE="${MESSAGE}                   or docker run -it --rm -p 5901:5901 -p 8888:8888 -e USER=root -v tensorflow:/root/workdir/shared $IMAGE_NAME:latest /bin/bash \n"
      MESSAGE="${MESSAGE}- then run 'vncserver :1 -geometry 1280x800 (or 1440x900, 1680x1050, etc) -depth 24'\n"
      MESSAGE="${MESSAGE}- then use a vncviewer on localhost:1, password is 'mate'\n"
      MESSAGE="${MESSAGE}- then (for example) python3 examples/mnist_cnn.py ...\n"
      MESSAGE="${MESSAGE}       or python3 examples/oliv/01.py ...\n"
      MESSAGE="${MESSAGE}  Several samples are available in the examples folder.\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
			MESSAGE="${MESSAGE}To start Jupyter Notebook, type: jupyter notebook --allow-root --ip 0.0.0.0 --no-browser\n"
			MESSAGE="${MESSAGE}  - Default port 8888 is exposed, you can use from the host http://localhost:8888/?token=6c95d878c045212bxxxxxx\n"
      MESSAGE="${MESSAGE}---------------------------------------------------\n"
      ;;
    *)
      echo -e "What? Unknown command [$a]"
      ;;
  esac
  #
done
#
#
if [ "$DOCKER_FILE" != "" ]
then
  #
  # Proxies, if needed
  # export HTTP_PROXY=http://www-proxy.us.oracle.com:80
  # export HTTPS_PROXY=http://www-proxy.us.oracle.com:80
  #
  EXTRA=
  if [ "$EXTRA_PRM" != "" ]
  then
    EXTRA="with $EXTRA_PRM"
  fi
  echo -e "---------------------------------------------------"
  echo -e "Generating $IMAGE_NAME from $DOCKER_FILE $EXTRA"
  echo -e "---------------------------------------------------"
  # Possibly use --quiet
  docker build -f $DOCKER_FILE -t $IMAGE_NAME $EXTRA_PRM .
  #
  # Now run
  echo -e "Now running $RUN_CMD..."
  CONTAINER_ID=`$RUN_CMD`
  echo -e "Running container ID $CONTAINER_ID"
fi
printf "%b" "$MESSAGE"
# Prompt for export
if [ "$DOCKER_FILE" != "" ]
then
  echo -en "== Do you want to export this container $CONTAINER_ID ? [n]|y > "
  read a
  if [ "$a" == "Y" ]  || [ "$a" == "y" ]
  then
    echo -e "Last generated one is $IMAGE_NAME:latest, its ID is $CONTAINER_ID"
    echo -en "== Please enter the name of the tar file to generate (like export.tar) > "
    read fName
    echo -en "Will export container $CONTAINER_ID into $fName - Is that correct ? [n]|y > "
    read a
    if [ "$a" == "Y" ]  || [ "$a" == "y" ]
    then
      docker export --output $fName $CONTAINER_ID
    fi
  fi
  echo -e "You can export a running container any time by running 'docker export --output export.tar [Container ID]'"
  echo -e "Docker commands are documented at https://docs.docker.com/engine/reference/commandline/docker/"
fi
