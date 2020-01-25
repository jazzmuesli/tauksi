#!/bin/sh


for i in `ls /sf110 | grep ^[0-9]`;
do
	if [ -f /sf110/$i/target/metrics.csv ];
	then
		echo "metrics already present $i"
	else
		sem -j+0 "sh process_single.sh $i"
	fi
done
sem --wait
