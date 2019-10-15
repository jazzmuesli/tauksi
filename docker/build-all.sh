nohup mongod --bind_ip_all &
mkdir /projects
mkdir /logs
for i in `cat /root/tauksi/docker/projects.txt | head -1`;
do
	cd /projects && git clone $i
done

#export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
java -version 2>&1 | tee /logs/java.version
javac -version 2>&1 | tee /logs/javac.version

for i in `ls /projects`;
do 
	echo $i
	cd /projects/$i
	git clean -f -d	
	root="/logs/`pwd | tr '/' '_'`"
	mkdir -p $root
	mvn org.jacoco:jacoco-maven-plugin:LATEST:prepare-agent install | tee $root/inst.txt
	mvn com.google.testability-explorer:maven-testability-plugin:testability | tee $root/testability.txt
	mvn -DmemoryInMB=9000 -Dcores=6 -DtimeInMinutesPerClass=2 org.evosuite.plugins:evosuite-maven-plugin:LATEST:generate org.evosuite.plugins:evosuite-maven-plugin:LATEST:export | tee $root/evosuite.txt
	mvn org.pavelreich.saaremaa:plugin:metrics | tee $root/metr.txt
	mvn -DseqTestMethods=false -DshuffleTests=true -DinterceptorEnabled=false -Dtimeout=15 org.pavelreich.saaremaa:plugin:ctest | tee $root/ctest.txt
	mvn -Dverbose=true -DwithHistory -DoutputFormats=CSV,XML,HTML  org.pitest:pitest-maven:mutationCoverage | tee $root/pitest.txt
done
