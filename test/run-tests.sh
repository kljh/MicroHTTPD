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
	host=http://192.168.0.13:8086/
fi
echo host $host


curl -i -X OPTIONS http://localhost:8080/ > OPTIONS.txt
#curl -i -X PROPFIND -H "Depth: 0" ${host} > PROPFIND_depth0_root.txt
#curl -i -X PROPFIND -H "Depth: 1" ${host} > PROPFIND_depth1_root.txt
#curl -i -X PROPFIND -H "Depth: 0" ${host}sdcard/ > PROPFIND_depth0_root_sdcard.txt
#curl -i -X PROPFIND -H "Depth: 1" ${host}sdcard/ > PROPFIND_depth1_root_sdcard.txt

curl -i -X MKCOL ${host}sdcard/test > MKCOL_sdcard_test.txt
curl -i -X MKCOL ${host}sdcard/test/another%20folder > MKCOL_sdcard_test_anotherSPACEfolder.txt
curl -i -X PUT   -d "my prose"   ${host}sdcard/test/prose.txt > PUT_sdcard_test_prose.txt
curl -i -X PUT   -d "my rimes"  ${host}sdcard/test/rimes.txt > PUT_sdcard_test_rimes.txt
curl -i -X MOVE  -H "Destination: /sdcard/test/pouet.txt" ${host}sdcard/test/rimes.txt > MOVE_sdcard_test_pouet.txt
curl -i -X GET   ${host}sdcard/test/prose.txt > GET_sdcard_test_prose.txt
curl -i -X GET   ${host}sdcard/test/pouet.txt > GET_sdcard_test_pouet.txt

#curl -i -X PUT    -u kljh:pioupiou -d @up.txt  http://192.168.0.7:8086/downloads/up/tst/b.txt
#curl -i -X POST   -u kljh:pioupiou -H "Content-Type: multipart/form-data" -H userid=7654 -F "userid=1234" -F "pwd=123" -F "data=@up.txt" -F "load=@up2.txt" http://192.168.0.7:8086/downloads/up/tst/b.txt

curl -i -X PROPFIND -H "Depth: 0" ${host}sdcard/test > PROPFIND_depth0_root_sdcard_test.txt
curl -i -X PROPFIND -H "Depth: 1" ${host}sdcard/test > PROPFIND_depth1_root_sdcard_test.txt

curl -i -X DELETE ${host}sdcard/test > DELETE_root_sdcard_test.txt
curl -i -X PROPFIND -H "Depth: 0" ${host}sdcard/test > PROPFIND_depth0_root_sdcard_test_POSTDELETE.txt
curl -i -X PROPFIND -H "Depth: 1" ${host}sdcard/test > PROPFIND_depth1_root_sdcard_test_POSTDELETE.txt


sed -i '' -e 's/^Date: .*$/Date: --/g' *.txt
sed -i '' -e 's/<d:getlastmodified>.*<\/d:getlastmodified>/<d:getlastmodified\/>/g' *.txt
sed -i '' -e 's/<d:creationdate>.*<\/d:creationdate>/<d:creationdate\/>/g' *.txt


ls -alt

echo git diff -w HEAD -- .

