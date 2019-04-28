#!/usr/bin/env bash

DOCKER_MIRROR_HOST=$(/sbin/ip route | awk '/default/ { print $3 }')

DOCKER_MIRROR_PORT=${MIRROR_PORT:-9001}
DOCKER_MIRROR="http://$DOCKER_MIRROR_HOST:$DOCKER_MIRROR_PORT"

echo $DOCKER_MIRROR
export HOST_IP=$(curl $DOCKER_MIRROR/hostip)
export HOST_PORT=$(curl $DOCKER_MIRROR/container/$HOSTNAME/port/$APP_PORT)
#export CONTAINER_IP

echo $HOST_IP   ":Host IP"
echo $HOST_PORT ":Host Port"
echo $APP_PORT  ":APP_PORT"


which java
java -version
java -jar akka-events-booking-java.jar