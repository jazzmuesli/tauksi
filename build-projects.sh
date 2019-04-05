# install tools
for project in `echo ckjm ck tauksi`;
do
	git clone https://github.com/jazzmuesli/${project}.git
	cd ~/$project
	mvn install
done

# build and analyse project
for project in `cat projects.txt`;
do
	cd
	git clone "https://github.com/${project}.git"
	cd $(basename $project)
	mvn -fn -DtestFailureIgnore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test 
	mvn org.jacoco:jacoco-maven-plugin:LATEST:report
done
python3 combine-jacoco.py

