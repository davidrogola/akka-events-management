#!/usr/bin/env bash
DOCKER_MIRROR_HOST=$(/sbin/ip route | awk '/default/ { print $3 }')

DOCKER_MIRROR_PORT=${MIRROR_PORT:-9001}
DOCKER_MIRROR="http://$DOCKER_MIRROR_HOST:$DOCKER_MIRROR_PORT"

# HOSTNAME is the Docker container ID, docker initializes this variable to the container ID
# APP_PORT is the container Port
export HOST_IP=$(curl $DOCKER_MIRROR/hostip)
export HOST_PORT=$(curl $DOCKER_MIRROR/container/$HOSTNAME/port/$APP_PORT)