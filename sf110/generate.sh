# wget http://www.evosuite.org/files/SF110-20130704-src.zip

P=`pwd`
for i in `ls /sf110 | grep ^[0-9]`;
do
cat $P/template-pom.xml | sed "s/ARTIFACT_ID/$i/g" > /sf110/$i/pom.xml
done
