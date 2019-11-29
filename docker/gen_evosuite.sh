#!/bin/sh
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/; export PATH=$JAVA_HOME/bin:$PATH
mvn compile
mvn -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
mvn -Dsandbox_mode=OFF -Duse_separate_classloader=false -DmemoryInMB=4000 -Dcores=6 -DtimeInMinutesPerClass=1 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate
mvn -Dsandbox_mode=OFF -Duse_separate_classloader=false org.evosuite.plugins:evosuite-maven-plugin:LATEST:export
