#!/bin/sh
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/; export PATH=$JAVA_HOME/bin:$PATH
mvn compile
#mvn -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
#mvn compile
mvn org.pavelreich.saaremaa:plugin:metrics
#mvn -DmemoryInMB=4000 -Dcores=6 -DtimeInMinutesPerClass=1 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate
#mvn org.evosuite.plugins:evosuite-maven-plugin:LATEST:export
#mvn -Drat.skip=true  -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 -fae -Dmaven.test.failure.ignore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test
mvn -Drat.skip=true -DseqTestMethods=false -DshuffleTests=true -DinterceptorEnabled=false -Dtimeout=65 org.pavelreich.saaremaa:plugin:ctest
#mvn -Dsandbox_mode=OFF
mvn org.pavelreich.saaremaa:plugin:analyse-testcases
mvn org.pavelreich.saaremaa:plugin:combine-metrics

