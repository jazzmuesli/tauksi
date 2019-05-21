
docker build . -t dgl-exp:v1
docker run --rm -it --name dgl-container dgl-exp:v1 /bin/bash /root/run_1.sh
