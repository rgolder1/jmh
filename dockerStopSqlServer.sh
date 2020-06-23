#!/bin/bash -eu
set -o pipefail

docker stop sqlserver-jmh

docker rm sqlserver-jmh
