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
	#host=http://192.168.0.13:8086/
fi
echo host $host

user=whoami
pwd=whatiknow
cred="-u ${user}:${pwd} --digest"

curl -i ${cred} -X OPTIONS http://localhost:8080/ > OPTIONS.txt
curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host} > PROPFIND_depth0_root.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host} > PROPFIND_depth1_root.txt
curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host}sdcard/ > PROPFIND_depth0_root_sdcard.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host}sdcard/ > PROPFIND_depth1_root_sdcard.txt


curl -i ${cred} -X MKCOL ${host}sdcard/test > MKCOL_sdcard_test.txt
curl -i ${cred} -X MKCOL ${host}sdcard/test/another%20folder > MKCOL_sdcard_test_anotherSPACEfolder.txt
curl -i ${cred} -X PUT   --data-binary "my prose" -H "Content-type: text/plain"  ${host}sdcard/test/prose.txt > PUT_sdcard_test_prose.txt
curl -i ${cred} -X PUT   --data-binary "my rimes" -H "Content-type: text/plain"  ${host}sdcard/test/rimes.txt > PUT_sdcard_test_rimes.txt
curl -i ${cred} -X MOVE  -H "Destination: /sdcard/test/pouet.txt" ${host}sdcard/test/rimes.txt > MOVE_sdcard_test_pouet.txt
curl -i ${cred} -X GET   ${host}sdcard/test/prose.txt > GET_sdcard_test_prose.txt
curl -i ${cred} -X GET   ${host}sdcard/test/pouet.txt > GET_sdcard_test_pouet.txt

#curl -i ${cred} -X PUT    -u kljh:pioupiou -d @up.txt  http://192.168.0.7:8086/downloads/up/tst/b.txt
#curl -i ${cred} -X POST   -u kljh:pioupiou -H "Content-Type: multipart/form-data" -H userid=7654 -F "userid=1234" -F "pwd=123" -F "data=@up.txt" -F "load=@up2.txt" http://192.168.0.7:8086/downloads/up/tst/b.txt

curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host}sdcard/test > PROPFIND_depth0_root_sdcard_test.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host}sdcard/test > PROPFIND_depth1_root_sdcard_test.txt

curl -i ${cred} -X DELETE ${host}sdcard/test > DELETE_root_sdcard_test.txt
curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host}sdcard/test > PROPFIND_depth0_root_sdcard_test_POSTDELETE.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host}sdcard/test > PROPFIND_depth1_root_sdcard_test_POSTDELETE.txt


curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host}photos/ > PROPFIND_depth0_root_photos_.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host}photos/ > PROPFIND_depth1_root_photos_.txt
curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host}photos/MISC > PROPFIND_depth0_root_photos_misc.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host}photos/MISC > PROPFIND_depth1_root_photos_misc.txt
curl -i ${cred} -X PROPFIND -H "Depth: 0" ${host}photos/Camera > PROPFIND_depth0_root_photos_camera.txt
curl -i ${cred} -X PROPFIND -H "Depth: 1" ${host}photos/Camera > PROPFIND_depth1_root_photos_camera.txt

sed -i '' -e 's/^Date: .*$/Date: --/g' *.txt
sed -i '' -e 's/<d:getlastmodified>.*<\/d:getlastmodified>/<d:getlastmodified\/>/g' *.txt
sed -i '' -e 's/<d:creationdate>.*<\/d:creationdate>/<d:creationdate\/>/g' *.txt


ls -alt

echo git diff -w HEAD -- .

