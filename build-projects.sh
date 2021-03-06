# install tools
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
for project in `echo ckjm ck tauksi javadepextractor`;
do
	cd
	git clone https://github.com/jazzmuesli/${project}.git
	cd ~/$project
	mvn -DskipTests install
	mvn assembly:single
done

cd

# build and analyse project
proj_fname=projects.txt
proj_fname=1000Github_urls.txt
proj_count=$(cat $proj_fname | wc -l)
for project in `cat $proj_fname`;
do
	cd
	dir=$(basename $project)
	if [ -d $dir ];
	then
		echo "Directory $dir already exists"
	else
		git clone "https://github.com/${project}.git"
	fi
	cd $dir
	if [ -f jacoco.csv ];
	then
		echo "Jacoco.csv already exists in $dir"
	else	
		timeout 1800s mvn -fn compile test-compile > build0.log
		timeout 1800s java -jar ~/javadepextractor/target/javadepextractor-1.0-SNAPSHOT-jar-with-dependencies.jar . > build0a.log
		timeout 1800s mvn -fn -DtestFailureIgnore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test > build1.log 
		timeout 1800s mvn org.jacoco:jacoco-maven-plugin:LATEST:report > build2.log
		timeout 1800s mvn -DwithHistory -DtimeoutConstant=230  -DoutputFormats=CSV,XML,HTML  org.pitest:pitest-maven:mutationCoverage > build3.log
	fi
done | tqdm --total $proj_count

cd
sh ~/tauksi/generate-git-history.sh
sh ~/tauksi/analyse-test-deps.sh
rm -vf combined-jacoco.csv
python3 combine-files.py
java -classpath ~/tauksi/target/tauksi-1.0-SNAPSHOT-jar-with-dependencies.jar org.pavelreich.saaremaa.ClassMetricsGatherer
zip metrics.zip *.csv
