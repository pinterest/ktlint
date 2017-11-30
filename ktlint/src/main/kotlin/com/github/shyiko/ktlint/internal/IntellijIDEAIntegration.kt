package com.github.shyiko.ktlint.internal

import com.github.shyiko.klob.Glob
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object IntellijIDEAIntegration {

    @Throws(IOException::class)
    fun apply(workDir: Path, dryRun: Boolean, android: Boolean = false): Array<Path> {
        if (!Files.isDirectory(workDir.resolve(".idea"))) {
            throw ProjectNotFoundException()
        }
        val home = System.getProperty("user.home")
        val editorConfig: Map<String, String> = EditorConfig.of(".") ?: emptyMap()
        val continuationIndentSize = editorConfig["continuation_indent_size"]?.toIntOrNull() ?: 4
        val indentSize = editorConfig["indent_size"]?.toIntOrNull() ?: 4
        val codeStyleName = "ktlint${
            if (continuationIndentSize == 4) "" else "-cis$continuationIndentSize"
        }${
            if (indentSize == 4) "" else "-is$indentSize"
        }"
        val paths =
            // macOS
            Glob.from("IntelliJIdea*", "IdeaIC*", "AndroidStudio*")
                .iterate(Paths.get(home, "Library", "Preferences"),
                    Glob.IterationOption.SKIP_CHILDREN, Glob.IterationOption.DIRECTORY).asSequence() +
            // linux/windows
            Glob.from(".IntelliJIdea*/config", ".IdeaIC*/config", ".AndroidStudio*/config")
                .iterate(Paths.get(home),
                    Glob.IterationOption.SKIP_CHILDREN, Glob.IterationOption.DIRECTORY).asSequence()
        val updates = (paths.flatMap { dir ->
            sequenceOf(
                Paths.get(dir.toString(), "codestyles", "$codeStyleName.xml") to
                    overwriteWithResource("/config/codestyles/ktlint.xml") { resource ->
                        resource
                            .replace("code_scheme name=\"ktlint\"",
                                "code_scheme name=\"$codeStyleName\"")
                            .replace("option name=\"INDENT_SIZE\" value=\"4\"",
                                "option name=\"INDENT_SIZE\" value=\"$indentSize\"")
                            .replace("option name=\"CONTINUATION_INDENT_SIZE\" value=\"8\"",
                                "option name=\"CONTINUATION_INDENT_SIZE\" value=\"$continuationIndentSize\"")
                    },
                Paths.get(dir.toString(), "options", "code.style.schemes.xml") to
                    overwriteWithResource("/config/options/code.style.schemes.xml") { content ->
                        content
                            .replace("option name=\"CURRENT_SCHEME_NAME\" value=\"ktlint\"",
                                "option name=\"CURRENT_SCHEME_NAME\" value=\"$codeStyleName\"")
                    },
                Paths.get(dir.toString(), "inspection", "ktlint.xml") to
                    overwriteWithResource("/config/inspection/ktlint.xml"),
                Paths.get(dir.toString(), "options", "editor.codeinsight.xml") to {
                    var arr = "<application></application>".toByteArray()
                    try {
                        arr = Files.readAllBytes(Paths.get(dir.toString(), "options", "editor.codeinsight.xml"))
                    } catch (e: IOException) {
                        if (e !is NoSuchFileException) {
                            throw e
                        }
                    }
                    enableOptimizeImportsOnTheFly(arr)
                }
            )
        } + sequenceOf(
            Paths.get(workDir.toString(), ".idea", "codeStyleSettings.xml") to
                overwriteWithResource("/config/.idea/codeStyleSettings.xml") { content ->
                    content.replace(
                        "option name=\"PREFERRED_PROJECT_CODE_STYLE\" value=\"ktlint\"",
                        "option name=\"PREFERRED_PROJECT_CODE_STYLE\" value=\"$codeStyleName\""
                    )
                },
            Paths.get(workDir.toString(), ".idea", "inspectionProfiles", "profiles_settings.xml") to
                overwriteWithResource("/config/.idea/inspectionProfiles/profiles_settings.xml")
        )).toList()
        if (!dryRun) {
            updates.forEach { (path, contentSupplier) ->
                Files.createDirectories(path.parent)
                Files.write(path, contentSupplier())
            }
        }
        return updates.map { (path) -> path }.toTypedArray()
    }

    private fun overwriteWithResource(resource: String, transformer: ((String) -> String) = { it }): () -> ByteArray = {
        transformer(getResourceText(resource)).toByteArray(charset("UTF-8"))
    }

    private fun enableOptimizeImportsOnTheFly(arr: ByteArray): ByteArray {
        /*
        <application>
          <component name="CodeInsightSettings">
            <option name="OPTIMIZE_IMPORTS_ON_THE_FLY" value="true" />
            ...
          </component>
          ...
        </application>
        */
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(arr))
        val xpath = XPathFactory.newInstance().newXPath()
        var cis = xpath.evaluate("//component[@name='CodeInsightSettings']",
            doc, XPathConstants.NODE) as Element?
        if (cis == null) {
            cis = doc.createElement("component")
            cis.setAttribute("name", "CodeInsightSettings")
            cis = doc.documentElement.appendChild(cis) as Element
        }
        var oiotf = xpath.evaluate("//option[@name='OPTIMIZE_IMPORTS_ON_THE_FLY']",
            cis, XPathConstants.NODE) as Element?
        if (oiotf == null) {
            oiotf = doc.createElement("option")
            oiotf.setAttribute("name", "OPTIMIZE_IMPORTS_ON_THE_FLY")
            oiotf = cis.appendChild(oiotf) as Element
        }
        oiotf.setAttribute("value", "true")
        val transformer = TransformerFactory.newInstance().newTransformer()
        val out = ByteArrayOutputStream()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        transformer.transform(DOMSource(doc), StreamResult(out))
        return out.toByteArray()
    }

    private fun getResourceText(name: String) =
        this::class.java.getResourceAsStream(name).readBytes().toString(Charset.forName("UTF-8"))

    class ProjectNotFoundException : RuntimeException()
}
