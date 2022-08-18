# Release testing

Before releasing a new version of KtLint, the release candidate is tested on a selection of open source projects to improve the quality of the release.

## Setup infrastructure

1. Create a directory in which the projects are grouped:
   ```shell
   mkdir sample-projects
   cd sample-projects
   ```
2. In the `sample-projects` directory, create a shell command script `exec-in-each-project.sh` with content below. This script is used to run the same command in each of the sample projects:
   ```shell
   # Examples of usage
   #
   #   Outstanding changes per project
   #     ./exec-in-each-project.sh "git status"
   #
   #   Rollback outstanding changes per project
   #     ./exec-in-each-project.sh "git reset --hard"
   #
   #   Run ktlint standard rules
   #     ktlint --version && ktlint "**/*.kt" -v --relative -F
   #
   #   Commit changes of standard rules:
   #     ./exec-in-each-project.sh "git commit -m \"ktlint (0.43.2) -F\""

   find . -maxdepth 1 -type d \( ! -name . \) -exec bash -c "cd '{}' && pwd && $@" \;
   ```
3. Clone the repositories of the sample projects (feel free to change this selection):
   ```shell
   git clone https://github.com/google/iosched.git
   git clone https://github.com/android/uamp.git
   git clone https://github.com/siempredelao/StaticAnalysisTest
   git clone https://github.com/Kotlin/kotlinx.coroutines.git
   git clone https://github.com/square/okhttp.git
   git clone https://github.com/detekt/detekt.git
   git clone https://github.com/ExpediaGroup/graphql-kotlin.git
   git clone https://github.com/Kotlin/kotlinx.serialization.git
   ```
4. Create an alias or script to run the latest development-version of ktlint (note that this script will print the version and the datetime of compilation as reference to which version is used):
   ```shell
   alias ktlint-dev="echo 'ktlint-dev version:';ls -aslh ~/git/ktlint/ktlint/build/run/ktlint;echo ' ';~/git/ktlint/ktlint/build/run/ktlint $@"
   ```
   Note that `~/git/ktlint` is the directory in which the ktlint project is checked out and that `~/git/ktlint/ktlint` refers to the `ktlint` CLI-module.

## Cleanup from previous testing

1. Reset the repositories of the sample projects:
   ```shell
   ./exec-in-each-project.sh "git reset --hard origin"
   ```
2. Optionally, pull latest changes of the sample projects:
   ```shell
   ./exec-in-each-project.sh "git pull"
   ```
3. Remove file `baseline.xml`

## Testing a new release

Pre-requisites:
* All changes have been merged to master.
* Latest version of master has been pulled to the local machine.
* The latest version of master has been build on the local machine. And it is verified that the datetime of `ktlint-dev` (more or less) matches with the datetime of this compilation.
   ```shell
   ktlint-dev --version
   ```

Formatting projects in which ktlint is not used may result in a huge amount of fixes. The main focus of this test is to see what the effects are when upgrading ktlint in a project already formatted with latest released ktlint version.

1. Format the sample projects with the *latest released* ktlint version:
   ```shell
   ktlint -F --experimental --relative # Do not call this command via the "./exec-in-each-project.sh" script.
   ```
   Note: Ignore all output as this is the old version!
2. Commit changes:
   ```shell
   ./exec-in-each-project.sh "git add --all && git commit -m \"Format with ktlint (xx.yy.zz) -F --experimental\""
   ```
3. Create a baseline file with the *latest released* ktlint version:
   ```shell
   ktlint --experimental --baseline=baseline.xml --relative # Do not call this command via the "./exec-in-each-project.sh" script as we want one combined baseline.xml file for all projects.
   ```
4. Rerun previous command. As all violations were stored in file `baseline.xml` it is to be expected that no violations will be reported anymore. However, if violations are reported, this should be investigated and fixed before proceeding as otherwise you might falsely interpret them as caused by changed in the new release. You might want to add some `.editorconfig` configuration to suppress violations which can not be autocorrected. Commit your changes:
   ```shell
   ./exec-in-each-project.sh "git add --all && git commit -m \"Fix for some reason\""
   ```
   Repeat until no new violations are reported.
5. Lint with *latest development* version:
   ```shell
   ktlint-dev --experimental --baseline=baseline.xml --relative # Do not call this command via the "./exec-in-each-project.sh" script as we want to use the one combined baseline.xml file for all projects.
   ```
   Inspect the output roughly (detailed inspection is done when formatting):
   * Is the amount of logging messages comparable to before? If not, are the changes intended?
   * Are violations related to rules that have actually been added or changed?
6. Format with *latest development* version:
   ```shell
   ktlint-dev -F --experimental --baseline=baseline.xml --relative # Do not call this command via the "./exec-in-each-project.sh" script as we want to use the one combined baseline.xml file for all projects.
   ```
   Inspect the output carefully:
    * If you see an error like below, then this version obviously may *not* be released. It is best to fix this error before continuing with testing and validating!
      ```plain
      Internal Error (...) in file '...' at position '0:0. Please create a ticket at https://github.com/pinterest/ktlint/issues ...
      ```
   * Ideally, no violations are shown. This means that all violations have been autocorrected.
   * Violations which could not be autocorrected should be validated for correctness but do not block the release as most likely this is intended behavior.
   * If a violation is shown which is not marked as being "can not be autocorrected" this means that during autocorrect of another violation a new violations has been introduced. This should be fixed before releasing especially when the next format introduces the original violation again which of course would result in an endless loop.
7. Inspect all fixed violations, Of course inspection similar violations tens of times does not make sense. At least check different types of violations a couple of times. Commit changes which do not need to be inspected again:
   ```shell
   ./exec-in-each-project.sh "git add --all && git commit -m \"Fixed with latest development version\""
   ```
8. Rerun lint with *latest development* version:
   ```shell
   ktlint-dev --experimental --baseline=baseline.xml --relative # Do not call this command via the "./exec-in-each-project.sh" script as we want to use the one combined baseline.xml file for all projects.
   ```
   No violations should be expected.
