#!/bin/bash -eu
set -o pipefail

docker stop mysql-jmh

docker rm mysql-jmh


