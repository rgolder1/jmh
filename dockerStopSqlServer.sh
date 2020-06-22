#!/bin/bash -eu
set -o pipefail

docker stop sqlserver-jmh

# Optionally remove the container.
#docker rm sqlserver-jmh
