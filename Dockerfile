FROM ubuntu:latest

# Install.
RUN \
  sed -i 's/# \(.*multiverse$\)/\1/g' /etc/apt/sources.list && \
  apt-get update && \
  apt-get -y upgrade && \
  apt-get install -y build-essential && \
  apt-get install default-jre maven -y && \
  apt-get install -y software-properties-common && \
  apt-get install -y byobu curl git htop man unzip vim wget zip && \
  apt-get install python3.6 python3-pip -y && \
  rm -rf /var/lib/apt/lists/*

# Add files.

ADD projects.txt /root/projects.txt
ADD build-projects.sh /root/build-projects.sh
ADD combine-files.py /root/combine-files.py

# Set environment variables.
ENV HOME /root

# Define working directory.
WORKDIR /root

RUN pip3 install pandas 

# Define default command.
CMD ["bash"]

