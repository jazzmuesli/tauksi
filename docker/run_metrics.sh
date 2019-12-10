#!/bin/sh

echo "params:$*"
ignoreChildProjects=true
run_tests=true
evosuite=false
if [[ $* == *"skip_ctest"* ]]; then
  echo "skip ctest"
  run_tests=false
fi

if [[ $* == *"evo"* ]]; then
  echo "run evosuite"
  evosuite=true
fi

if [[ $* == *"ignore_child_projects"* ]]; then
  echo "ignore child projects"
  ignoreChildProjects=false
fi
echo "run_tests:$run_tests"
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/; export PATH=$JAVA_HOME/bin:$PATH
mvn -Drat.skip=true test-compile
#mvn -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
#mvn compile
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:metrics

if [[ "$evosuite" == "true" ]];
then
	mvn -Drat.skip=true -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test;junit:junit:4.12:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
	mvn -Drat.skip=true -Dsandbox_mode=OFF -Duse_separate_classloader=false -DmemoryInMB=4000 -Dcores=6 -DtimeInMinutesPerClass=1 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate
	mvn -Drat.skip=true -Dsandbox_mode=OFF -Duse_separate_classloader=false org.evosuite.plugins:evosuite-maven-plugin:LATEST:export
	grep -lRi org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED . | xargs sed -ibak 's/org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED/org.evosuite.runtime.sandbox.Sandbox.SandboxMode.OFF/g'
	mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:metrics
fi
#mvn -Drat.skip=true  -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8 -fae -Dmaven.test.failure.ignore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test
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
mvn -Drat.skip=true -DignoreChildProjects=$ignoreChildProjects org.pavelreich.saaremaa:plugin:combine-metrics

