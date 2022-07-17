# Welcome to Ktlint

<h1 align="center">
<a href="https://ktlint.github.io/">
  <img src="https://cloud.githubusercontent.com/assets/370176/26518284/38b680da-4262-11e7-8d27-2b9e849fb55f.png"/>
</a>
</h1>
<p align="center">
<a href="https://kotlinlang.slack.com/messages/CKS3XG0LS"><img src="https://img.shields.io/badge/slack-@kotlinlang/ktlint-yellow.svg?logo=slack" alt="Join the chat at https://kotlinlang.slack.com"/></a>
<a href="https://github.com/pinterest/ktlint/actions?query=workflow%3A%22Snapshot+Publish%22"><img src="https://github.com/pinterest/ktlint/workflows/Snapshot%20Publish/badge.svg" alt="Build status"></a>
<a href="https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.pinterest%22%20AND%20a%3A%22ktlint%22"><img src="https://img.shields.io/maven-central/v/com.pinterest/ktlint.svg" alt="Maven Central"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="ktlint"></a>
</p>
<p align="center">
<a href="https://kotlinlang.org/">Kotlin</a> linter in spirit of <a href="https://github.com/feross/standard">feross/standard</a> (JavaScript) and <a href="https://golang.org/cmd/gofmt/">gofmt</a> (Go).  
</p>

## Features

- **No configuration required**  
 `ktlint` aims to capture the [official Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html) and [Android Kotlin Style Guide](https://android.github.io/kotlin-guides/style.html). In some aspects `ktlint` is a bit more strict[*](https://github.com/pinterest/ktlint/issues/284#issuecomment-425177186).
- **Rule sets**  
  `ktlint` offers a `standard` and an `experimental` rule set. Next to this, it is easy to provide [custom rule sets](#creating-a-ruleset).
- **.editorconfig**  
  Some rules do allow further configuration, but in all cases a reasonable default is set when not provided. `ktlint` primarily uses the [.editorconfig file](#editorconfig) to read default `.editorconfig`, IntelliJ IDEA specific and Ktlint specific properties.
- **Disable rules**  
  If need be, rules can be disabled easily[*](https://github.com/pinterest/ktlint#how-do-i-globally-disable-a-rule).
- **Built-in formatter**  
  Most lint violations don't need to be fixed manually. `ktlint` has a built-in formatter which fixes violations when possible. Some violations can not be fixed in a deterministic way, and need manual action.
- **Customizable output**  
  Several reporters are available out-of-the-box: `plain` (+ `plain?group_by_file`), `json`, `html` and `checkstyle`.
  It's also easy to [create a custom reporter](#creating-a-reporter).
- **Executable jar**  
  `ktlint` is releases as a single executable jar with all dependencies included.

## Legal

This project is not affiliated with nor endorsed by JetBrains.  
All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2019 Pinterest, Inc.  
Copyright (c) 2016-2019 Stanley Shyiko.
