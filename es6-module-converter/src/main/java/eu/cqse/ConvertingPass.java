package eu.cqse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.cqse.FileUtils.getFileContentSafe;
import static eu.cqse.ReaderPass.BASE_JS;
import static eu.cqse.ReaderPass.GOOG_JS;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

class ConvertingPass {

	private static final Set<String> RESERVED_KEYWORDS = Set.of("Array",
			"Date", "Error", "File", "LogRecord", "Logger", "Map", "Notification", "Object", "ServiceWorker", "Set", "array",
			"console", "document", "localStorage", "number", "parseInt", "string", "window", "Element", "Event",
			"MouseEvent", "BrowserEvent", "EventTarget", "Node", "Document", "FileReader", "ProgressEvent",
			"XmlHttpFactory", "Promise");

	private static final Map<String, String> DEFAULT_REPLACEMENTS = ImmutableMap.of(
			"string", "strings",
			"number", "numbers"
	);

	private static final Set<String> IMPORT_WHOLE_MODULE_EXCEPTIONS = ImmutableSet.of("goog.i18n.GraphemeBreak",
			"goog.html.CssSpecificity", "goog.html.sanitizer.CssSanitizer", "goog.ui.ComponentUtil",
			"goog.html.sanitizer.CssPropertySanitizer", "goog.debug.entryPointRegistry", "goog.userAgent",
			"goog.i18n.uChar", "goog.dom.animationFrame", "goog.dom.BrowserFeature");

	private static final Set<String> IMPORT_CLASS_EXCEPTIONS = ImmutableSet.of("ts.dom", "goog.dispose");
	private static final Pattern ASSIGNED_GOOG_DEFINE_PATTERN = Pattern.compile("(?:let\\s+)?([" + JsCodeUtils.IDENTIFIER_PATTERN + ".]+)\\s*=[\\s\\n]*goog\\s*\\.\\s*define\\s*\\(\\s*'([^']+\\.([^'.]+))',\\s*([^)]+)\\);?");
	private static final Set<String> GOOG_IMPORTED_ELEMENTS = Set.of("goog.global",
			"goog.require",
			"goog.isString",
			"goog.isBoolean",
			"goog.isNumber",
			"goog.define",
			"goog.DEBUG",
			"goog.LOCALE",
			"goog.TRUSTED_SITE",
			"goog.STRICT_MODE_COMPATIBLE",
			"goog.DISALLOW_TEST_ONLY_CODE",
			"goog.FEATURESET_YEAR",
			"goog.module.get",
			"goog.setTestOnly",
			"goog.forwardDeclare",
			"goog.getObjectByName",
			"goog.basePath",
			"goog.addSingletonGetter",
			"goog.typeOf",
			"goog.isArray",
			"goog.isArrayLike",
			"goog.isDateLike",
			"goog.isFunction",
			"goog.isObject",
			"goog.getUid",
			"goog.hasUid",
			"goog.removeUid",
			"goog.mixin",
			"goog.now",
			"goog.globalEval",
			"goog.getCssName",
			"goog.setCssNameMapping",
			"goog.getMsg",
			"goog.getMsgWithFallback",
			"goog.exportSymbol",
			"goog.exportProperty",
			"goog.isDef",
			"goog.isNull",
			"goog.isDefAndNotNull",
			"goog.globalize",
			"goog.nullFunction",
			"goog.abstractMethod",
			"goog.removeHashCode",
			"goog.getHashCode",
			"goog.cloneObject",
			"goog.bind",
			"goog.partial",
			"goog.inherits",
			"goog.base",
			"goog.scope",
			"goog.defineClass",
			"goog.declareModuleId", "goog.tagUnsealableClass");

	void process(ReaderPass readerPass) throws IOException {
		for (File file : readerPass.providesByFile.keySet()) {
			String content = getFileContentSafe(file);
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
			List<GoogRequireOrForwardDeclare> requires = extendRequires(file, readerPass, content);
			content = replaceRequires(file, content, requires, readerPass.filesByNamespace, shortExports);
			content = replaceSuppressedExtraRequires(content);

			// Remove namespaces from non officially exported elements
			HashSet<String> remainingGoogNamespaces = getRemainingGoogNamespaces(content);
			for (String namespace : remainingGoogNamespaces) {
				// TODO Might need to be imported to work properly (AFAIKS only happens for type comments where the type is only used in the comment)
				// TODO still needed?
				content = rewriteFullyQualifiedNamespace(content, Collections.emptySet(), namespace, false);
//				content = replaceFullyQualifiedCallWith(content, namespace, StringUtils.getLastPart(namespace, '.'));
			}

			content = content.replaceAll("(\\W)COMPILED(\\W)", "$1true$2");
			content = content.replace("* @define {", "* @type {");
			FileUtils.writeFileContent(file, content);
		}
	}

	private List<GoogRequireOrForwardDeclare> extendRequires(File file, ReaderPass readerPass, String content) {
		Collection<GoogRequireOrForwardDeclare> requires = readerPass.requiresByFile.get(file);
		List<GoogRequireOrForwardDeclare> extendedRequires = new ArrayList<>(requires);
		Set<String> requiredNamespaces = requires.stream().map(r -> r.requiredNamespace).collect(toSet());
		for (String requiredNamespace : requiredNamespaces) {
			Collection<GoogProvideOrModule> similarProvides = readerPass.providesByFile.get(readerPass.filesByNamespace.get(requiredNamespace));
			for (GoogProvideOrModule similarProvide : similarProvides) {
				if (!requiredNamespaces.contains(similarProvide.namespace) && similarProvide.namespace.startsWith(requiredNamespace) && content.contains(similarProvide.namespace)) {
					extendedRequires.add(new GoogRequireOrForwardDeclare(null, similarProvide.namespace, null, null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
				}
			}
		}

		if (!file.getName().equals(GOOG_JS) && !file.getName().equals(BASE_JS)) { // TODO optimize way too slow
			if (content.contains("goog.dispose(") && !file.getName().equals("disposable.js") && !requiredNamespaces.contains("goog.dispose")) {
				extendedRequires.add(new GoogRequireOrForwardDeclare(null, "goog.dispose", null, null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
			}
			if (content.contains("goog.disposeAll(") && !file.getName().equals("disposable.js") && !requiredNamespaces.contains("goog.disposeAll")) {
				extendedRequires.add(new GoogRequireOrForwardDeclare(null, "goog.disposeAll", null, null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
			}
			if (content.contains("goog.a11y.aria.State") && !file.getName().equals("attributes.js") && !requiredNamespaces.contains("goog.a11y.aria.State")) {
				extendedRequires.add(new GoogRequireOrForwardDeclare(null, "goog.a11y.aria.State", null, null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
			}
			if (content.contains("goog.string.Const") && !file.getName().equals("const.js") && !requiredNamespaces.contains("goog.string.Const")) {
				extendedRequires.add(new GoogRequireOrForwardDeclare(null, "goog.string.Const", null, null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
			}
			if (Pattern.compile("goog\\.string\\.(startsWith|endsWith|caseInsensitiveContains)").matcher(content).find() && !file.getName().equals("string.js") && !requiredNamespaces.contains("goog.string")) {
				extendedRequires.add(new GoogRequireOrForwardDeclare(null, "goog.string", null, null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
			}
			extendedRequires.add(new GoogRequireOrForwardDeclare(null, "goog", "goog", null, GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_STRICT));
		}

		return extendedRequires;
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
	static String fixGoogDefineKeywords(String content, Collection<AliasedElement> exportedNamespaces) {
		Matcher matcher = ASSIGNED_GOOG_DEFINE_PATTERN.matcher(content);
		while (matcher.find()) {
			String currentReference = matcher.group(1);
			String fullReference = matcher.group(2);
			String shortReference = matcher.group(3);
			String defaultValue = matcher.group(4);
			content = content.replace(matcher.group(), "const " + shortReference + " = " + defaultValue + ";");
			if (!currentReference.equals(fullReference)) {
				content = replaceFullyQualifiedCallWith(content, currentReference, shortReference);
			}
			content = replaceFullyQualifiedCallWith(content, fullReference, shortReference);
			exportedNamespaces.add(new AliasedElement(shortReference));
			matcher = ASSIGNED_GOOG_DEFINE_PATTERN.matcher(content);
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
				if (require.requireType == GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_LENIENT) {
					continue;
				}
				throw new RuntimeException("Required namespace " + require.requiredNamespace + " could not be found "
						+ (requiredFile == null ? "" : requiredFile.getName()));
			}
			if (file.getAbsoluteFile().equals(requiredFile.getAbsoluteFile())) {
				// No need to include types from itself
				continue;
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

			if (shouldImportAsModule(require, importedElement)) {
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

	private boolean shouldImportAsModule(GoogRequireOrForwardDeclare require, String importedElement) {
		if (IMPORT_CLASS_EXCEPTIONS.contains(require.requiredNamespace)) {
			return false;
		}
		if (isClassName(importedElement) && importedElement.contains("Template")) {
			return true;
		}
		return !isClassName(importedElement) && !isFunctionName(importedElement)
				|| IMPORT_WHOLE_MODULE_EXCEPTIONS.contains(require.requiredNamespace);
	}

	private String replaceOrInsert(String content, String fullText, String replacement) {
		if (fullText == null) {
			return replacement + "\n" + content.replace(replacement, StringUtils.EMPTY_STRING);
		}
		// Ensure import is only present once
		return replacement + "\n" + content.replace(fullText, StringUtils.EMPTY_STRING).replace(replacement, StringUtils.EMPTY_STRING);
	}

	private static boolean isClassName(String importedElement) {
		return Character.isUpperCase(importedElement.charAt(0));
	}

	private static boolean isFunctionName(String importedElement) {
		return importedElement.matches(".*[A-Z].*");
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

		while (Pattern.compile("[^." + JsCodeUtils.IDENTIFIER_PATTERN + "]" + newShortName + "\\.").matcher(documentText).find()) {
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
		Set<AliasedElement> exports = new TreeSet<>();

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
			return content + "\n\n" + "export {" + exports.stream().map(AliasedElement::toEs6Fragment).collect(Collectors.joining(", ")) + "};";
		}
	}

	private HashSet<String> getRemainingGoogNamespaces(String content) {
		Matcher matcher = Pattern.compile("goog\\.[" + JsCodeUtils.IDENTIFIER_PATTERN + ".]+(?<!\\.)").matcher(content);
		HashSet<String> remainingGoogNamespaces = new HashSet<>();
		while (matcher.find()) {
			String namespace = matcher.group();
			if (!namespace.equals(GOOG_JS) && !namespace.contains("prototype") && !StringUtils.startsWithOneOf(namespace, GOOG_IMPORTED_ELEMENTS)) {
				remainingGoogNamespaces.add(namespace);
			}
		}
		return remainingGoogNamespaces;
	}

	private String rewriteFullyQualifiedNamespace(String content, Set<AliasedElement> exports, String namespace, boolean isProvided) {
		if (isTypeDef(namespace, content)) {
			// Typedefs:
			// foo.bar.MyClass; -> let MyClass;
			String shortClassName = getShortNameAndAddToExports(exports, namespace, isProvided);
			content = content.replaceAll("(?m)^" + JsCodeUtils.multilineSafeNamespacePattern(namespace) + ";", "let " + shortClassName + ";");
			return replaceFullyQualifiedCallWith(content, namespace, shortClassName);
		}
		if (isProvideForClassOrEnum(namespace, content)) {
			// Class
			// foo.bar.MyClass -> MyClass
			String shortClassName = getShortNameAndAddToExports(exports, namespace, isProvided);
			content = content.replaceAll("(?m)^" + JsCodeUtils.multilineSafeNamespacePattern(namespace) + " =", "let " + shortClassName + " =");
			return replaceFullyQualifiedCallWith(content, namespace, shortClassName);
		}
		// Prepare export of non-private methods
		Pattern methodOrConstantPattern = Pattern
				.compile("(?m)^" + JsCodeUtils.multilineSafeNamespacePattern(namespace) + "\\s*\\.\\s*([" + JsCodeUtils.IDENTIFIER_PATTERN + "]+)(\\s*=[^=])");
		Matcher matcher = methodOrConstantPattern.matcher(content);
		while (matcher.find()) {
			String methodOrConstantName = matcher.group(1);
			String internalMethodOrConstantName = methodOrConstantName;
			if (RESERVED_KEYWORDS.contains(internalMethodOrConstantName)) {
				internalMethodOrConstantName = "_" + internalMethodOrConstantName;
			}
			if (isPublicByConvention(methodOrConstantName) && isProvided) {
				exports.add(new AliasedElement(methodOrConstantName, internalMethodOrConstantName));
			}
			content = content.replaceAll("(?m)^" + Pattern.quote(matcher.group()), "let " + JsCodeUtils.safeReplaceString(internalMethodOrConstantName) + matcher.group(2));
			content = replaceFullyQualifiedCallWith(content, namespace + "." + methodOrConstantName,
					internalMethodOrConstantName);
		}
		// Prepare export of exported namespace typedefs e.g. goog.soy -> goog.soy.StrictTemplate
		Pattern typedefPattern = Pattern
				.compile("(?m)^" + JsCodeUtils.multilineSafeNamespacePattern(namespace) + "\\s*\\.\\s*([" + JsCodeUtils.IDENTIFIER_PATTERN + "]+);");
		matcher = typedefPattern.matcher(content);
		while (matcher.find()) {
			String typeName = matcher.group(1);
			if (isPublicByConvention(typeName) && isProvided) {
				exports.add(new AliasedElement(typeName));
			}
			content = content.replace(matcher.group(), "let " + typeName + ";");
			content = replaceFullyQualifiedCallWith(content, namespace + "." + typeName,
					typeName);
		}
		return content;
	}

	private String getShortNameAndAddToExports(Set<AliasedElement> exports, String namespace, boolean isProvided) {
		String[] parts = namespace.split("\\.");
		String classOrFunction = parts[parts.length - 1];
		String shortClassName = classOrFunction;
		if (RESERVED_KEYWORDS.contains(shortClassName)) {
			shortClassName = parts[parts.length - 2] + "_" + shortClassName;
		}
		if (isPublicByConvention(classOrFunction) && isProvided) {
			AliasedElement e = new AliasedElement(classOrFunction, shortClassName);
			exports.add(e);
		}
		return shortClassName;
	}

	private boolean isPublicByConvention(String classOrFunction) {
		return !classOrFunction.endsWith("_");
	}

	private static String replaceFullyQualifiedCallWith(String content, String fullyQualifiedCall, String newCall) {
		return content.replaceAll("(?<!['\"/" + JsCodeUtils.IDENTIFIER_PATTERN + "])" + JsCodeUtils.multilineSafeNamespacePattern(fullyQualifiedCall) + "(?!['\"/" + JsCodeUtils.IDENTIFIER_PATTERN + "])", JsCodeUtils.safeReplaceString(newCall));
	}

	private boolean isProvideForClassOrEnum(String namespace, String content) {
		return Pattern.compile("(?m)^\\s*" + JsCodeUtils.multilineSafeNamespacePattern(namespace) + "\\s*=\\s*(class|function\\s+)?").matcher(content).find();
	}

	private boolean isTypeDef(String namespace, String content) {
		return Pattern.compile("(?m)^\\s*" + JsCodeUtils.multilineSafeNamespacePattern(namespace) + ";").matcher(content).find();
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
			content = content.replace(export.fullMatch, "export const " + export.exportName.externalName + " =");
			content = content.replace("export const " + export.exportName.externalName + " = " + export.exportName.externalName, "export {" + export.exportName.externalName + "}");
		}

		List<AliasedElement> exportedNames = globalExports.stream().map(export -> export.exportName).collect(toList());
		content = fixGoogDefineKeywords(content, exportedNames);

		if (globalExports.isEmpty()) {
			return content;
		}

		// Remove Closure's 'exports {...}' and then append ES6's 'export {...}' to the file content
		content = content.replace(globalExports.get(0).fullMatch, "");
		return content + "\n\nexport {" + exportedNames.stream().map(AliasedElement::toEs6Fragment).collect(Collectors.joining(", ")) + "};";
	}
}
