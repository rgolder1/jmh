#!/bin/bash -eu
set -o pipefail

docker stop postgres-jmh

docker rm postgres-jmh
