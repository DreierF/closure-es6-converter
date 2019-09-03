package eu.cqse;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class Es6Processor {

    void process(String inputDirPath) throws IOException {
        File inputDir = new File(inputDirPath);

        if (!inputDir.isDirectory()) {
            throw new IOException("Input dir not found");
        }

        for (File file : Files.fileTraverser().breadthFirst(inputDir)) {
            if (isRelevantJsFile(file)) {
                processJsFile(file);
            }
        }

        System.out.println("\n==== Finished ====");
    }

    private boolean isRelevantJsFile(File file) {
        return file.getName().toLowerCase().endsWith(".js")
                && !StringUtils.containsOneOf(file.getAbsolutePath(), "\\less\\", "js-cache", "\\testing\\")
                && StringUtils.containsOneOf(file.getAbsolutePath(), "closure-library", "src-js",
                "third_party/closure/")
                && !StringUtils.endsWithOneOf(file.getAbsolutePath().toLowerCase(), "_test.js", "_perf.js", "tester.js",
                "alltests.js", "testhelpers.js", "testing.js", "relativecommontests.js", "mockiframeio.js");
    }


    protected abstract void processJsFile(File jsFile) throws IOException;
}
