#!/bin/sh

docker stop tauksi-container
docker rm tauksi-container
docker build . -t tauksi:v1
# non-interactive
docker run --name tauksi-container tauksi:v1 /bin/bash /root/build-projects.sh
docker cp tauksi-container:/root/metrics.zip ./

# attach if necessary: docker  exec -it tauksi-container /bin/bash
