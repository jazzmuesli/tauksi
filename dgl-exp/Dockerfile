FROM ubuntu:latest

ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Europe/London

# Install.
RUN \
  sed -i 's/# \(.*multiverse$\)/\1/g' /etc/apt/sources.list && \
  apt-get update && \
  apt-get -y upgrade && \
  apt-get install -y build-essential && \
  apt-get install -y software-properties-common && \
  apt-get install -y byobu curl git htop man unzip vim wget zip && \
  apt-get install python3.6 python3.6-tk python3-pip -y && \
  rm -rf /var/lib/apt/lists/*

# Set environment variables.
ENV HOME /root

# Define working directory.
WORKDIR /root

RUN pip3 install networkx pandas matplotlib 

RUN pip3 install -v --log /tmp/pip.log torch

RUN pip3 install dgl

RUN pip3 install tqdm

ADD 1_first.py /root/1_first.py
ADD run_1.sh /root/run_1.sh

# Define default command.
CMD ["bash"]

