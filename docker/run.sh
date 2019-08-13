#!/bin/sh

docker stop tauksi2-container
docker rm tauksi2-container
docker build . -t tauksi2:v1
# non-interactive
docker run -p 80:80 -p 27017:37017 -it --name tauksi-container tauksi2:v1 /bin/bash
#docker cp tauksi-container:/root/metrics.zip ./

# attach if necessary: docker  exec -it tauksi-container /bin/bash
