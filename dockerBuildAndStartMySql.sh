#!/bin/bash -eu
set -o pipefail

# MySQL

docker build -t mysql-jmh -f ./resources/mysql/Dockerfile .

docker images -a | grep mysql-jmh

docker run -d --name mysql-jmh -e MYSQL_ROOT_PASSWORD=mysql -p 3306:3306 mysql-jmh

docker ps | grep mysql-jmh

