#!/bin/sh
# https://github.com/pinterest/ktlint pre-commit hook
# On Linux xargs must be told to do nothing on no input. On MacOS (linux distribution "Darwin") this is default behavior and the xargs flag "--no-run-if-empty" flag does not exists
[ "$(uname -s)" != "Darwin" ] && no_run_if_empty=--no-run-if-empty
git diff --name-only --cached --relative | grep '\.kt[s"]\?$' | xargs $no_run_if_empty ktlint --android --relative
if [ $? -ne 0 ]; then exit 1; fi
