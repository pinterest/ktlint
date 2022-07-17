!!! note Command Line usage
    If you don't plan to use `ktlint`'s command line interface then you can skip this section.

## Download and verification

### Download manually from github

All releases of `ktlint` can be downloaded from the [releases](https://github.com/pinterest/ktlint/releases) page.

### Download using curl

A particular version of `ktlint` can be downloaded with next command which also changes the file to an executable in directory `/usr/local/bin`:

```sh title="Download"
curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.46.0/ktlint && chmod a+x ktlint && sudo mv ktlint /usr/local/bin/
```

!!! tip "Curl not installed or behind proxy"
    If you don't have curl installed - replace `curl -sL` with `wget -qO-`.  
    If you are behind a proxy see - [curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / [wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. Usually simple:  
    ```shell
    http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...
    ```

### Verification of download

`ktlint.asc` contains PGP signature which you can verify with:

```sh title="Verify releases 0.32.0 and above"
curl -sS https://keybase.io/ktlint/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc
```

```sh title="Verify releases up through 0.31.0"
curl -sS https://keybase.io/shyiko/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc
```

### Package managers

`ktlint` can be installed via several OS specific package managers.

Install with [brew on maxOC](https://brew.sh/) or [Homebrew on Linux](https://docs.brew.sh/Homebrew-on-Linux)
```sh
brew install ktlint
```

Install with [MacPorts](https://www.macports.org/)
```sh
port install ktlint
```

On Arch Linux install package [ktlint <sup>AUR</sup>](https://aur.archlinux.org/packages/ktlint/).

## Command line usage

A good starting point is to read the help page:

```shell title="Get help about all available commands"
ktlint --help
```

When no arguments are specified, the style of all Kotlin files (ending with '.kt' or '.kts') inside the current dir (recursively) are validated with the rules from the [standard ruleset](https://ktlint.github.io/rules/standard/). Hidden folders will be skipped.

```shell title="Default validation with standard ruleset"
ktlint
```

Globs can be used to specify more exactly what files and directories are to be validated. `ktlint` uses the [`.gitignore` pattern style syntax for globs](https://git-scm.com/docs/gitignore). Globs are processed from left to right. Prepend a glob with `!` to negate it. Hidden folders will be skipped.

```shell title="Check only certain locations starting from the current directory"
# Check all '.kt' files in 'src/' directory, but ignore files ending with 'Test.kt':
ktlint "src/**/*.kt" "!src/**/*Test.kt"

# Check all '.kt' files in 'src/' directory, but ignore 'generated' directory and its subdirectories:
ktlint "src/**/*.kt" "!src/**/generated/**"
```

Most style violations can be corrected automatically. Errors that can not be corrected, are printed to `stderr`. 

```shell title="Auto-correct style violations"
$ ktlint -F
```

`ktlint` supports different type of reporters. When not specified the `plain` reporter is used. Optionally the `plain` reporter can group the violations per file.

```shell title="Style violation grouped by file"
$ ktlint --reporter=plain?group_by_file
```

Style violations can be written to an output file which is convenient when multiple reporters are specified. In example below, the plain reporter is used to write to the console while the checkstyle reports is written to a file:

```shell title="Multiple reporters"
ktlint --reporter=plain --reporter=checkstyle,output=ktlint-report-in-checkstyle-format.xml
```

If resolving all existing errors in a project is unwanted, it is possible to create a baseline and in following invocations compare violations against this baseline. Violations that are registered in the baseline, will be ignored silently. Remove the baseline file in case you want to reset it.

```shell title="Check against a baseline file"
ktlint --baseline=ktlint-baseline.xml # Baseline is created when not existing
```

Predefined git hooks can be installed, to automatically validate lint errors before commit or push.

```shell title="Install git pre-commit hook"
ktlint installGitPreCommitHook
```

```shell title="Install git pre-push hook"
ktlint installGitPrePushHook
```

!!! tip "Microsoft Windows"
    On Microsoft Windows you'll have to use `java -jar ktlint ...`.
