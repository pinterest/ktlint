Most of the kotlin files in this directory contain lint violations which are required for testing. Files that do contain lint errors have extension ".test". This additional extension prevents that the file is being picked up by ktlint when running it on the project itself. Fixing the lint violations in those files results in failing tests in the next test run.

File "no-code-style-error/Main.kt" does not contain a lint violation and its file name does not end with ".test" as the files needs to be picked up by a test which tests the default kotlin extension.
