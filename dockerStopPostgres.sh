#!/bin/bash -eu
set -o pipefail

docker stop postgres-jmh

# Optionally remove the container.
#docker rm postgres-jmh
