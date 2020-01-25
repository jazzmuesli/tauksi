ps -axf | awk '/java|perl/ { print $1}'  | xargs kill
