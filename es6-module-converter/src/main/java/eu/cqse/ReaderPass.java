package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReaderPass extends Es6Processor {

    final Map<String, File> filesByNamespace = new HashMap<>();

    final Multimap<File, GoogRequireOrForwardDeclare> requiresByFile = ArrayListMultimap.create();
    final Multimap<File, GoogProvideOrModule> providesByFile = ArrayListMultimap.create();

    @Override
    protected void processJsFile(File jsFile) throws IOException {
        String content = Files.asCharSource(jsFile, Charsets.UTF_8).read();

        if (content.contains("goog.setTestOnly();")) {
            System.out.println("WARN: " + jsFile.getName() + " (" + jsFile.getAbsolutePath() + ") seems to be test-only, skipping file.");
            return;
        }

        List<GoogProvideOrModule> providesOrModules = getProvidedNamespaces(content);
        if (providesOrModules.isEmpty()) {
            System.out.println(
                    "INFO: " + jsFile.getAbsolutePath() + " does not seem to goog.provide or goog.module anything");
            return;
        }

        providesByFile.putAll(jsFile, providesOrModules);

        for (GoogProvideOrModule provideOrModule : providesOrModules) {
            if (filesByNamespace.containsKey(provideOrModule.namespace)) {
                throw new UnsupportedOperationException("Namespace " + provideOrModule.namespace + " is already provided by more than one file: " + jsFile.getName() + ", " + filesByNamespace.get(provideOrModule.namespace));
            }
            filesByNamespace.put(provideOrModule.namespace, jsFile);
        }

        List<GoogRequireOrForwardDeclare> googRequires = parseGoogRequires(content);
        requiresByFile.putAll(jsFile, googRequires);
    }
}
