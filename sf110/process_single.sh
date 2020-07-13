#!/bin/sh

ignoreChildProjects=true
run_tests=true
evosuite=false
add_deps=true
name="unknown"
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
	n)
	    name=shift
	    ;;
        *)
            usage
            ;;
    esac
done

build_system=""

if [ -f pom.xml ];
then
	build_system=maven
else if [ -f build.gradle ];
then
	build_system=gradle
else
	echo "There is no build.gradle nor pom.xml, can't build"
	exit 1
fi
	
echo "build_system: $build_system; run_tests:$run_tests, evo: $evosuite, ignoreChildProjects: $ignoreChildProjects, add_deps: $add_deps"

i=$name

	/usr/local/bin/j8
	
	if [ -f target/ck.ok ];
	then
		echo "build already present $i"
    elif [ "$build_system" == "mvn" ];
    then
		mvn -DskipTests clean install  && touch target/build.ok
    elif [ "$build_system" == "gradle" ];
    then
    	grep -i jdk11 build.gradle && /usr/local/bin/j11
		./gradlew build -x test && touch target/build.ok
    fi

	if [ -f target/ck.ok ];
	then
		echo "ck already present $i"
    elif [ "$build_system" == "mvn" ];
    then
		mvn -o org.pavelreich.saaremaa:plugin:metrics && touch target/ck.ok
    elif [ "$build_system" == "gradle" ];
    then
		./gradlew ck && touch target/ck.ok
    fi
	if [ -f target/testan.ok ];
	then
		echo "testan already present $i"
    else
        mvn -o org.pavelreich.saaremaa:plugin:testan && touch target/testan.ok
    fi
	if [ -f target/testcases.ok ];
	then
		echo "testcases already present $i"
    else
		mvn -o org.pavelreich.saaremaa:plugin:analyse-testcases && touch target/testcases.ok
    fi
	if [ -f target/testability.ok ];
	then
		echo "testability already present $i"
    elif [ "$build_system" == "mvn" ];
    then
        mvn -o com.google.testability-explorer:testability-mvn-plugin:testability org.pavelreich.saaremaa:plugin:parse-testability && touch target/testability.ok
    elif [ "$build_system" == "gradle" ];
    then
		./gradlew --offline testability && touch target/ck.ok
    fi

    if [ -f target/ckjm.ok ];
    then
	    echo "ckjm already present $i"
    elif [ "$build_system" == "mvn" ];
    then
		mvn test-compile com.github.jazzmuesli:ckjm-mvn-plugin:metrics && touch target/ckjm.ok
    elif [ "$build_system" == "gradle" ];
    then
		./gradlew --offline ckjm && ./gradlew --offline ckjmRebuild && touch target/ckjm.ok
    fi
    
	if [ -f target/metrics.ok ];
	then
		echo "metrics already present $i"
    elif [ "$build_system" == "mvn" ];
    then
		mvn -DignoreChildProjects=$ignoreChildProjects -o org.pavelreich.saaremaa:plugin:combine-metrics && touch target/metrics.ok
    elif [ "$build_system" == "gradle" ];
    then
    	./gradlew --offline combine-metrics && touch target/metrics.ok
	fi
echo "DONE $i"
