IMPORTANT: This module is kept around for future reference in case the current RuleSetProviderV3 is being deprecated.

This module contains a rule set provider based on the deprecated `com.pinterest.ktlint.core.api.RuleSetProviderV2` which is already removed from the current code base. It was used to build a custom rule set JAR for testing the `ktlint-cli` module.

As this module is not meant to be published, you need to build the jar explicitly and copy the jar to the `ktlint-cli` test resource folder:
```shell
cd .. # run from root of ktlint project
./gradlew ktlint-test-ruleset-provider-v2-deprecated:jar && \
cp ktlint-test-ruleset-provider-v2-deprecated/build/libs/ktlint-test-ruleset-provider-v2-deprecated.jar  ktlint-cli/src/test/resources/cli/custom-ruleset/rule-set-provider-v2/
```
