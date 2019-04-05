#!/bin/sh

docker build . -t tauksi:v1
docker run -it --name tauksi-container tauksi:v1

