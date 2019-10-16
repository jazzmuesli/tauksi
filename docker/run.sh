#!/bin/sh

id=tauksi3
container_id="$id-container"
docker stop $container_id
docker rm $container_id
docker build . -t $id:v2
# non-interactive
docker run -p 127.0.0.1:37017:27017 -it --name $container_id $id:v2 /bin/bash
#docker cp tauksi-container:/root/metrics.zip ./

# attach if necessary: docker  exec -it tauksi-container /bin/bash
