#!/bin/sh

docker stop tauksi-container
docker rm tauksi-container
docker build . -t tauksi:v1
docker run -it --name tauksi-container tauksi:v1 /bin/bash /root/build-projects.sh

