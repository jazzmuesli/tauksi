#!/bin/sh

echo "params:$*"
ignoreChildProjects=true
run_tests=true
evosuite=false
add_deps=true
usage() { echo "Usage: $0 [-c -r -e -a]" 1>&2; exit 1; }

while getopts "crea" o; do
    case "${o}" in
        c)
            ignoreChildProjects=false
            ;;
        a)
            add_deps=false
            ;;
        r)
            run_tests=false
            ;;
	e)
	    evosuite=true
	    ;;
        *)
            usage
            ;;
    esac
done

echo "run_tests:$run_tests, evo: $evosuite, ignoreChildProjects: $ignoreChildProjects, add_deps: $add_deps"
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/; export PATH=$JAVA_HOME/bin:$PATH
if [ "$add_deps" = "true" ];
then
	mvn -Drat.skip=true -Ddependencies="org.evosuite:evosuite-standalone-runtime:LATEST:test" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
	mvn -Drat.skip=true -Ddependencies="junit:junit:4.12:compile" -Doverwrite=true org.pavelreich.saaremaa:plugin:add-dependency
fi
mvn -Drat.skip=true test-compile || exit 1
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:metrics
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:analyse-testcases

if [ "$evosuite" = "true" ];
then
	mvn -Drat.skip=true -Dsandbox_mode=OFF -Duse_separate_classloader=false -DmemoryInMB=4000 -Dcores=6 -DtimeInMinutesPerClass=2 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate
	mvn -Drat.skip=true -Dsandbox_mode=OFF -Duse_separate_classloader=false org.evosuite.plugins:evosuite-maven-plugin:LATEST:export
	mvn -Drat.skip=true test-compile || exit 1
	mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:metrics
fi
grep -lRi org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED . | xargs sed -i 's/org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED/org.evosuite.runtime.sandbox.Sandbox.SandboxMode.OFF/g'
grep -lRi 'separateClassLoader = true' . | xargs sed -i 's/separateClassLoader = true/separateClassLoader = false/g'
mvn -Drat.skip=true com.google.testability-explorer:testability-mvn-plugin:testability
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:parse-testability
if [ "$run_tests" = "true" ];
then
	mvn -Drat.skip=true -DtestExtractor=MetricsTestExtractor -DpoolSize=5 org.pavelreich.saaremaa:plugin:ctest
else
	echo "Not running tests"
fi
mvn -Drat.skip=true org.pavelreich.saaremaa:plugin:analyse-testcases
mvn -Drat.skip=true -DignoreChildProjects=$ignoreChildProjects org.pavelreich.saaremaa:plugin:combine-metrics

