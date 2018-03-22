#!/usr/bin/env bash
#
CP=./build/libs/RasPISamples-1.0-all.jar
#
# To invoke after running ./remote.snap.sh
#
# ./remote.snap.sh sends an HTTP request to the Raspberry PI where the camera is connected, to take the picture.
# Then it downloads the snapshot.
# From there, it can be processed as below
#
java -cp $CP weatherstation.ImageEncoder web/snap-test.jpg > web/encoded.txt
#
PROXY=
# PROXY="-Dhttp.proxyHost=www-proxy.us.oracle.com -Dhttp.proxyPort=80 -Dhttps.proxyHost=www-proxy.us.oracle.com -Dhttps.proxyPort=80"
#
# Post image to Adafruit.IO
java -cp $CP $PROXY -Dkey=54c2767878ca793f2e3cae1c45d62aa7ae9f8056 weatherstation.POSTImage web/encoded.txt
