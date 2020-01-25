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
	if [ -f /sf110/$i/target/testability.csv ];
        then
                echo "testan already present $i"
        else
		# mvn -Doverwrite=true -DdepDir=lib/  org.pavelreich.saaremaa:plugin:add-dependency
                sem -j+0 "cd /sf110/$i && mvn -o org.pavelreich.saaremaa:plugin:testan com.google.testability-explorer:testability-mvn-plugin:testability org.pavelreich.saaremaa:plugin:parse-testability"
        fi
	if [ -f /sf110/$i/target/metrics1.csv ];
	then
		echo "already present $i"
	else
		sem -j+0 "cd /sf110/$i && mvn -o org.pavelreich.saaremaa:plugin:metrics org.pavelreich.saaremaa:plugin:analyse-testcases org.pavelreich.saaremaa:plugin:combine-metrics"
	fi
done
sem --wait
