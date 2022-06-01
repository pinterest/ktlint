package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.initKtLintKLogger
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import mu.KotlinLogging
import org.w3c.dom.Element
import org.xml.sax.SAXException

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

// TODO: Rename to baseline once the class and methods in this file are no longer in the public API
@Deprecated("Method will be removed from the public API in KtLint 0.47.0 or later. Please create an issue if you have a valid reason for using it.")
public class CurrentBaseline(
    public val baselineRules: Map<String, List<LintError>>?,
    public val baselineGenerationNeeded: Boolean
)

/**
 * Loads the baseline file if one is provided.
 *
 * @param baselineFilePath the path to the xml baseline file
 * @return a [CurrentBaseline] with the file details
 */
@Deprecated("Method will be removed from the public API in KtLint 0.47.0 or later. Please create an issue if you have a valid reason for using it.")
public fun loadBaseline(
    baselineFilePath: String
): CurrentBaseline {
    if (baselineFilePath.isBlank()) {
        return CurrentBaseline(null, false)
    }

    var baselineRules: Map<String, List<LintError>>? = null
    var baselineGenerationNeeded = true
    val baselineFile = Paths.get(baselineFilePath).toFile()
    if (baselineFile.exists()) {
        try {
            baselineRules = parseBaseline(baselineFile.inputStream())
            baselineGenerationNeeded = false
        } catch (e: IOException) {
            logger.error { "Unable to parse baseline file: $baselineFilePath" }
            baselineGenerationNeeded = true
        } catch (e: ParserConfigurationException) {
            logger.error { "Unable to parse baseline file: $baselineFilePath" }
            baselineGenerationNeeded = true
        } catch (e: SAXException) {
            logger.error { "Unable to parse baseline file: $baselineFilePath" }
            baselineGenerationNeeded = true
        }
    }

    // delete the old file if one exists
    if (baselineGenerationNeeded && baselineFile.exists()) {
        baselineFile.delete()
    }

    return CurrentBaseline(baselineRules, baselineGenerationNeeded)
}

/**
 * Parses the file to generate a mapping of [LintError]
 *
 * @param baselineFile the file containing the current baseline
 * @return a mapping of file names to a list of all [LintError] in that file
 */
internal fun parseBaseline(baselineFile: InputStream): Map<String, List<LintError>> {
    val baselineRules = HashMap<String, MutableList<LintError>>()
    val builderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = builderFactory.newDocumentBuilder()
    val doc = docBuilder.parse(baselineFile)
    val filesList = doc.getElementsByTagName("file")
    for (i in 0 until filesList.length) {
        val fileElement = filesList.item(i) as Element
        val fileName = fileElement.getAttribute("name")
        val baselineErrors = parseBaselineErrorsByFile(fileElement)
        baselineRules[fileName] = baselineErrors
    }
    return baselineRules
}

/**
 * Parses the errors inside each file tag in the xml
 *
 * @param element the xml "file" element
 * @return a list of [LintError] for that file
 */
private fun parseBaselineErrorsByFile(element: Element): MutableList<LintError> {
    val errors = mutableListOf<LintError>()
    val errorsList = element.getElementsByTagName("error")
    for (i in 0 until errorsList.length) {
        val errorElement = errorsList.item(i) as Element
        errors.add(
            LintError(
                line = errorElement.getAttribute("line").toInt(),
                col = errorElement.getAttribute("column").toInt(),
                ruleId = errorElement.getAttribute("source"),
                detail = "" // we don't have details in the baseline file
            )
        )
    }
    return errors
}

/**
 * Checks if the list contains the lint error. We cannot use the contains function
 * as the `checkstyle` reporter formats the details string and hence the comparison
 * normally fails
 */
@Deprecated("Method will be removed from the public API in KtLint 0.47.0 or later. Please create an issue if you have a valid reason for using it.")
public fun List<LintError>.containsLintError(error: LintError): Boolean {
    return firstOrNull { lintError ->
        lintError.col == error.col &&
            lintError.line == error.line &&
            lintError.ruleId == error.ruleId
    } != null
}

/**
 * Gets the relative route of the file for baselines
 * Also adjusts the slashes for uniformity between file systems
 */
@Deprecated("Method will be removed from the public API in KtLint 0.47.0 or later. Please create an issue if you have a valid reason for using it.")
public val File.relativeRoute: String
    get() {
        val rootPath = Paths.get("").toAbsolutePath()
        val filePath = this.toPath()
        return rootPath.relativize(filePath).toString().replace(File.separatorChar, '/')
    }
