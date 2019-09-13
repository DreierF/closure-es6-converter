package eu.cqse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

class ConvertingPass {

	private static final Set<String> RESERVED_KEYWORDS = Set.of("Array",
			"Date", "Error", "File", "LogRecord", "Logger", "Map", "Notification", "Object", "ServiceWorker", "Set", "array",
			"console", "document", "localStorage", "number", "parseInt", "string", "window");

	private static final Map<String, String> DEFAULT_REPLACEMENTS = new HashMap<>();
	private static final Set<String> IMPORT_WHOLE_MODULE_EXCEPTIONS = ImmutableSet.of("GraphemeBreak", "CssSpecificity", "CssSanitizer", "ComponentUtil");
	private static final Set<String> IMPORT_CLASS_EXCEPTIONS = ImmutableSet.of("ts.dom");
	private static final String IDENTIFIER_PATTERN = "\\w$";

	static {
		DEFAULT_REPLACEMENTS.put("string", "strings");
		DEFAULT_REPLACEMENTS.put("number", "numbers");
	}

	void process(ReaderPass readerPass) throws IOException {
		for (File file : readerPass.providesByFile.keySet()) {
			String content = Files.asCharSource(file, Charsets.UTF_8).read();
			List<GoogProvideOrModule> provides = new ArrayList<>(readerPass.providesByFile.get(file));
			boolean isModule = provides.stream().anyMatch(provideOrModule -> provideOrModule.isModule);
			List<String> shortExports = new ArrayList<>();
			if (isModule) {
				GoogProvideOrModule googModule = provides.get(0);
				if (googModule.fullMatch == null) {
					// Skip classes that are already in ES6 format
					continue;
				}
				content = convertGoogleModuleFile(googModule, content);
				shortExports.addAll(googModule.exports.stream().map(e -> e.exportName.internalName).collect(Collectors.toList()));
			} else {
				content = convertGoogProvideFile(provides, file, content, shortExports);
			}
			content = replaceRequires(file, content, new ArrayList<>(readerPass.requiresByFile.get(file)),
					readerPass.filesByNamespace, shortExports);
			content = replaceSuppressedExtraRequires(content);
			content = content.replaceAll("(\\W)COMPILED(\\W)", "$1true$2");
			content = content.replaceAll("const ([" + IDENTIFIER_PATTERN + "]+) = goog.require('ts.([" + IDENTIFIER_PATTERN + "]+).([" + IDENTIFIER_PATTERN + "]+)');", "import {$1} from 'soy/ts/$2/$3'");
			content = content.replaceAll("const ([" + IDENTIFIER_PATTERN + "]+) = goog.require('ts.([" + IDENTIFIER_PATTERN + "]+).([" + IDENTIFIER_PATTERN + "]+).([" + IDENTIFIER_PATTERN + "]+)');", "import {$1} from 'soy/ts/$2/$3/$4'");
			Files.asCharSink(file, Charsets.UTF_8).write(content);
		}
	}

	/**
	 * '@suppress{extraRequires}' will no longer work in ES6 and cause a compiler error
	 */
	private String replaceSuppressedExtraRequires(String content) {
		return content.replaceAll("@suppress\\s*\\{extraRequire}", "");
	}

	/**
	 * let FOO = goog.define(...) is invalid an needs to use 'const' instead.
	 */
	@VisibleForTesting
	static String fixGoogDefineKeywords(String content, Collection<ExportedEntity> exportedNamespaces) {
		Matcher matcher = Pattern.compile("let\\s+([" + IDENTIFIER_PATTERN + "]+)\\s*=[\\s\\n]*goog\\s*\\.\\s*define\\s*\\(")
				.matcher(content);
		while (matcher.find()) {
			content = content.replace(matcher.group(), "const " + matcher.group(1) + " = goog.define(");
			exportedNamespaces.add(new ExportedEntity(matcher.group(1)));
		}
		matcher = Pattern.compile("(?m)^[\\s\\n]*goog\\s*\\.\\s*define\\s*\\('([^)]+\\.([^).]+))',").matcher(content);
		while (matcher.find()) {
			content = content.replace(matcher.group(), "const " + matcher.group(2) + " = " + matcher.group());
			content = replaceFullyQualifiedCallWith(content, matcher.group(1), matcher.group(2));
			exportedNamespaces.add(new ExportedEntity(matcher.group(2)));
		}
		return content;
	}

	private String replaceRequires(File file, String content, List<GoogRequireOrForwardDeclare> requires,
								   Map<String, File> filesByNamespace, List<String> shortExports) {
		Set<String> usedShortReferencesInFile = new HashSet<>(shortExports);
		requires.sort((require1, require2) -> require2.requiredNamespace.length() - require1.requiredNamespace.length());
		for (GoogRequireOrForwardDeclare require : requires) {
			File requiredFile = filesByNamespace.get(require.requiredNamespace);
			if (requiredFile == null || !requiredFile.isFile() || !requiredFile.canRead()) {
				throw new RuntimeException("Required namespace " + require.requiredNamespace + " could not be found "
						+ (requiredFile == null ? "" : requiredFile.getName()));
			}
			String relativePath = getRequirePathFor(file.getAbsolutePath(), requiredFile.getAbsolutePath());

			if (require.importedFunction != null) {
				content = replaceOrInsert(content, require.fullText, "import {" + require.importedFunction + "} from '" + relativePath + "';");
				usedShortReferencesInFile.add(require.importedFunction);
				continue;
			}

			if (require.shortReference == null) {
				require.shortReference = findSafeReferenceForGoogRequire(content, require.requiredNamespace,
						Sets.union(usedShortReferencesInFile, RESERVED_KEYWORDS));
				content = replaceFullyQualifiedCallWith(content, require.requiredNamespace, require.shortReference);
			}
			usedShortReferencesInFile.add(require.shortReference);

			String importedElement = StringUtils.getLastPart(require.requiredNamespace, ".");

			if ((!isClassName(importedElement) || IMPORT_WHOLE_MODULE_EXCEPTIONS.contains(importedElement)) && !IMPORT_CLASS_EXCEPTIONS.contains(require.requiredNamespace)) {
				content = replaceOrInsert(content, require.fullText, "import * as " + require.shortReference + " from '" + relativePath + "';"
				);
			} else if (importedElement.equals(require.shortReference)) {
				content = replaceOrInsert(content, require.fullText, "import {" + require.shortReference + "} from '" + relativePath + "';"
				);
			} else {
				content = replaceOrInsert(content,
						require.fullText, "import {" + importedElement + " as " + require.shortReference + "} from '" + relativePath + "';");
			}
		}
		return content;
	}

	private String replaceOrInsert(String content, String fullText, String replacement) {
		if (fullText == null) {
			int importIndex = content.indexOf("\nimport");
			int googIndex = content.indexOf("\ngoog");
			int firstImport;
			if (importIndex == -1) {
				firstImport = googIndex + 1;
			} else {
				firstImport = importIndex + 1;
			}
			return content.substring(0, firstImport) + replacement + "\n" + content.substring(firstImport);
		}
		// Ensure import is only present once
		String placeholder = "#!#!PLACEHOLDER_IMPORT#!#!#";
		return content.replace(fullText, replacement).replace(replacement, placeholder).replaceFirst(placeholder, replacement).replace(placeholder, StringUtils.EMPTY_STRING);
	}

	private static boolean isClassName(String importedElement) {
		return Character.isUpperCase(importedElement.charAt(0));
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

	private static String findSafeReferenceForGoogRequire(String documentText, String requiredNamespace,
														  Set<String> forbiddenShortReferences) {
		String[] namespaceParts = requiredNamespace.split("\\.");
		String newShortName = namespaceParts[namespaceParts.length - 1];

		boolean needsToBeUppercase = isClassName(newShortName);

		int namespacePartIndex = namespaceParts.length - 1;

		while (forbiddenShortReferences.contains(newShortName) || isShadedByVariableDefinition(documentText, newShortName)) {

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

		while (Pattern.compile("[^." + IDENTIFIER_PATTERN + "]" + newShortName + "\\.").matcher(documentText).find()) {
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

	private static boolean isShadedByVariableDefinition(String documentText, String newShortName) {
		return documentText.contains("var " + newShortName) || documentText.contains("let " + newShortName) || documentText.contains("const " + newShortName);
	}

	private String convertGoogProvideFile(List<GoogProvideOrModule> provides, File file,
										  String content, List<String> shortExports) {
		Set<ExportedEntity> exports = new TreeSet<>();

		content = fixGoogDefineKeywords(content, exports);

		provides.sort((provide1, provide2) -> provide2.namespace.length() - provide1.namespace.length());
		for (GoogProvideOrModule provide : provides) {
			content = rewriteFullyQualifiedNamespace(content, exports, provide.namespace, true);
			content = content.replaceAll(Pattern.quote(provide.fullMatch) + "\\s*", "");
		}


		if (exports.isEmpty()) {
			System.out.println("WARN: Don't know what to export, skipping: " + file.getPath());
			return content;
		} else {
			shortExports.addAll(exports.stream().map(e -> e.internalName).collect(Collectors.toSet()));
			return content + "\n\n" + "export {" + exports.stream().map(ExportedEntity::toEs6Fragment).collect(Collectors.joining(", ")) + "};";
		}
	}

	private String rewriteFullyQualifiedNamespace(String content, Set<ExportedEntity> exports, String namespace, boolean isProvided) {
		if (isProvideForClassOrEnum(namespace, content) || isTypeDef(namespace, content)) {
			// Class
			// foo.bar.MyClass -> MyClass
			// Typedefs:
			// foo.bar.MyClass; -> let MyClass;
			String shortClassName = getShortNameAndAddToExports(exports, namespace, isProvided);
			content = content.replaceAll("(?m)^" + multilineSafeNamespacePattern(namespace) + "( =|;)", "let " + shortClassName + "$1");
			return replaceFullyQualifiedCallWith(content, namespace, shortClassName);
		}
		// Prepare export of non-private methods
		Pattern methodOrConstantPattern = Pattern
				.compile("(?m)^" + multilineSafeNamespacePattern(namespace) + "\\s*\\.\\s*([" + IDENTIFIER_PATTERN + "]+)(\\s*=[^=])");
		Matcher matcher = methodOrConstantPattern.matcher(content);
		while (matcher.find()) {
			String methodOrConstantName = matcher.group(1);
			String internalMethodOrConstantName = methodOrConstantName;
			if (RESERVED_KEYWORDS.contains(internalMethodOrConstantName)) {
				internalMethodOrConstantName = "_" + internalMethodOrConstantName;
			}
			if (isPublicByConvention(methodOrConstantName) && isProvided) {
				exports.add(new ExportedEntity(methodOrConstantName, internalMethodOrConstantName));
			}
			content = content.replaceAll("(?m)^" + Pattern.quote(matcher.group()), "let " + safeReplaceString(internalMethodOrConstantName) + matcher.group(2));
			content = replaceFullyQualifiedCallWith(content, namespace + "." + methodOrConstantName,
					internalMethodOrConstantName);
		}
		// Prepare export of exported namespace typedefs e.g. goog.soy -> goog.soy.StrictTemplate
		Pattern typedefPattern = Pattern
				.compile("(?m)^" + multilineSafeNamespacePattern(namespace) + "\\s*\\.\\s*([" + IDENTIFIER_PATTERN + "]+);");
		matcher = typedefPattern.matcher(content);
		while (matcher.find()) {
			String typeName = matcher.group(1);
			if (isPublicByConvention(typeName) && isProvided) {
				exports.add(new ExportedEntity(typeName));
			}
			content = content.replace(matcher.group(), "let " + typeName + ";");
			content = replaceFullyQualifiedCallWith(content, namespace + "." + typeName,
					typeName);
		}
		return content;
	}

	private String getShortNameAndAddToExports(Set<ExportedEntity> exports, String namespace, boolean isProvided) {
		String[] parts = namespace.split("\\.");
		String classOrFunction = parts[parts.length - 1];
		String shortClassName = classOrFunction;
		if (RESERVED_KEYWORDS.contains(shortClassName)) {
			shortClassName = parts[parts.length - 2] + "_" + shortClassName;
		}
		if (isPublicByConvention(classOrFunction) && isProvided) {
			ExportedEntity e = new ExportedEntity(classOrFunction, shortClassName);
			exports.add(e);
		}
		return shortClassName;
	}

	private boolean isPublicByConvention(String classOrFunction) {
		return !classOrFunction.endsWith("_");
	}

	private static String safeReplaceString(String methodOrConstantName) {
		return methodOrConstantName.replace("$", "\\$");
	}

	private static String multilineSafeNamespacePattern(String namespace) {
		return Pattern.quote(namespace).replace(".", "\\E\\s*\\.\\s*\\Q");
	}

	private static String replaceFullyQualifiedCallWith(String content, String fullyQualifiedCall, String newCall) {
		return content.replaceAll("(?<!['\"" + IDENTIFIER_PATTERN + "])" + multilineSafeNamespacePattern(fullyQualifiedCall) + "(?!['\"" + IDENTIFIER_PATTERN + "])", safeReplaceString(newCall));
	}

	private boolean isProvideForClassOrEnum(String namespace, String content) {
		return Pattern.compile("(?m)^\\s*" + multilineSafeNamespacePattern(namespace) + "\\s*=\\s*(class|function\\s+)?").matcher(content).find();
	}

	private boolean isTypeDef(String namespace, String content) {
		return Pattern.compile("(?m)^\\s*" + multilineSafeNamespacePattern(namespace) + ";").matcher(content).find();
	}

	private String convertGoogleModuleFile(GoogProvideOrModule moduleOrProvide, String content) {
		content = content.replaceAll(Pattern.quote(moduleOrProvide.fullMatch) + "\\s*", "");
		content = content.replaceAll("goog\\.module\\.declareLegacyNamespace\\(\\);?\n?", "");

		List<GoogModuleExport> inlineExports = moduleOrProvide.exports.stream().filter(export -> export.isInlineExport).collect(toList());
		List<GoogModuleExport> globalExports = moduleOrProvide.exports.stream().filter(export -> !export.isInlineExport).collect(toList());

		for (GoogModuleExport export : inlineExports) {
			// Replace the closure version
			// 'exports foo ='
			// with
			// 'export foo ='
			content = content.replace(export.fullMatch, "export " + export.exportName.externalName + " =");
			content = content.replace("export " + export.exportName.externalName + " = " + export.exportName.externalName, "export {" + export.exportName.externalName + "}");
		}

		List<ExportedEntity> exportedNames = globalExports.stream().map(export -> export.exportName).collect(toList());
		content = fixGoogDefineKeywords(content, exportedNames);

		if (globalExports.isEmpty()) {
			return content;
		}

		// Remove Closure's 'exports {...}' and then append ES6's 'export {...}' to the file content
		content = content.replace(globalExports.get(0).fullMatch, "");
		return content + "\n\nexport {" + exportedNames.stream().map(ExportedEntity::toEs6Fragment).collect(Collectors.joining(", ")) + "};";
	}
}
