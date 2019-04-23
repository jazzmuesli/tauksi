for i in `find . -name .git`;
do
	dir=$(dirname $i)
	cd /root/$dir
	echo "dir:$dir"
	java -classpath ~/tauksi/target/tauksi-1.0-SNAPSHOT-jar-with-dependencies.jar org.pavelreich.saaremaa.GitHistory
done

