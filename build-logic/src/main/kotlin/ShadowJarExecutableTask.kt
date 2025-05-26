import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class ShadowJarExecutableTask @Inject constructor(
    objectFactory: ObjectFactory,
    layout: ProjectLayout,
) : DefaultTask() {

    init {
        description = "Creates self-executable file, that runs generated shadow jar"
        group = "Distribution"
    }


    private val shadowOutputPath = layout.buildDirectory.dir("run")

    @InputFile
    val selfExecutable = objectFactory.fileProperty()

    @InputFile
    val windowsBatchFileInputPath = objectFactory.fileProperty()

    @OutputFile
    val selfExecutableKtlintOutputPath = shadowOutputPath.map { it.file("ktlint").asFile }

    @OutputFile
    val selfExecutableKtlintSignatureOutputPath = shadowOutputPath.map { it.file("ktlint.asc").asFile }

    @OutputFile
    val windowsBatchFileOutputPath = shadowOutputPath.map { it.file("ktlint.bat").asFile }

    @TaskAction
    fun run() {
        selfExecutableKtlintOutputPath.get().apply {
            logger.lifecycle("Creating the self-executable file: ${selfExecutableKtlintOutputPath.get()}")

            // writeText effective replaces the entire content if the file already exists. If appendText is used, the file keeps on growing
            // with each build if the clean target is not used.
            // language=bash
            writeText(
                """
                #!/bin/sh

                # From this SO answer: https://stackoverflow.com/a/56243046

                # First we get the major Java version as an integer, e.g. 8, 11, 16. It has special handling for the leading 1 of older java
                # versions, e.g. 1.8 = Java 8
                JV=$(java -version 2>&1 | sed -E -n 's/.* version "([^.-]*).*".*/\1/p')

                # Add --add-opens for java version 16 and above
                X=$( [ "${"$"}JV" -ge "16" ] && echo "--add-opens java.base/java.lang=ALL-UNNAMED" || echo "")

                exec java ${"$"}X -Xmx512m -jar "$0" "$@"

                """.trimIndent(),
            )
            // Add the jar
            appendBytes(selfExecutable.asFile.get().readBytes())

            setExecutable(true, false)
        }
        logger.lifecycle("Creating the batch file for Windows OS: ${windowsBatchFileOutputPath.get()}")
        windowsBatchFileOutputPath.get().writeText(windowsBatchFileInputPath.get().asFile.readText())

        logger.lifecycle("Finished creating output files ktlint-cli")
    }
}
