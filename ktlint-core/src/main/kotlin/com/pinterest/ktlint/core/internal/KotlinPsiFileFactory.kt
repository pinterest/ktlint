package com.pinterest.ktlint.core.internal

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.DefaultLogger
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.Logger as DiagnosticLogger
import org.jetbrains.kotlin.com.intellij.openapi.extensions.ExtensionPoint
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions.getRootArea
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.TreeCopyHandler
import org.jetbrains.kotlin.config.CompilerConfiguration
import sun.reflect.ReflectionFactory

/**
 * Initialize Kotlin Lexer.
 */
internal fun initPsiFileFactory(): PsiFileFactory {
    DiagnosticLogger.setFactory(LoggerFactory::class.java)

    val compilerConfiguration = CompilerConfiguration()
    compilerConfiguration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

    val project = KotlinCoreEnvironment.createForProduction(
        {},
        compilerConfiguration,
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    ).project as MockProject

    project.enableASTMutations()

    return PsiFileFactory.getInstance(project)
}

/**
 * Do not print anything to the stderr when lexer is unable to match input.
 */
private class LoggerFactory : DiagnosticLogger.Factory {
    override fun getLoggerInstance(
        p: String
    ): DiagnosticLogger = object : DefaultLogger(null) {
        override fun warn(message: String?, t: Throwable?) {}
        override fun error(message: String?, vararg details: String?) {}
    }
}

/**
 * Enables AST mutations (`ktlint -F ...`).
 */
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

private class FormatPomModel : UserDataHolderBase(), PomModel {

    override fun runTransaction(
        transaction: PomTransaction
    ) {
        (transaction as PomTransactionBase).run()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : PomModelAspect> getModelAspect(
        aspect: Class<T>
    ): T? {
        if (aspect == TreeAspect::class.java) {
            // using approach described in https://git.io/vKQTo due to the magical bytecode of TreeAspect
            // (check constructor signature and compare it to the source)
            // (org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3)
            val constructor = ReflectionFactory
                .getReflectionFactory()
                .newConstructorForSerialization(
                    aspect,
                    Any::class.java.getDeclaredConstructor(*arrayOfNulls<Class<*>>(0))
                )
            return constructor.newInstance() as T
        }
        return null
    }
}
