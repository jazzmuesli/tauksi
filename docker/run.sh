#!/bin/sh

docker stop tauksi2-container
docker rm tauksi2-container
docker build . -t tauksi2:v1
# non-interactive
docker run -p 37017:27017 -it --name tauksi2-container tauksi2:v1 /bin/bash
#docker cp tauksi-container:/root/metrics.zip ./

# attach if necessary: docker  exec -it tauksi-container /bin/bash
