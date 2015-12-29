#!/bin/bash

curl -i -X OPTIONS http://localhost:8080/ > OPTIONS.txt
curl -i -X PROPFIND -H "Depth: 0" http://localhost:8080/ > PROPFIND_depth0_root.txt
curl -i -X PROPFIND -H "Depth: 1" http://localhost:8080/ > PROPFIND_depth1_root.txt
curl -i -X PROPFIND -H "Depth: 0" http://localhost:8080/mysub/ > PROPFIND_depth0_root_mysub.txt
curl -i -X PROPFIND -H "Depth: 1" http://localhost:8080/mysub/ > PROPFIND_depth1_root_mysub.txt

git diff HEAD

