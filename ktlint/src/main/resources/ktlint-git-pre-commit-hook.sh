#!/bin/sh
# https://github.com/pinterest/ktlint pre-commit hook
# On Linux xargs must be told to do nothing on no input but this is the default on Macs
[ "$(uname -s)" != "Darwin" ] && no_run=--no-run-if-empty
git diff --name-only --cached --relative | grep '\.kt[s"]\?$' | xargs $no_run ktlint --relative
if [ $? -ne 0 ]; then exit 1; fi
