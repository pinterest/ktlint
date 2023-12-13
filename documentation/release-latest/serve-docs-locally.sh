# !/bin/bash

echo "Serving docs from directory '$(basename "${PWD##*/}")'"
echo ""

python3 -m mkdocs serve
if [[ $? -ne 0 ]]; then
  echo "Invalid command. Please ensure that 'mkdocs' is installed."
  echo " - If needed install python3"
  echo " - If needed run 'pip3 install mkdocs'"
  echo " - If needed run 'pip3 install mkdocs-material'"
  echo "Or run 'docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material'"
fi
