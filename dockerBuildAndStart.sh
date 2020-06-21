#!/bin/bash -eu
set -o pipefail

docker build --no-cache -t postgres-jmh .

docker images -a | grep postgres-jmh

docker run -d --name postgres-jmh -p 5432:5432 postgres-jmh

docker ps | grep postgres-jmh
