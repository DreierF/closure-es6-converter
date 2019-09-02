package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ConvertingPass {

    private static final HashSet<String> RESERVED_KEYWORDS = new HashSet<>(Arrays.asList("document", "Array",
            "localStorage", "Map", "Set", "string", "number", "Object", "Notification", "Error", "Date"));

    private static final Map<String, String> DEFAULT_REPLACEMENTS = new HashMap<>();

    static {
        DEFAULT_REPLACEMENTS.put("string", "strings");
        DEFAULT_REPLACEMENTS.put("number", "numbers");
    }

    private static final Set<String> RESERVERD_KEYWORDS = new HashSet<>(Arrays.asList("Error", "console", "File",
            "document", "window", "Array", "Set", "Map", "Notification", "ServiceWorker", "string", "array"));

    void process(ReaderPass readerPass) throws IOException {
        for (File file : readerPass.providesByFile.keys()) {
            String content = Files.asCharSource(file, Charsets.UTF_8).read();
            List<GoogProvideOrModule> provides = new ArrayList<>(readerPass.providesByFile.get(file));
            boolean isModule = provides.stream().anyMatch(provideOrModule -> provideOrModule.isModule);
            if (isModule) {
                content = convertGoogleModuleFile(provides.get(0), file, content);
            } else {
                content = convertGoogProvideFile(provides, file, content);
            }
            content = replaceRequires(file, content, new ArrayList<>(readerPass.requiresByFile.get(file)),
                    readerPass.filesByNamespace);
            content = fixGoogDefineKeywords(content);
            content = replaceSupressedExtraRequires(content);
            Files.asCharSink(file, Charsets.UTF_8).write(content);
        }
    }

    /**
     * '@suppress{extraRequires}' will no longer work in ES6 and cause a compiler error
     */
    private String replaceSupressedExtraRequires(String content) {
        return content.replaceAll("@suppress\\s*\\{extraRequire}", "");
    }

    /**
     * let FOO = goog.define(...) is invalid an needs to use 'const' instead.
     */
    private String fixGoogDefineKeywords(String content) {
        Matcher matcher = Pattern.compile("let\\s+([A-Za-z_0-9]+)\\s*=[\\s\\n]*goog\\s*\\.\\s*define\\s*\\(")
                .matcher(content);
        while (matcher.find()) {
            content = content.replace(matcher.group(), "const " + matcher.group(1) + " = goog.define(");
        }
        return content;
    }

    private String replaceRequires(File file, String content, List<GoogRequireOrForwardDeclare> requires,
                                   Map<String, File> filesByNamespace) {
        Set<String> usedShortReferencesInFile = new HashSet<>();
        for (GoogRequireOrForwardDeclare require : requires) {
            File requiredFile = filesByNamespace.get(require.requiredNamespace);
            if (requiredFile == null || !requiredFile.isFile() || !requiredFile.canRead()) {
                throw new RuntimeException("Required namespace " + require.requiredNamespace + " could not be found "
                        + (requiredFile == null ? "" : requiredFile.getName()));
            }
            String relativePath = getRequirePathFor(file.getAbsolutePath(), requiredFile.getAbsolutePath());
            if (require.importedFunction != null) {
                content = content.replace(require.fullText,
                        "import {" + require.importedFunction + "} from '" + relativePath + "';");
                continue;
            }
            if (require.shortReference != null) {
                usedShortReferencesInFile.add(require.shortReference);
                content = content.replace(require.fullText,
                        "import * as " + require.shortReference + " from '" + relativePath + "';");
                continue;
            }

            String shortReference = findSafeReferenceForGoogRequire(content, require.requiredNamespace,
                    Sets.union(usedShortReferencesInFile, (RESERVED_KEYWORDS)));
            content = replaceFullyQualifiedCallWith(content, require.requiredNamespace, shortReference);
            content = content.replace(require.fullText,
                    "import * as " + shortReference + " from '" + relativePath + "';");
            usedShortReferencesInFile.add(shortReference);
        }
        return content;
    }

    private String getRequirePathFor(String callingFile, String targetFile) {
        Path caller = Paths.get(callingFile).getParent();
        Path targetPath = Paths.get(targetFile);
        String relativePath = caller.relativize(targetPath).toString().replaceAll("\\\\", "/");
        if (!relativePath.startsWith(".")) {
            return "./" + relativePath;
        }
        return relativePath;
    }

    public static String findSafeReferenceForGoogRequire(String documentText, String requiredNamespace,
                                                         Set<String> forbiddenShortReferences) {
        String[] namespaceParts = requiredNamespace.split("\\.");
        String newShortName = namespaceParts[namespaceParts.length - 1];

        boolean needsToBeUppercase = Character.isUpperCase(newShortName.charAt(0));

        int namespacePartIndex = namespaceParts.length - 1;

        while (forbiddenShortReferences.contains(newShortName)) {

            if (DEFAULT_REPLACEMENTS.containsKey(newShortName)) {
                newShortName = DEFAULT_REPLACEMENTS.get(newShortName);
                continue;
            }

            namespacePartIndex--;
            if (namespacePartIndex >= 0) {
                newShortName = namespaceParts[namespacePartIndex] + newShortName;
            } else {
                newShortName = "_" + newShortName;
            }
        }

        while (Pattern.compile("[^.\\w]" + newShortName + "\\.").matcher(documentText).find()) {
            namespacePartIndex--;
            if (namespacePartIndex >= 0) {
                newShortName = namespaceParts[namespacePartIndex] + "_" + newShortName;
            } else if (!newShortName.endsWith("s")) {
                newShortName += "s";
            } else {
                newShortName = "_" + newShortName;
            }
        }

        if (needsToBeUppercase) {
            newShortName = newShortName.substring(0, 1).toUpperCase() + newShortName.substring(1);
        }
        return newShortName;
    }

    private String convertGoogProvideFile(List<GoogProvideOrModule> provides, File file,
                                          final String originalContent) {

        String content = originalContent;
        Set<String> exports = new TreeSet<>();

        provides.sort((provide1, provide2) -> provide2.namespace.length() - provide1.namespace.length());
        for (GoogProvideOrModule provide : provides) {
            String namespace = provide.namespace;
            String[] parts = namespace.split("\\.");
            String classOrFunction = parts[parts.length - 1];
            if (isProvideForPublicClassOrEnum(classOrFunction, namespace, content)) {
                // foo.bar.MyClass -> MyClass
                String shortClassName = classOrFunction;
                if (RESERVERD_KEYWORDS.contains(shortClassName)) {
                    shortClassName = parts[parts.length - 2] + "_" + shortClassName;
                }
                content = content.replaceAll("(?m)^" + provide.fullMatch, shortClassName);
                exports.add(shortClassName);
            } else if (isTypeDef(classOrFunction, namespace, content)) {
                // Typedefs:
                // foo.bar.MyClass; -> let MyClass;
                String shortClassName = classOrFunction;
                if (RESERVERD_KEYWORDS.contains(shortClassName)) {
                    shortClassName = parts[parts.length - 2] + "_" + shortClassName;
                }
                content = content.replaceAll("(?m)^" + Pattern.quote(namespace) + ";", "let " + shortClassName + ";");
                exports.add(shortClassName);
            } else {
                // Prepare export of non-private methods
                Pattern methodOrConstantPattern = Pattern
                        .compile("(?m)^" + Pattern.quote(namespace) + "\\.([a-z_A-Z\\d]+[a-zA-Z\\d]+)\\s*=[^=]");
                Matcher matcher = methodOrConstantPattern.matcher(content);
                while (matcher.find()) {
                    String methodOrConstantName = matcher.group(1);
                    exports.add(methodOrConstantName);
                    content = content.replace(matcher.group(), "let " + methodOrConstantName + " =");
                    content = replaceFullyQualifiedCallWith(content, namespace + methodOrConstantName,
                            methodOrConstantName);
                }
            }

            content = content.replace(provide.fullMatch, "");
        }

        exports.removeIf(StringUtils::isEmpty);
        if (exports.isEmpty()) {
            System.out.println("WARN: Don't know what to export, skipping: " + file.getName());
            return originalContent;
        } else {
            return content + "\n\n" + "export {" + (exports.size() <= 1 ? exports.iterator().next()
                    : StringUtils.joinDifferentLastDelimiter(new ArrayList<>(exports), ", ", ", ")) + "};";
        }
    }

    private String replaceFullyQualifiedCallWith(String content, String fullyQualifiedCall, String newCall) {
        Matcher matcher = Pattern.compile("([^\\w])" + Pattern.quote(fullyQualifiedCall) + "([^\\w])").matcher(content);
        String[] invalidChars = {".", ",", "'", "\""};
        while (matcher.find()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(2);
            if (StringUtils.equalsOneOf(prefix, invalidChars) || StringUtils.equalsOneOf(suffix, invalidChars)) {
                continue;
            }
            content = content.replace(matcher.group(), prefix + newCall + suffix);
        }
        return content;
    }

    private boolean isProvideForPublicClassOrEnum(String classOrFunction, String namespace, String content) {
        return !classOrFunction.endsWith("_")
                && Pattern.compile(Pattern.quote(namespace) + "\\s*=\\s*(class|function\\s+)?").matcher(content).find();
    }

    private boolean isTypeDef(String classOrFunction, String namespace, String content) {
        return !classOrFunction.endsWith("_")
                && Pattern.compile("(?m)^" + Pattern.quote(namespace) + ";").matcher(content).find();
    }

    private String convertGoogleModuleFile(GoogProvideOrModule moduleOrProvide, File file,
                                           String content) {
        content = content.replace(moduleOrProvide.fullMatch, "");
        content = content.replace("goog.module.declareLegacyNamespace();", "");
        content = content.replaceAll("exports\\s*=\\s*\\{.*?};?", "");
        content = content + "\n\nexport {"
                + (moduleOrProvide.exports.size() > 1 ? String.join(",", moduleOrProvide.exports)
                : moduleOrProvide.exports.get(0))
                + "};";
        return content;
    }
}
