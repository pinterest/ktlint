package com.github.shyiko.ktlint.idea;

import com.github.shyiko.klob.Glob;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class KtLintIntellijIDEAIntegration {

    public static Path[] apply(final Path baseDir, final boolean dryRun) throws IOException {
        if (!Files.isDirectory(baseDir.resolve(".idea"))) {
            throw new ProjectNotFoundException();
        }
        final List<Path> result = new ArrayList<>();
        final BiConsumer<Path, String> update = (path, resource) -> {
            result.add(path);
            if (!dryRun) {
                try {
                    Files.createDirectories(path.getParent());
                    Files.write(path, getResourceText(resource).getBytes("UTF-8"));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
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
            update.accept(Paths.get(dir, "codestyles", "ktlint.xml"), "/config/codestyles/ktlint.xml");
            update.accept(Paths.get(dir, "inspection", "ktlint.xml"), "/config/inspection/ktlint.xml");
            update.accept(Paths.get(dir, "options", "code.style.schemes.xml"),
                "/config/options/code.style.schemes.xml");
        }
        update.accept(Paths.get(baseDir.toString(), ".idea", "inspectionProfiles", "profiles_settings.xml"),
            "/config/.idea/inspectionProfiles/profiles_settings.xml");
        return result.toArray(new Path[0]);
    }

    private static String getResourceText(final String name) {
        // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
        return new Scanner(Main.class.getResourceAsStream(name), "UTF-8").useDelimiter("\\A").next();
    }

    static class ProjectNotFoundException extends RuntimeException {}
}
