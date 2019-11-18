#!/bin/sh
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
mvn compile
mvn -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
mvn compile
mvn org.pavelreich.saaremaa:plugin:metrics
mvn -DmemoryInMB=4000 -Dcores=6 -DtimeInMinutesPerClass=1 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate
mvn org.evosuite.plugins:evosuite-maven-plugin:LATEST:export
mvn -fae -Dmaven.test.failure.ignore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test
mvn org.pavelreich.saaremaa:plugin:analyse-testcases
mvn org.pavelreich.saaremaa:plugin:combine-metrics

