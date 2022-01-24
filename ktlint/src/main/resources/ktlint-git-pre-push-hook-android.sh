#!/bin/sh
# https://github.com/pinterest/ktlint pre-push hook
git diff --name-only HEAD origin/$(git rev-parse --abbrev-ref HEAD) | grep '\.kt[s"]\?$' | xargs ktlint --android --relative
if [ $? -ne 0 ]; then exit 1; fi
