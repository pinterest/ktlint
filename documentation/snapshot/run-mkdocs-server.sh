#!/bin/bash

echo "Run mkdocs server. Terminate with CTRL-C"
echo
docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material
