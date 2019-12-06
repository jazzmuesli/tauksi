#!/bin/sh

echo "params:$*"
run_tests=true
if [[ $* == *"skip_ctest"* ]]; then
  echo "skip ctest"
  run_tests=false
fi
echo "run_tests:$run_tests"
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/; export PATH=$JAVA_HOME/bin:$PATH
mvn -Drat.skip=true test-compile
#mvn -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
#mvn compile
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:metrics
#mvn -Dsandbox_mode=OFF -Duse_separate_classloader=false -DmemoryInMB=4000 -Dcores=6 -DtimeInMinutesPerClass=1 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate
#mvn -Dsandbox_mode=OFF -Duse_separate_classloader=false org.evosuite.plugins:evosuite-maven-plugin:LATEST:export
#mvn -Drat.skip=true  -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 -fae -Dmaven.test.failure.ignore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:metrics
mvn -Drat.skip=true com.google.testability-explorer:testability-mvn-plugin:testability
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:parse-testability
if [[ "$run_tests" == "true" ]];
then
	mvn -Drat.skip=true -DseqTestMethods=false -DshuffleTests=true -DinterceptorEnabled=false -Dtimeout=165 org.pavelreich.saaremaa:plugin:ctest
else
	echo "Not running tests"
fi
#mvn -Dsandbox_mode=OFF
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:analyse-testcases
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:combine-metrics

