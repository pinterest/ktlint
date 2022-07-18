# Build & test documentation on local machine

The documentation of ktlint is served with [mkdocs-material](https://squidfunk.github.io/mkdocs-material/creating-your-site/#advanced-configuration). For full documentation visit [mkdocs.org](https://www.mkdocs.org).

To build and test documentation on your local development machine, follow steps below:

* In IntelliJ IDEA
  * Open `Preferences`
  * Search for `JSON Schema mappings`
  * Add new schema for url `https://squidfunk.github.io/mkdocs-material/schema.json` and add file `mkdocs.yml` for this url.
* Pull docker image
  ```shell
  $ docker pull squidfunk/mkdocs-material
  ```
* Start mkdocs server from root of project (e.g. from same directory where file mkdocs.yml is located) 
  ```shell
  docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material
  ```
* Visit page `http://0.0.0.0:8000/` in your browser.
* Edit the documentation and explicitly save the file. The mkdocs server refreshes its cached and the current page in the browser is automatically refreshed.
