package com.github.shyiko.ktlint.idea;

import com.esotericsoftware.wildcard.Paths;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    private Main() {}

    private static void writeToFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        final PrintWriter writer = new PrintWriter(file, "UTF-8");
        try {
            writer.println(content);
        } finally {
            writer.close();
        }
    }

    private static String getResourceText(String name) {
        // https://community.oracle.com/blogs/pat/2004/10/23/stupid-scanner-tricks
        return new Scanner(Main.class.getResourceAsStream(name), "UTF-8").useDelimiter("\\A").next();
    }

    public static void main(String[] args) throws IOException {
        Arrays.sort(args);
        final boolean help = Arrays.binarySearch(args, "--help") > -1;
        final boolean version = Arrays.binarySearch(args, "--version") > -1;
        final boolean apply = Arrays.binarySearch(args, "apply") > -1 && !help;
        final boolean dryRun = Arrays.binarySearch(args, "--dry-run") > -1;
        if (version) {
            System.err.println(Main.class.getPackage().getImplementationVersion());
            System.exit(0);
        }
        if (apply) {
            if (!new File(".idea").isDirectory()) {
                System.err.println(".idea directory not found. " +
                    "Are you sure you are executing ktlint-intellij-idea-integration inside project root directory?");
                return;
            }
            final String home = System.getProperty("user.home");
            // https://github.com/EsotericSoftware/wildcard
            final Paths paths = new Paths();
            // macOS
            paths.glob(
                new File(new File(home, "Library"), "Preferences").getAbsolutePath(),
                "IntelliJIdea*",
                "IdeaIC*",
                "AndroidStudio*"
            );
            // linux/windows
            paths.glob(
                home,
                ".IntelliJIdea*/config",
                ".IdeaIC*/config",
                ".AndroidStudio*/config"
            );
            for (String dir : paths.dirsOnly()) {
                final File codeStyleConfig = new File(new File(dir, "codestyles"), "ktlint.xml");
                System.err.println("Writing " + codeStyleConfig.toString());
                if (!dryRun) {
                    writeToFile(codeStyleConfig, getResourceText("/config/codestyles/ktlint.xml"));
                }
                final File inspectionConfig = new File(new File(dir, "inspection"), "ktlint.xml");
                System.err.println("Writing " + inspectionConfig.toString());
                if (!dryRun) {
                    writeToFile(inspectionConfig, getResourceText("/config/inspection/ktlint.xml"));
                }
                final File activeCodeStyleConfig = new File(new File(dir, "options"), "code.style.schemes.xml");
                System.err.println("Writing " + activeCodeStyleConfig.toString());
                if (!dryRun) {
                    writeToFile(activeCodeStyleConfig, getResourceText("/config/options/code.style.schemes.xml"));
                }
            }
            final File projectActiveInspectionProfileConfig =
                new File(new File(".idea", "inspectionProfiles"), "profiles_settings.xml");
            System.err.println("Writing " + projectActiveInspectionProfileConfig.toString());
            if (!dryRun) {
                writeToFile(projectActiveInspectionProfileConfig,
                    getResourceText("/config/.idea/inspectionProfiles/profiles_settings.xml"));
                System.err.println("\nPlease restart you IDE");
                System.err.println("(if you experience any issues please report them at https://github.com/shyiko/ktlint)");
            }
        } else {
            System.err.println(
                "ktlint Intellij IDEA integration (https://github.com/shyiko/ktlint).\n" +
                "\n" +
                "Usage:\n" +
                "  ktlint-intellij-idea-integration <flags> apply\n" +
                "  java -jar ktlint-intellij-idea-integration <flags> apply\n" +
                "\n" +
                "Flags:\n" +
                "  --dry-run : Do not modify anything, just show what's going to happen\n" +
                "  --version : Version\n"
            );
            if (!help) {
                System.exit(1);
            }
        }
    }
}
