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

echo "run_tests:$run_tests, evo: $evosuite, ignoreChildProjects: $ignoreChildProjects, add_deps: $add_deps"

i=$name

	if [ -f target/ck.ok ];
	then
		echo "ck already present $i"
    else
		mvn -o org.pavelreich.saaremaa:plugin:metrics && touch target/ck.ok
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
	if [ -f target/testability.xml ];
	then
		echo "testability already present $i"
    else
        mvn -o com.google.testability-explorer:testability-mvn-plugin:testability org.pavelreich.saaremaa:plugin:parse-testability
    fi

    if [ -f target/ckjm.ok ];
    then
	    echo "ckjm already present $i"
    else
	    mvn com.github.jazzmuesli:ckjm-mvn-plugin:metrics && touch target/ckjm.ok
    fi
	if [ -f target/metrics.ok ];
	then
		echo "metrics already present $i"
	else
		mvn -DignoreChildProjects=$ignoreChildProjects -o org.pavelreich.saaremaa:plugin:combine-metrics && touch target/metrics.ok
	fi
echo "DONE $i"
