nohup mongod --bind_ip_all &
mkdir /projects
for i in `cat /root/tauksi/docker/projects.txt`;
do
	cd /projects && git clone $i
done

export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
for i in `ls /projects`;
do 
	echo $i
	cd /projects/$i
	git clean -f -d	
	mvn install | tee inst.txt
	mvn org.pavelreich.saaremaa:plugin:metrics | tee metr.txt
	mvn -DseqTestMethods=false -DshuffleTests=true -DinterceptorEnabled=false -Dtimeout=15 org.pavelreich.saaremaa:plugin:ctest | tee ctest.txt
done
