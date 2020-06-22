#!/bin/bash -eu
set -o pipefail

# SQL Server

docker build --no-cache -t sqlserver-jmh -f ./resources/sqlserver/Dockerfile .

docker images -a | grep sqlserver-jmh

docker run -d --name sqlserver-jmh -p 1433:1433 sqlserver-jmh

docker ps | grep sqlserver-jmh
