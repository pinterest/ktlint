# !/bin/bash

echo "Serving docs from directory '$(basename "${PWD##*/}")'"
echo ""

mkdocs serve
if [[ $? -ne 0 ]]; then
  echo "Invalid command. Please ensure that 'mkdocs' is installed."
  echo " - If needed install python3"
  echo " - If needed run 'pip install mkdocs'"
  echo "Or run 'docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material'"
fi
