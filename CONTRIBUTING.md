# Contributing

First off, thanks for taking the time to contribute! This guide will answer
some common questions about how this project works.

While this is a Pinterest open source project, we welcome contributions from
everyone. Regular outside contributors can become project maintainers.

## Help

If you're having trouble using this project, please start by reading the [`README.md`](README.md)
and searching for solutions in the existing open and closed issues.

## Security

If you've found a security issue in one of our open source projects,
please report it at [Bugcrowd](https://bugcrowd.com/pinterest); you may even
make some money!

## Code of Conduct

Please be sure to read and understand our [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
We work hard to ensure that our projects are welcoming and inclusive to as many
people as possible.

## Reporting Issues

If you have a bug report, please provide as much information as possible so that
we can help you out:

- Version of the project you're using.
- Code (or even better whole projects) which reproduce the issue.
- Steps which reproduce the issue.
- Stack traces for crashes.
- Any logs produced.

## Making Changes

1. Fork this repository to your own account
2. Make your changes and verify that tests pass
3. Commit your work and push to a new branch on your fork
4. Submit a pull request
5. Participate in the code review process by responding to feedback

Once there is agreement that the code is in good shape, one of the project's
maintainers will merge your contribution.

To increase the chances that your pull request will be accepted:

- Follow the coding style
- Write tests for your changes
- Write a good commit message

If you would like to introduce a new rule to the core set, please [file an issue](https://github.com/pinterest/ktlint/issues/new)
first so that it can be discussed amongst the community. Remember that you are can make your own custom rulesets
without having to change the core ruleset.

New core rules will be added first to the experimental ruleset before being rolled out widely.

## Updating dependencies

Project has enabled [Gradle dependencies verification](https://docs.gradle.org/6.2/userguide/dependency_verification.html).
On adding/updating any dependency, ensure that you've added dependency provided checksum/signature to `gradle/verification-metadata.xml` file.

## Using koltin development versions

Add following flag - `-PkotlinDev` to enable kotlin development version.

## License

By contributing to this project, you agree that your contributions will be
licensed under its [license](LICENSE).
