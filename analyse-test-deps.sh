for i in `find . -type d -name java | grep test/java$`;
do
	cd ~/$i
	java -classpath ~/tauksi/target/tauksi-1.0-SNAPSHOT-jar-with-dependencies.jar org.pavelreich.saaremaa.testdepan.TestFileProcessor .
done

