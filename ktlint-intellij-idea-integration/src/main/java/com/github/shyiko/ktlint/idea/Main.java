package com.github.shyiko.ktlint.idea;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        Arrays.sort(args);
        final boolean help = Arrays.binarySearch(args, "--help") > -1;
        final boolean version = Arrays.binarySearch(args, "--version") > -1;
        final boolean apply = Arrays.binarySearch(args, "apply") > -1 && !help;
        final boolean forceApply = Arrays.binarySearch(args, "-y") > -1;
        if (version) {
            System.err.println(Main.class.getPackage().getImplementationVersion());
            System.exit(0);
        }
        if (apply) {
            try {
                final Path workDir = Paths.get(".");
                if (!forceApply) {
                    final Path[] fileList = KtLintIntellijIDEAIntegration.apply(workDir, true);
                    System.err.println("The following files are going to be updated:\n\n\t" +
                        Arrays.stream(fileList).map(Path::toString).collect(Collectors.joining("\n\t")) +
                        "\n\nDo you wish to proceed? [y/n]\n" +
                        "(in future, use -y flag if you wish to skip confirmation)");
                    final Scanner scanner = new Scanner(System.in);
                    final String res = Stream.generate(() -> {
                            try { return scanner.next(); } catch (NoSuchElementException e) { return "n"; }
                        })
                        .filter(line -> !line.trim().isEmpty())
                        .findFirst()
                        .get();
                    if (!"y".equalsIgnoreCase(res)) {
                        System.err.println("(update canceled)");
                        System.exit(1);
                    }
                }
                KtLintIntellijIDEAIntegration.apply(workDir, false);
            } catch (KtLintIntellijIDEAIntegration.ProjectNotFoundException e) {
                System.err.println(".idea directory not found. " +
                    "Are you sure you are executing \"apply\" inside project root directory?");
                System.exit(1);
            }
            System.err.println("(updated)");
            System.err.println("\nPlease restart your IDE");
            System.err.println("(if you experience any issues please report them at https://github.com/shyiko/ktlint)");
        } else {
            System.err.println(
                "ktlint Intellij IDEA integration (https://github.com/shyiko/ktlint).\n" +
                "\n" +
                "Usage:\n" +
                "  ktlint-intellij-idea-integration <flags> apply\n" +
                "  java -jar ktlint-intellij-idea-integration <flags> apply\n" +
                "\n" +
                "Flags:\n" +
                "  --version : Version\n"
            );
            if (!help) {
                System.exit(1);
            }
        }
    }
}
