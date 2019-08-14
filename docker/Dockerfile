FROM ubuntu:latest

ENV DEBIAN_FRONTEND=noninteractive
# Install.
RUN \
  sed -i 's/# \(.*multiverse$\)/\1/g' /etc/apt/sources.list && \
  apt-get update && \
  apt-get -y upgrade && \
  apt-get install -y build-essential && \
  apt-get install default-jre default-jdk maven -y && \
  apt-get install -y software-properties-common && \
  apt-get install -y byobu curl git htop man unzip vim wget zip && \
  apt-get install python3.6 python3-pip -y && \
  apt-get install python2.7 python-pip parallel -y && \
  apt-get install r-base -y && \
  rm -rf /var/lib/apt/lists/*

RUN wget -qO - https://www.mongodb.org/static/pgp/server-4.2.asc | apt-key add -
RUN echo "deb [ arch=amd64 ] https://repo.mongodb.org/apt/ubuntu bionic/mongodb-org/4.2 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-4.2.list
RUN apt-get update
RUN apt-get install -y mongodb-org

# Set environment variables.
ENV HOME /root

# Define working directory.
WORKDIR /root

RUN git clone https://github.com/jazzmuesli/RefactoringMiner.git
RUN git clone https://github.com/jazzmuesli/ck.git
RUN git clone https://github.com/jazzmuesli/tauksi.git
RUN git clone https://github.com/jazzmuesli/javadepextractor.git
RUN cd /root/ck && mvn -DskipTests install
RUN cd /root/javadepextractor && mvn -DskipTests install
RUN wget https://services.gradle.org/distributions/gradle-5.0-bin.zip -P /tmp
RUN unzip -d /opt/gradle /tmp/gradle-*.zip
ENV export GRADLE_HOME=/opt/gradle/gradle-5.0
ENV export PATH=/opt/gradle/gradle-5.0/bin:${PATH}

RUN cd /root/RefactoringMiner && git checkout upgrade-jdt-318 && /opt/gradle/gradle-5.0/bin/gradle install
RUN cd /root/tauksi && git pull && mvn -DskipTests install
RUN mkdir -p /data/db

# Define default command.
CMD ["bash"]

