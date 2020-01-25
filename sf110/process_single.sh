#!/bin/sh

$i=$1
cd /sf110/$i || exit 1
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
		echo "ck already present $i"
    else
                mvn -o org.pavelreich.saaremaa:plugin:analyse-testcases && touch target/testcases.ok
    fi
	if [ -f target/testability.xml ];
	then
		echo "testability already present $i"
    else
        mvn -o com.google.testability-explorer:testability-mvn-plugin:testability org.pavelreich.saaremaa:plugin:parse-testability
    fi
	if [ -f target/metrics.csv ];
	then
		echo "metrics already present $i"
	else
		mvn -o org.pavelreich.saaremaa:plugin:combine-metrics
	fi
echo "DONE $i"