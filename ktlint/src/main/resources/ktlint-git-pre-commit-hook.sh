#!/bin/sh
# https://github.com/shyiko/ktlint pre-commit hook
export hasKt=$(git diff --name-only --cached --relative | grep '\.kt[s"]\?$')

if [ ! -z "$hasKt" ]; then
	tput setaf 2; tput bold; echo "\n🔍  Ktlint check in progress\n"; tput sgr0
	
	tput setaf 1; tput bold; git diff --name-only --cached --relative | grep '\.kt[s"]\?$' | xargs ./.ktlint --relative .
	
	if [ $? -ne 0 ]; then
    tput setaf 1; tput bold; echo "\n✘ Kotlin lint errors found, fix and recommit (run \"./.ktlint -F\" to format simple lint errors) \n"; tput sgr0
	  exit 1
	fi
	
	tput setaf 2; tput bold; echo "✔ Ktlint passed\n"; tput sgr0
fi
