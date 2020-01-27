#!/bin/sh


for i in `ls /sf110 | grep ^[0-9]`;
do
	if [ -f /sf110/$i/target/metrics.csv ];
	then
		echo "metrics already present $i"
	else
		sem -j+0 "cd /sf110/$i && sh process_single.sh $i"
	fi
done
sem --wait
cd /sf110 && jar -cvf metrics.zip `find . -name metrics.csv -or -name result.json -or -name class.csv -or -name method.csv`