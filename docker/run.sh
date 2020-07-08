#!/bin/sh

id=tauksi4
container_id="$id-container"
docker stop $container_id
docker rm $container_id
docker build . -t $id:v2
# non-interactive
PROJ_DIR=$HOME/projects
ROOT_DIR=$HOME/$id
mkdir -p $ROOT_DIR $PROJ_DIR
docker run -v $PROJ_DIR:/projects -v $ROOT_DIR:/shared --net="host" -it --name $container_id $id:v2 /bin/bash
#docker cp tauksi-container:/root/metrics.zip ./

# attach if necessary: docker  exec -it tauksi-container /bin/bash
