#!/bin/sh

for i in `ls /sf110 | grep ^[0-9]`;
do
	echo $i
	cd /sf110/$i

	mvn -o org.pavelreich.saaremaa:plugin:metrics || exit 1
	mvn -o org.pavelreich.saaremaa:plugin:analyse-testcases || exit 1
	mvn -o org.pavelreich.saaremaa:plugin:combine-metrics || exit 1
done
