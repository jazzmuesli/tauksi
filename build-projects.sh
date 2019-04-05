for project in `cat projects.txt`;
do
	cd
	git clone "https://github.com/${project}.git"
	cd $(basename $project)
	mvn -fn -DtestFailureIgnore=true org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent test 
	mvn org.jacoco:jacoco-maven-plugin:LATEST:report
done
python3 combine-jacoco.py

