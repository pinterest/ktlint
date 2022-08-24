#!/bin/sh

# <https://github.com/pinterest/ktlint> pre-push hook

git diff --name-only -z HEAD "origin/$(git rev-parse --abbrev-ref HEAD)" -- '*.kt' '*.kts' | ktlint --android --relative --patterns-from-stdin=''
