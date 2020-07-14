for i in $(find /projects -maxdepth 1 -type d ); do echo $i && cd $i && /root/tauksi/sf110/process_single.sh ; done | tee /projects/deb
