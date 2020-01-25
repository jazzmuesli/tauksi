#!/bin/sh

work() {
	i=$1
	echo $i
        cd /sf110/$i

        mvn -o org.pavelreich.saaremaa:plugin:metrics || exit 1
        mvn -o org.pavelreich.saaremaa:plugin:analyse-testcases || exit 1
        mvn -o org.pavelreich.saaremaa:plugin:combine-metrics || exit 1
}
for i in `ls /sf110 | grep ^[0-9]`;
do
	if [ -f /sf110/$i/target/ck.ok ];
	then
		echo "ck already present $i"
    else
                sem -j+0 "cd /sf110/$i && mvn -o org.pavelreich.saaremaa:plugin:metrics && touch /sf110/$i/target/ck.ok"
    fi
	if [ -f /sf110/$i/target/testan.ok ];
	then
		echo "testan already present $i"
    else
                sem -j+0 "cd /sf110/$i && mvn -o org.pavelreich.saaremaa:plugin:testan && touch /sf110/$i/target/testan.ok"
    fi
	if [ -f /sf110/$i/target/testcases.ok ];
	then
		echo "ck already present $i"
    else
                sem -j+0 "cd /sf110/$i && mvn -o org.pavelreich.saaremaa:plugin:analyse-testcases && touch /sf110/$i/target/testcases.ok"
    fi
	if [ -f /sf110/$i/target/testability.xml ];
	then
		echo "testability already present $i"
    else
                sem -j+0 "cd /sf110/$i && mvn -o com.google.testability-explorer:testability-mvn-plugin:testability org.pavelreich.saaremaa:plugin:parse-testability"
    fi
	if [ -f /sf110/$i/target/metrics.csv ];
	then
		echo "metrics already present $i"
	else
		sem -j+0 "cd /sf110/$i && mvn -o org.pavelreich.saaremaa:plugin:combine-metrics"
	fi
done
sem --wait
