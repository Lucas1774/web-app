#!/bin/bash

cd src/main/resources/python/finbert || exit
docker rm -f finbert
docker build -t finbert .
docker run -d -p 8082:80 --name finbert finbert:latest
