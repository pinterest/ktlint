package com.github.shyiko.ktlint.idea;

import com.github.shyiko.klob.Glob;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class KtLintIntellijIDEAIntegration {

    public static Path[] apply(final Path baseDir, final boolean dryRun) throws IOException {
        if (!Files.isDirectory(baseDir.resolve(".idea"))) {
            throw new ProjectNotFoundException();
        }
        final List<Path> result = new ArrayList<>();
        final BiConsumer<Path, Supplier<byte[]>> update = (path, contentSupplier) -> {
            result.add(path);
            if (!dryRun) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.write(path, contentSupplier.get());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
        final Function<String, Supplier<byte[]>> overwriteWithResource = (resource) -> () -> {
            try {
                return getResourceText(resource).getBytes("UTF-8");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        final String home = System.getProperty("user.home");
        final Stream<String> paths = Stream.concat(
            // macOS
            StreamSupport.stream(((Iterable<Path>)() ->
                Glob.from("IntelliJIdea*", "IdeaIC*", "AndroidStudio*")
                    .iterate(Paths.get(home, "Library", "Preferences"),
                        Glob.IterationOption.SKIP_CHILDREN, Glob.IterationOption.DIRECTORY)
            ).spliterator(), false),
            // linux/windows
            StreamSupport.stream(((Iterable<Path>)() ->
                Glob.from(".IntelliJIdea*/config", ".IdeaIC*/config", ".AndroidStudio*/config")
                    .iterate(Paths.get(home),
                        Glob.IterationOption.SKIP_CHILDREN, Glob.IterationOption.DIRECTORY)
            ).spliterator(), false)
        ).map(Path::toString);
        for (final String dir : (Iterable<String>) paths::iterator) {
            update.accept(Paths.get(dir, "codestyles", "ktlint.xml"),
                overwriteWithResource.apply("/config/codestyles/ktlint.xml"));
            update.accept(Paths.get(dir, "inspection", "ktlint.xml"),
                overwriteWithResource.apply("/config/inspection/ktlint.xml"));
            update.accept(Paths.get(dir, "options", "code.style.schemes.xml"),
                overwriteWithResource.apply("/config/options/code.style.schemes.xml"));
            update.accept(Paths.get(dir, "options", "editor.codeinsight.xml"),
                () -> {
                    byte[] in = "<application></application>".getBytes();
                    try {
                        in = Files.readAllBytes(Paths.get(dir, "options", "editor.codeinsight.xml"));
                    } catch (IOException e) {
                        if (!(e instanceof NoSuchFileException)) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    try {
                        return enableOptimizeImportsOnTheFly(in);
                    } catch (Exception e) {
                        throw new UncheckedIOException(new IOException(e));
                    }
                });
        }
        update.accept(Paths.get(baseDir.toString(), ".idea", "inspectionProfiles", "profiles_settings.xml"),
            overwriteWithResource.apply("/config/.idea/inspectionProfiles/profiles_settings.xml"));
        return result.toArray(new Path[0]);
    }

    private static byte[] enableOptimizeImportsOnTheFly(final byte[] in) throws Exception {
        /*
        <application>
          <component name="CodeInsightSettings">
            <option name="OPTIMIZE_IMPORTS_ON_THE_FLY" value="true" />
            ...
          </component>
          ...
        </application>
        */
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(in));
        XPath xpath = XPathFactory.newInstance().newXPath();
        Element cis = (Element) xpath.evaluate("//component[@name='CodeInsightSettings']",
            doc, XPathConstants.NODE);
        if (cis == null) {
            cis = doc.createElement("component");
            cis.setAttribute("name", "CodeInsightSettings");
            cis = (Element) doc.getDocumentElement().appendChild(cis);
        }
        Element oiotf = (Element) xpath.evaluate("//option[@name='OPTIMIZE_IMPORTS_ON_THE_FLY']",
            cis, XPathConstants.NODE);
        if (oiotf == null) {
            oiotf = doc.createElement("option");
            oiotf.setAttribute("name", "OPTIMIZE_IMPORTS_ON_THE_FLY");
            oiotf = (Element) cis.appendChild(oiotf);
        }
        oiotf.setAttribute("value", "true");
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }

    private static String getResourceText(final String name) {
        // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
        return new Scanner(Main.class.getResourceAsStream(name), "UTF-8").useDelimiter("\\A").next();
    }

    static class ProjectNotFoundException extends RuntimeException {}
}
