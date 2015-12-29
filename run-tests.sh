#!/bin/bash

curl -i -X OPTIONS http://localhost:8080/ > OPTIONS.txt
curl -i -X PROPFIND http://localhost:8080/ > PROPFIND_depth0_root.txt
