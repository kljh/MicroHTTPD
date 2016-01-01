#!/bin/bash
#echo argc $#
#echo argv[0] $0

debug=~/Documents/Code/vision.git/
#rm $debug/mysub/.DS_Store
#rm $debug/mysub/._hub-hexagon.png 

if [ "$#" -eq 1 ];	then
	host=$1
else
	host=http://localhost:8080/
fi
echo host $host


curl -i -X OPTIONS http://localhost:8080/ > OPTIONS.txt
curl -i -X PROPFIND -H "Depth: 0" ${host} > PROPFIND_depth0_root.txt
curl -i -X PROPFIND -H "Depth: 1" ${host} > PROPFIND_depth1_root.txt
curl -i -X PROPFIND -H "Depth: 0" ${host}mysub/ > PROPFIND_depth0_root_mysub.txt
curl -i -X PROPFIND -H "Depth: 1" ${host}mysub/ > PROPFIND_depth1_root_mysub.txt

sed -i '' -e 's/^Date: .*$/Date: --/g' *.txt

git diff -w HEAD

