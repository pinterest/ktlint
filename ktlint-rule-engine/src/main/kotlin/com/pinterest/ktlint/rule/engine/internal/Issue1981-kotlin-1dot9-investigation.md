@goodwinnk Tnx for the pointers. I am not sure whether I understand all that you wrote. I will come back to that on later point in time if needed, and I hope that is ok with you. You may ignore, the analysis below. It is just here, to keep track of what I have done sofar.

Sofar, the initial investigation revealed that when running on the [kotlin 1.9.0-dev-6976 bootstrap](https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/org/jetbrains/kotlin/kotlin-compiler-embeddable/1.9.0-dev-6976/):
* 247 tests out of 1705 fail when removing when *not* calling `enableASTMutations()`
  * If `enableASTMutations()` is called and, only the registration of the TreeCopyHandler class is skipped, the number of failed tests drops to 47. So the absence of the registration of the TreeCopyHandler results in 47 errors.
* Replacing the current implementation of method below in `KotlinPsiFileFactory`:
```
private fun MockProject.enableASTMutations() {
    val extensionPoint = "org.jetbrains.kotlin.com.intellij.treeCopyHandler"
    val extensionClassName = TreeCopyHandler::class.java.name
    for (area in arrayOf(extensionArea, getRootArea())) {
        if (!area.hasExtensionPoint(extensionPoint)) {
            area.registerExtensionPoint(extensionPoint, extensionClassName, ExtensionPoint.Kind.INTERFACE)
        }
    }

    registerService(PomModel::class.java, FormatPomModel())
}
```
with below (analog to [suggested replacement](https://github.com/JetBrains/kotlin/commit/3caa1d13492fef61a47132c17475dcde02a47624)):
```
private fun MockProject.enableASTMutations() {
    val extensionPointName: ExtensionPointName<MockProject> = ExtensionPointName.create("org.jetbrains.kotlin.com.intellij.treeCopyHandler")
    val extensionClass = TreeCopyHandler::class.java
    for (area in arrayOf(extensionArea, getRootArea())) {
        CoreApplicationEnvironment.registerExtensionPoint(
            extensionArea,
            extensionPointName,
            extensionClass
        )
    }

    registerService(PomModel::class.java, FormatPomModel())
}
```
does not have any effect as an IllegalArgumentException like below is thrown:
```text

Rule 'test:auto-correct' throws exception in file '<stdin>' at position (0:0)
   Rule maintainer: Not specified (and not maintained by the Ktlint project)
   Issue tracker  : Not specified
   Repository     : Not specified
com.pinterest.ktlint.rule.engine.api.KtLintRuleException: Rule 'test:auto-correct' throws exception in file '<stdin>' at position (0:0)
   Rule maintainer: Not specified (and not maintained by the Ktlint project)
   Issue tracker  : Not specified
   Repository     : Not specified
	at app//com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRule(RuleExecutionContext.kt:64)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine$format$3.invoke(KtLintRuleEngine.kt:136)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine$format$3.invoke(KtLintRuleEngine.kt:135)
	at app//com.pinterest.ktlint.rule.engine.internal.VisitorProvider$visitor$3.invoke(VisitorProvider.kt:46)
	at app//com.pinterest.ktlint.rule.engine.internal.VisitorProvider$visitor$3.invoke(VisitorProvider.kt:44)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine.format(KtLintRuleEngine.kt:135)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintTest$Given an API consumer$Given that format is invoked via the KtLintRuleEngine.Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback(KtLintTest.kt:146)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base@17.0.3/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base@17.0.3/java.lang.reflect.Method.invoke(Method.java:568)
	at app//org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
	at app//org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
	at app//org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
	at app//org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
	at app//org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:217)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:213)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:138)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:68)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
	at app//org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at app//org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:107)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:88)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:54)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:67)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:52)
	at app//org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:114)
	at app//org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
	at app//org.junit.platform.launcher.core.DefaultLauncherSession$DelegatingLauncher.execute(DefaultLauncherSession.java:86)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor.processAllTestClasses(JUnitPlatformTestClassProcessor.java:110)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor.access$000(JUnitPlatformTestClassProcessor.java:90)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor.stop(JUnitPlatformTestClassProcessor.java:85)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.stop(SuiteTestClassProcessor.java:62)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base@17.0.3/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base@17.0.3/java.lang.reflect.Method.invoke(Method.java:568)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:33)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:94)
	at jdk.proxy1/jdk.proxy1.$Proxy2.stop(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker$3.run(TestWorker.java:193)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.executeAndMaintainThreadName(TestWorker.java:129)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:100)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:60)
	at org.gradle.process.internal.worker.child.ActionExecutionWorker.execute(ActionExecutionWorker.java:56)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:113)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:65)
	at app//worker.org.gradle.process.internal.worker.GradleWorkerMain.run(GradleWorkerMain.java:69)
	at app//worker.org.gradle.process.internal.worker.GradleWorkerMain.main(GradleWorkerMain.java:74)
Caused by: java.lang.IllegalArgumentException: Missing extension point: org.jetbrains.kotlin.com.intellij.treeCopyHandler in container {}
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionsAreaImpl.getExtensionPoint(ExtensionsAreaImpl.java:250)
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.BaseExtensionPointName.getPointImpl(BaseExtensionPointName.java:28)
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPointName.getExtensionList(ExtensionPointName.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.encodeInformation(ChangeUtil.java:43)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.lambda$encodeInformation$0(ChangeUtil.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.DebugUtil.performPsiModification(DebugUtil.java:481)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.encodeInformation(ChangeUtil.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.copyLeafWithText(ChangeUtil.java:78)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
	at com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.beforeVisitChildNodes(KtLintTest.kt:506)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$1.invoke(RuleExecutionContext.kt:93)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$1.invoke(RuleExecutionContext.kt:92)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:92)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRule(RuleExecutionContext.kt:61)
	... 110 more
```
* The 47 failing tests have 10 distinct root causes in 8 ktlint rules which need further investigation:
```
org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
	at com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.beforeVisitChildNodes(KtLintTest.kt:506)

org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
	at com.pinterest.ktlint.ruleset.standard.rules.NoTrailingSpacesRule.removeTrailingSpacesBeforeNewline(NoTrailingSpacesRule.kt:76)

org.jetbrains.kotlin.com.intellij.extapi.psi.ASTDelegatePsiElement.addAfter(ASTDelegatePsiElement.java:292)
	at com.pinterest.ktlint.ruleset.standard.rules.SpacingAroundColonRule.beforeVisitChildNodes(SpacingAroundColonRule.kt:63)

org.jetbrains.kotlin.psi.KtExpressionImplStub.replace(KtExpressionImplStub.java:43)
	at com.pinterest.ktlint.ruleset.standard.rules.StringTemplateRule.beforeVisitChildNodes(StringTemplateRule.kt:47)

org.jetbrains.kotlin.com.intellij.extapi.psi.ASTDelegatePsiElement.addAfter(ASTDelegatePsiElement.java:292)
	at com.pinterest.ktlint.ruleset.standard.rules.NoLineBreakBeforeAssignmentRule.beforeVisitChildNodes(NoLineBreakBeforeAssignmentRule.kt:38)

org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
	at com.pinterest.ktlint.ruleset.standard.rules.ParameterListSpacingRule.replaceWithSingleSpace(ParameterListSpacingRule.kt:252)

org.jetbrains.kotlin.com.intellij.extapi.psi.ASTDelegatePsiElement.addAfter(ASTDelegatePsiElement.java:292)
	at com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnCallSiteRule.reportAndCorrectTrailingCommaNodeBefore(TrailingCommaOnCallSiteRule.kt:177)

org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement.replace(LeafPsiElement.java:198)
	at com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnDeclarationSiteRule.reportAndCorrectTrailingCommaNodeBefore(TrailingCommaOnDeclarationSiteRule.kt:326)

org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement.replace(LeafPsiElement.java:198)
	at com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnDeclarationSiteRule.reportAndCorrectTrailingCommaNodeBefore(TrailingCommaOnDeclarationSiteRule.kt:341)

org.jetbrains.kotlin.com.intellij.extapi.psi.ASTDelegatePsiElement.addAfter(ASTDelegatePsiElement.java:292)
	at com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnDeclarationSiteRule.reportAndCorrectTrailingCommaNodeBefore(TrailingCommaOnDeclarationSiteRule.kt:348)
```
* More worrisome is that similar problems can occur in custom rule sets and will need to be investigated and fixed by maintainers of those rulesets.



===

Summary for new issue to be created:


Ktlint lint and formats Kotlin code. For formatting of the code it is necessary that Ktlint is able to mutate the AST of files in which lint violations are found. For this an extension point on class `org.jetbrains.kotlin.com.intellij.treeCopyHandler` is registered in https://github.com/pinterest/ktlint/blob/cb0de4c3c848d1f1f61d53ed86475e165dd517ba/ktlint-rule-engine/src/main/kotlin/com/pinterest/ktlint/rule/engine/internal/KotlinPsiFileFactory.kt#L116.

In [ktlint issue 1981](https://github.com/pinterest/ktlint/issues/1981) it was reported that ktlint uses a deprecated endpoint which is to be removed in Kotlin 1.9 resulting in runtime failures in Ktlint. We have applied a fix similar to this suggestion for the [kotlin embeddable compiler](https://github.com/JetBrains/kotlin/commit/3caa1d13492fef61a47132c17475dcde02a47624) and compiled and run Ktlint with [kotlin 1.9.0-dev-6976 bootstrap](https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/org/jetbrains/kotlin/kotlin-compiler-embeddable/1.9.0-dev-6976/). Some tests of Ktlint fail with exceptions like below:
```text
Rule 'test:auto-correct' throws exception in file '<stdin>' at position (0:0)
   Rule maintainer: Not specified (and not maintained by the Ktlint project)
   Issue tracker  : Not specified
   Repository     : Not specified
com.pinterest.ktlint.rule.engine.api.KtLintRuleException: Rule 'test:auto-correct' throws exception in file '<stdin>' at position (0:0)
   Rule maintainer: Not specified (and not maintained by the Ktlint project)
   Issue tracker  : Not specified
   Repository     : Not specified
	at app//com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRule(RuleExecutionContext.kt:64)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine$format$3.invoke(KtLintRuleEngine.kt:136)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine$format$3.invoke(KtLintRuleEngine.kt:135)
	at app//com.pinterest.ktlint.rule.engine.internal.VisitorProvider$visitor$3.invoke(VisitorProvider.kt:46)
	at app//com.pinterest.ktlint.rule.engine.internal.VisitorProvider$visitor$3.invoke(VisitorProvider.kt:44)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine.format(KtLintRuleEngine.kt:135)
	at app//com.pinterest.ktlint.rule.engine.api.KtLintTest$Given an API consumer$Given that format is invoked via the KtLintRuleEngine.Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback(KtLintTest.kt:146)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base@17.0.3/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base@17.0.3/java.lang.reflect.Method.invoke(Method.java:568)
	at app//org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
	at app//org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
	at app//org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
	at app//org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
	at app//org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestMethod(TimeoutExtension.java:86)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker$ReflectiveInterceptorCall.lambda$ofVoidMethod$0(InterceptingExecutableInvoker.java:103)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.lambda$invoke$0(InterceptingExecutableInvoker.java:93)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain$InterceptedInvocation.proceed(InvocationInterceptorChain.java:106)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain.proceed(InvocationInterceptorChain.java:64)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain.chainAndInvoke(InvocationInterceptorChain.java:45)
	at app//org.junit.jupiter.engine.execution.InvocationInterceptorChain.invoke(InvocationInterceptorChain.java:37)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:92)
	at app//org.junit.jupiter.engine.execution.InterceptingExecutableInvoker.invoke(InterceptingExecutableInvoker.java:86)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.lambda$invokeTestMethod$7(TestMethodTestDescriptor.java:217)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.invokeTestMethod(TestMethodTestDescriptor.java:213)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:138)
	at app//org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor.execute(TestMethodTestDescriptor.java:68)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:151)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at java.base@17.0.3/java.util.ArrayList.forEach(ArrayList.java:1511)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.invokeAll(SameThreadHierarchicalTestExecutorService.java:41)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$6(NodeTestTask.java:155)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$8(NodeTestTask.java:141)
	at app//org.junit.platform.engine.support.hierarchical.Node.around(Node.java:137)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.lambda$executeRecursively$9(NodeTestTask.java:139)
	at app//org.junit.platform.engine.support.hierarchical.ThrowableCollector.execute(ThrowableCollector.java:73)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.executeRecursively(NodeTestTask.java:138)
	at app//org.junit.platform.engine.support.hierarchical.NodeTestTask.execute(NodeTestTask.java:95)
	at app//org.junit.platform.engine.support.hierarchical.SameThreadHierarchicalTestExecutorService.submit(SameThreadHierarchicalTestExecutorService.java:35)
	at app//org.junit.platform.engine.support.hierarchical.HierarchicalTestExecutor.execute(HierarchicalTestExecutor.java:57)
	at app//org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine.execute(HierarchicalTestEngine.java:54)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:107)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:88)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.lambda$execute$0(EngineExecutionOrchestrator.java:54)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.withInterceptedStreams(EngineExecutionOrchestrator.java:67)
	at app//org.junit.platform.launcher.core.EngineExecutionOrchestrator.execute(EngineExecutionOrchestrator.java:52)
	at app//org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:114)
	at app//org.junit.platform.launcher.core.DefaultLauncher.execute(DefaultLauncher.java:86)
	at app//org.junit.platform.launcher.core.DefaultLauncherSession$DelegatingLauncher.execute(DefaultLauncherSession.java:86)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor.processAllTestClasses(JUnitPlatformTestClassProcessor.java:110)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor$CollectAllTestClassesExecutor.access$000(JUnitPlatformTestClassProcessor.java:90)
	at org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestClassProcessor.stop(JUnitPlatformTestClassProcessor.java:85)
	at org.gradle.api.internal.tasks.testing.SuiteTestClassProcessor.stop(SuiteTestClassProcessor.java:62)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base@17.0.3/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
	at java.base@17.0.3/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base@17.0.3/java.lang.reflect.Method.invoke(Method.java:568)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:36)
	at org.gradle.internal.dispatch.ReflectionDispatch.dispatch(ReflectionDispatch.java:24)
	at org.gradle.internal.dispatch.ContextClassLoaderDispatch.dispatch(ContextClassLoaderDispatch.java:33)
	at org.gradle.internal.dispatch.ProxyDispatchAdapter$DispatchingInvocationHandler.invoke(ProxyDispatchAdapter.java:94)
	at jdk.proxy1/jdk.proxy1.$Proxy2.stop(Unknown Source)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker$3.run(TestWorker.java:193)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.executeAndMaintainThreadName(TestWorker.java:129)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:100)
	at org.gradle.api.internal.tasks.testing.worker.TestWorker.execute(TestWorker.java:60)
	at org.gradle.process.internal.worker.child.ActionExecutionWorker.execute(ActionExecutionWorker.java:56)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:113)
	at org.gradle.process.internal.worker.child.SystemApplicationClassLoaderWorker.call(SystemApplicationClassLoaderWorker.java:65)
	at app//worker.org.gradle.process.internal.worker.GradleWorkerMain.run(GradleWorkerMain.java:69)
	at app//worker.org.gradle.process.internal.worker.GradleWorkerMain.main(GradleWorkerMain.java:74)
Caused by: java.lang.IllegalArgumentException: Missing extension point: org.jetbrains.kotlin.com.intellij.treeCopyHandler in container {}
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionsAreaImpl.getExtensionPoint(ExtensionsAreaImpl.java:250)
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.BaseExtensionPointName.getPointImpl(BaseExtensionPointName.java:28)
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPointName.getExtensionList(ExtensionPointName.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.encodeInformation(ChangeUtil.java:43)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.lambda$encodeInformation$0(ChangeUtil.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.DebugUtil.performPsiModification(DebugUtil.java:481)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.encodeInformation(ChangeUtil.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.copyLeafWithText(ChangeUtil.java:78)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
	at com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.beforeVisitChildNodes(KtLintTest.kt:506)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$1.invoke(RuleExecutionContext.kt:93)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$1.invoke(RuleExecutionContext.kt:92)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:92)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.access$executeRuleOnNodeRecursively(RuleExecutionContext.kt:29)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:100)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext$executeRuleOnNodeRecursively$2$1.invoke(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.SuppressHandler.handle-dhiVX_g(SuppressHandler.kt:28)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRuleOnNodeRecursively(RuleExecutionContext.kt:99)
	at com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.executeRule(RuleExecutionContext.kt:61)
	... 110 more
```

The relevant part of this stacktrace seems to be the following:
```text
Caused by: java.lang.IllegalArgumentException: Missing extension point: org.jetbrains.kotlin.com.intellij.treeCopyHandler in container {}
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionsAreaImpl.getExtensionPoint(ExtensionsAreaImpl.java:250)
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.BaseExtensionPointName.getPointImpl(BaseExtensionPointName.java:28)
	at org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPointName.getExtensionList(ExtensionPointName.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.encodeInformation(ChangeUtil.java:43)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.lambda$encodeInformation$0(ChangeUtil.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.DebugUtil.performPsiModification(DebugUtil.java:481)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.encodeInformation(ChangeUtil.java:39)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.ChangeUtil.copyLeafWithText(ChangeUtil.java:78)
	at org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
```

See below for the list of root causes found by the unit tests of ktlint:
```
org.jetbrains.kotlin.psi.KtExpressionImplStub.replace(KtExpressionImplStub.java:43)
org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement.replaceWithText(LeafElement.java:153)
org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement.replace(LeafPsiElement.java:198)
org.jetbrains.kotlin.com.intellij.extapi.psi.ASTDelegatePsiElement.addAfter(ASTDelegatePsiElement.java:292)
```
Most likely we can work around those root causes as the majority of Ktlint's rules that modify the AST are not using above methods. But as Ktlint has the format of custom rulesets, similar analysis and fixes have to be applies by all maintainers of such ruleset.

Ideally, the extension point for class `org.jetbrains.kotlin.com.intellij.treeCopyHandler` keeps working in kotlin 1.9 and beyond. If that is not possible, it would be great if another extension point can be used that provides for similar functionality.
