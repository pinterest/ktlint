package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SCRIPT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SCRIPT_INITIALIZER
import com.pinterest.ktlint.rule.engine.core.util.cast
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.DefaultLogger
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.kotlin.com.intellij.pom.PomModel
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.com.intellij.pom.impl.PomTransactionBase
import org.jetbrains.kotlin.com.intellij.pom.tree.TreeAspect
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import sun.reflect.ReflectionFactory
import org.jetbrains.kotlin.com.intellij.openapi.diagnostic.Logger as DiagnosticLogger

/**
 * Embedded Kotlin Compiler configured for use by Ktlint.
 */
public object KtlintKotlinCompiler {
    private val psiFileFactory = initPsiFileFactory()

    /**
     * Create a PSI file with name [psiFileName] and content [text].
     */
    public fun createPsiFileFromText(
        psiFileName: String,
        text: String,
    ): PsiFile = psiFileFactory.createFileFromText(psiFileName, KotlinLanguage.INSTANCE, text)

    /**
     * Create the AST for a given piece of code.
     */
    public fun createASTNodeFromText(text: String): ASTNode? =
        // For a code snippet which is not necessarily compilable if it was compiled as a standalone file, it is better to compile it as
        // kotlin script.
        createPsiFileFromText("File.kts", text)
            .node
            .findChildByType(SCRIPT)
            ?.findChildByType(BLOCK)
            ?.let { it.findChildByType(SCRIPT_INITIALIZER) ?: it }
}

/**
 * Initialize Kotlin Lexer.
 */
private fun initPsiFileFactory(): PsiFileFactory {
    DiagnosticLogger.setFactory(LoggerFactory::class.java)

    val compilerConfiguration =
        CompilerConfiguration()
            .apply { put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE) }

    val disposable = Disposer.newDisposable()
    try {
        val project =
            KotlinCoreEnvironment
                .createForProduction(
                    disposable,
                    compilerConfiguration,
                    EnvironmentConfigFiles.JVM_CONFIG_FILES,
                ).project
                .cast<MockProject>()
                .apply { registerFormatPomModel() }

        return PsiFileFactory.getInstance(project)
    } finally {
        // Dispose explicitly to (possibly) prevent memory leak
        // https://discuss.kotlinlang.org/t/memory-leak-in-kotlincoreenvironment-and-kotlintojvmbytecodecompiler/21950
        // https://youtrack.jetbrains.com/issue/KT-47044
        disposable.dispose()
    }
}

/**
 * Do not print anything to the stderr when lexer is unable to match input.
 */
private class LoggerFactory : DiagnosticLogger.Factory {
    override fun getLoggerInstance(p: String): DiagnosticLogger =
        object : DefaultLogger(null) {
            override fun warn(
                message: String?,
                t: Throwable?,
            ) {}

            override fun error(
                message: String?,
                vararg details: String?,
            ) {}
        }
}

private fun MockProject.registerFormatPomModel() {
    registerService(PomModel::class.java, FormatPomModel())
}

private class FormatPomModel :
    UserDataHolderBase(),
    PomModel {
    override fun runTransaction(transaction: PomTransaction) {
        (transaction as PomTransactionBase).run()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : PomModelAspect> getModelAspect(aspect: Class<T>): T? {
        if (aspect == TreeAspect::class.java) {
            // using approach described in https://git.io/vKQTo due to the magical bytecode of TreeAspect
            // (check constructor signature and compare it to the source)
            // (org.jetbrains.kotlin:kotlin-compiler-embeddable:1.0.3)
            val constructor =
                ReflectionFactory
                    .getReflectionFactory()
                    .newConstructorForSerialization(
                        aspect,
                        Any::class.java.getDeclaredConstructor(*arrayOfNulls<Class<*>>(0)),
                    )
            return constructor.newInstance() as T
        }
        return null
    }
}
