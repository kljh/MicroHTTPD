#!/bin/bash

curl -i -X OPTIONS http://localhost:8080/ > OPTIONS.txt
curl -i -X PROPFIND -H "Depth: 0" http://localhost:8080/ > PROPFIND_depth0_root.txt
curl -i -X PROPFIND -H "Depth: 1" http://localhost:8080/ > PROPFIND_depth1_root.txt

git diff HEAD

