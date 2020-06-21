#!/bin/bash -eu
set -o pipefail

# Postgres

docker build --no-cache -t postgres-jmh -f ./resources/postgres/Dockerfile .

docker images -a | grep postgres-jmh

docker run -d --name postgres-jmh -p 5432:5432 postgres-jmh

docker ps | grep postgres-jmh
