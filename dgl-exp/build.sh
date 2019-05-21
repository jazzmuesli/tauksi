
docker build . -t dgl-exp:v1
docker run -it --name dgl-container --rm dgl-exp:v1 /bin/bash /root/run_1.sh
