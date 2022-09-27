#!/bin/sh

# <https://github.com/pinterest/ktlint> pre-commit hook

git diff --name-only -z --cached --relative -- '*.kt' '*.kts' | ktlint --android --relative --patterns-from-stdin=''
