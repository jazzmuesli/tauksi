for i in `grep -lRi 'separateClassLoader = false'`;
do 
	sed -i '' 's/separateClassLoader = false/separateClassLoader = false/g' $i
done
