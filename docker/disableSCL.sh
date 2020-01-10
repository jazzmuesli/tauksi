for i in `grep -lRi 'separateClassLoader = true' .`;
do 
	sed -i '' 's/separateClassLoader = true/separateClassLoader = false/g' $i
done
