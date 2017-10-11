#!/bin/sh
# https://github.com/shyiko/ktlint pre-commit hook
git diff --name-only --cached --relative | grep '\.kts\?$' | xargs ktlint --relative .
if [ $? -ne 0 ]; then exit 1; fi
