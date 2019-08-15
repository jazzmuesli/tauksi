ps axf | awk '/build-all|java|tee/ { print $1}' | xargs kill
sleep 1
rm -vf hohup.out
nohup sh build-all.sh &
