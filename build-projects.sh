# install tools
for project in `echo ckjm ck tauksi`;
do
	cd
	git clone https://github.com/jazzmuesli/${project}.git
	cd ~/$project
	mvn -DskipTests install
	mvn assembly:single
done

cd

# build and analyse project
for project in `cat projects.txt`;
do
	cd
	git clone "https://github.com/${project}.git"
	cd $(basename $project)
	timeout 1800s mvn -fn -DtestFailureIgnore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test 
	timeout 1800s mvn org.jacoco:jacoco-maven-plugin:LATEST:report
	timeout 1800s mvn -DwithHistory package org.pitest:pitest-maven:mutationCoverage
done
cd
python3 combine-jacoco.py
java -classpath ~/tauksi/target/tauksi-1.0-SNAPSHOT-jar-with-dependencies.jar org.pavelreich.saaremaa.ClassMetricsGatherer
zip metrics.zip *.csv
