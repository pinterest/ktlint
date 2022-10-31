package com.pinterest.ktlint.core.api

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.api.Baseline.Status.INVALID
import com.pinterest.ktlint.core.api.Baseline.Status.NOT_FOUND
import com.pinterest.ktlint.core.api.Baseline.Status.VALID
import com.pinterest.ktlint.core.initKtLintKLogger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import kotlin.io.path.relativeToOrSelf
import mu.KotlinLogging
import org.w3c.dom.Element
import org.xml.sax.SAXException

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * Baseline of lint errors to be ignored in subsequent calls to ktlint.
 */
public class Baseline(
    /**
     * Lint errors grouped by (relative) file path.
     */
    public val lintErrorsPerFile: Map<String, List<LintError>> = emptyMap(),

    /**
     * Status of the baseline file.
     */
    public val status: Status,
) {
    public enum class Status {
        /**
         * Baseline file is successfully parsed.
         */
        VALID,

        /**
         * Baseline file does not exist. File needs to be generated by the consumer first.
         */
        NOT_FOUND,

        /**
         * Baseline file is not successfully parsed. File needs to be regenerated by the consumer.
         */
        INVALID,
    }
}

/**
 * Loads the [Baseline] from the file located on [path].
 */
public fun loadBaseline(path: String): Baseline {
    Paths
        .get(path)
        .toFile()
        .takeIf { it.exists() }
        ?.let { baselineFile ->
            try {
                return Baseline(
                    lintErrorsPerFile = parseBaseline(baselineFile.inputStream()),
                    status = VALID,
                )
            } catch (e: IOException) {
                LOGGER.error { "Unable to parse baseline file: $path" }
            } catch (e: ParserConfigurationException) {
                LOGGER.error { "Unable to parse baseline file: $path" }
            } catch (e: SAXException) {
                LOGGER.error { "Unable to parse baseline file: $path" }
            }

            // Baseline can not be parsed.
            try {
                baselineFile.delete()
            } catch (e: IOException) {
                LOGGER.error { "Unable to delete baseline file: $path" }
            }
            return Baseline(status = INVALID)
        }

    return Baseline(status = NOT_FOUND)
}

/**
 * Parses the [inputStream] of a baseline file and return the lint errors grouped by the relative file names.
 */
internal fun parseBaseline(inputStream: InputStream): Map<String, List<LintError>> {
    val lintErrorsPerFile = HashMap<String, List<LintError>>()
    val fileNodeList =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(inputStream)
            .getElementsByTagName("file")
    for (i in 0 until fileNodeList.length) {
        with(fileNodeList.item(i) as Element) {
            lintErrorsPerFile[getAttribute("name")] = parseBaselineErrorsByFile()
        }
    }
    return lintErrorsPerFile
}

/**
 * Parses the [LintError]s inside a file element in the xml
 */
private fun Element.parseBaselineErrorsByFile(): List<LintError> {
    val errors = mutableListOf<LintError>()
    val errorsList = getElementsByTagName("error")
    for (i in 0 until errorsList.length) {
        errors.add(
            with(errorsList.item(i) as Element) {
                LintError(
                    line = getAttribute("line").toInt(),
                    col = getAttribute("column").toInt(),
                    ruleId = getAttribute("source"),
                    detail = "", // Not available in the baseline file
                )
            },
        )
    }
    return errors
}

/**
 * Checks if the list contains the given [LintError]. The [List.contains] function can not be used as [LintError.detail]
 * is not available in the baseline file and a normal equality check on the [LintErrpr] fails.
 */
public fun List<LintError>.containsLintError(lintError: LintError): Boolean =
    any { it.isSameAs(lintError) }

private fun LintError.isSameAs(lintError: LintError) =
    col == lintError.col &&
        line == lintError.line &&
        ruleId == lintError.ruleId

/**
 * Checks if the list does not contain the given [LintError]. The [List.contains] function can not be used as
 * [LintError.detail] is not available in the baseline file and a normal equality check on the [LintErrpr] fails.
 */
public fun List<LintError>.doesNotContain(lintError: LintError): Boolean =
    none { it.isSameAs(lintError) }

/**
 * Gets the relative route of the file for baselines. Also adjusts the slashes for uniformity between file systems
 */
public val File.relativeRoute: String
    get() {
        val rootPath = Paths.get("").toAbsolutePath()
        val filePath = this.toPath()
        return filePath.relativeToOrSelf(rootPath).toString().replace(File.separatorChar, '/')
    }
