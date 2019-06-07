R=$(pwd)
#for i in `find  . -name measure.sh`; do echo $i && cd $R && cd $(dirname $i) && sh measure.sh; done
for i in `comm -23 <(find . -name measure.sh | xargs dirname | sort) <(find . -name coverageByMethod.csv | xargs dirname | sort)`;
do
	echo "dir:$i"
	cd $R
	cd $i
	sh measure.sh
done
