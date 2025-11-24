#!/usr/bin/env bash
set -euo pipefail

echo "Building coderunner/gcc:12..."
docker build -f executor/images/gcc/Dockerfile -t coderunner/gcc:12 executor/images/gcc

echo "Building coderunner/java:17..."
docker build -f executor/images/java/Dockerfile -t coderunner/java:17 executor/images/java

echo "Building coderunner/python:3.11..."
docker build -f executor/images/python/Dockerfile -t coderunner/python:3.11 executor/images/python

echo "Done."
