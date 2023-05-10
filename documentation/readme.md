# Documentation

Two versions of the documentation are kept in the 'master' branch:

* The `snapshot` version of the documentation applies to the SNAPSHOT versions of ktlint. Upon the publication of the next official release of ktlint, the `release-latest` version of the documentation is replaced with the `snapshot` version. 
* The `release-latest` version of the documentation applies to the last officially published version of ktlint. Upon the publication of the next official release of ktlint, this version of the documentation is replaced with the `snapshot` version.

Whenever a fix is to be made to the documentation, it should be determined in which version(s) of the documentation is to be fixed. Documentation fixes which only apply to the `SNAPSHOT` version of ktlint may only be fixed in the `snapshot` version of the documentation.

All other kind of documentation fixes most likely needs to be fixed in both the `snapshot` and `release-latest` versions. Only fixing it in `release-latest` may result in the fix being lost upon publication of the next official ktlint version.


Docs can be viewed on the local machine in one of following ways:
* Run script `serve-docs-locally.sh` which starts a docker container running mkdocs
* Run command `mike serve` which requires that `python`, `pip`, `mike` and `mkdocs` have been installed (one time only) before 
