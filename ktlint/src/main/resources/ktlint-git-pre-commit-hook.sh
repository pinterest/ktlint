#!/bin/sh
# https://github.com/pinterest/ktlint pre-commit hook
git diff --name-only --cached --relative | grep '\.kt[s"]\?$' | xargs --no-run-if-empty ktlint --relative
if [ $? -ne 0 ]; then exit 1; fi
