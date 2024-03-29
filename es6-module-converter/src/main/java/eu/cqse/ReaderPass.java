package eu.cqse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static eu.cqse.JsCodeUtils.IDENTIFIER_PATTERN;

public class ReaderPass {

	static final Pattern PROVIDE_OR_MODULE_PATTERN = Pattern
			.compile("(?m)^goog\\s*\\.\\s*(?:provide|module)\\s*\\(['\"]([\\w.]+)['\"]\\s*\\)\\s*;?");

	final Map<String, File> filesByNamespace = new HashMap<>();

	final Multimap<File, GoogRequireOrForwardDeclare> requiresByFile = ArrayListMultimap.create();
	final Multimap<File, GoogProvideOrModule> providesByFile = ArrayListMultimap.create();

	public static final String BASE_JS = "base.js";
	public static final String GOOG_JS = "goog.js";
	private static final Pattern DEFAULT_EXPORT_PATTERN = Pattern.compile("(?m)^\\s*exports\\s*=\\s*\\{?(([$\\w_,\\s+:*]|//.*|/\\**[^/]*(?<=\\*)/)+)}?;?");
	private static final Pattern DOTTED_EXPORT = Pattern.compile("(?m)^\\s*exports\\.([\\w_]+)\\s*=");
	private static final Pattern REQUIRE_PATTERN = Pattern.compile(
			"(?m)^(?:(?:const|let|var)\\s+(\\{?[\\w_, :]+}?)\\s*=\\s*)?goog\\s*\\.\\s*(?:require|requireType|forwardDeclare)[\\s\\r\\n]*\\(\\s*['\"]([\\w_.]+)['\"]\\s*\\)\\s*;?");

	void process(File... inputDirPaths) throws IOException {
		FileUtils.processRelevantJsFiles(this::processJsFile, inputDirPaths);
	}

	private void processJsFile(File jsFile) {
		String content = FileUtils.getFileContentSafe(jsFile);

		if (content.contains("goog.setTestOnly();")) {
			return;
		}

		List<GoogProvideOrModule> providesOrModules = getProvidedNamespaces(content);
		if (providesOrModules.isEmpty()) {
			if (!shouldIgnoreMissingProvide(jsFile)) {
				System.out.println(
						"INFO: " + jsFile.getAbsolutePath() + " does not seem to goog.provide or goog.module anything");
			}
			return;
		}

		List<GoogRequireOrForwardDeclare> googRequires = parseGoogRequires(content);
		addImplicitTypeOnlyGoogRequires(googRequires, content);
		insertProvidesAndRequiresForFile(jsFile, providesOrModules, googRequires);
	}

	private boolean shouldIgnoreMissingProvide(File jsFile) {
		return jsFile.getName().endsWith("Externs.js") ||
				jsFile.getName().equals("Index.js") ||
				jsFile.getPath().contains("demos") ||
				jsFile.getPath().contains("closure-deps") ||
				jsFile.getPath().contains("generate_closure_unit_tests") ||
				jsFile.getPath().contains("bootstrap") ||
				jsFile.getParent().endsWith("closure-library") ||
				jsFile.getParent().endsWith("goog") ||
				jsFile.getName().equals("base.js") ||
				jsFile.getName().equals("article.js") ||
				jsFile.getName().equals("deps.js");
	}

	private void addImplicitTypeOnlyGoogRequires(List<GoogRequireOrForwardDeclare> googRequires, String content) {
		Set<String> requires = googRequires.stream().map(r -> r.requiredNamespace).collect(Collectors.toSet());
		// Matches e.g.:
		// {?ts.data.Test=}
		// {!Listenable$$module$closure$goog$events$eventhandler|null}
		// {Element|string|function():Element=}
		// {!Array.<ts.data.Test>}
		Pattern requirePattern = Pattern.compile("(?m)(?:param|return|type|extends|typedef|private|protected|public)\\s*\\{([^}]+)}");
		Matcher matcher = requirePattern.matcher(content);
		while (matcher.find()) {
			String typeDefinition = matcher.group(1);
			String[] namespaces = typeDefinition.split("[^" + IDENTIFIER_PATTERN + ".]+");
			for (String namespace : namespaces) {
				if (namespace.equals("goog.net.XhrLike.OrNative")) {
					namespace = "goog.net.XhrLike";
				}
				if (namespace.contains(".") && !requires.contains(namespace) && !namespace.equals("Array.")) {
					googRequires.add(new GoogRequireOrForwardDeclare(null, namespace, null, List.of(),
							GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_LENIENT));
					requires.add(namespace);
				}
			}
		}
	}

	private void insertProvidesAndRequiresForFile(File jsFile, List<GoogProvideOrModule> providesOrModules, List<GoogRequireOrForwardDeclare> googRequires) {
		providesByFile.putAll(jsFile, providesOrModules);

		for (GoogProvideOrModule provideOrModule : providesOrModules) {
			if (filesByNamespace.containsKey(provideOrModule.namespace)) {
				throw new UnsupportedOperationException("Namespace " + provideOrModule.namespace + " is already provided by more than one file: " + jsFile + ", " + filesByNamespace.get(provideOrModule.namespace));
			}
			filesByNamespace.put(provideOrModule.namespace, jsFile);
		}

		requiresByFile.putAll(jsFile, googRequires);
	}

	private static List<GoogProvideOrModule> getProvidedNamespaces(String fileContent) {
		List<GoogProvideOrModule> provides = new ArrayList<>();
		Matcher matcher = PROVIDE_OR_MODULE_PATTERN.matcher(fileContent);
		while (matcher.find()) {
			String fullMatch = matcher.group();
			String namespace = matcher.group(1);
			boolean isModule = matcher.group().contains("module(");
			List<GoogModuleExport> exports = new ArrayList<>();
			if (isModule) {
				exports.addAll(extractExportsOfGoogModule(fileContent, namespace));
				if (exports.isEmpty()) {
					throw new RuntimeException(
							"Namespace '" + namespace + "' is provided as goog.module, but not exports were found");
				}
			}
			provides.add(new GoogProvideOrModule(namespace, isModule, exports, fullMatch));
		}
		return provides;
	}

	@VisibleForTesting
	static List<GoogModuleExport> extractExportsOfGoogModule(String fileContent, String module) {
		List<GoogModuleExport> googExports = new ArrayList<>();
		Matcher dottedExportMatcher = DOTTED_EXPORT.matcher(fileContent);
		while (dottedExportMatcher.find()) {
			String identifier = dottedExportMatcher.group(1).trim();
			googExports.add(new GoogModuleExport(new AliasedElement(identifier, identifier), true, dottedExportMatcher.group()));
		}
		Matcher defaultExportMatcher = DEFAULT_EXPORT_PATTERN.matcher(fileContent);
		while (defaultExportMatcher.find()) {
			String rawContent = defaultExportMatcher.group(1).replaceAll("/\\**[^/]*(?<=\\*)/", "").replaceAll("//.*", "");
			List<AliasedElement> exportedNames = new ArrayList<>();
			if (!defaultExportMatcher.group(0).contains("{")) {
				exportedNames.add(new AliasedElement(StringUtils.getLastPart(module, '.'), rawContent.trim()));
			} else if (rawContent.contains(",")) {
				exportedNames.addAll(Arrays.stream(rawContent.split(",")).filter(e -> !e.isBlank())
						.map(ReaderPass::normalizeExportEntry).collect(Collectors.toList()));
			} else {
				exportedNames.add(normalizeExportEntry(rawContent));
			}
			exportedNames.forEach(exportedName -> googExports.add(new GoogModuleExport(exportedName, false, defaultExportMatcher.group())));
		}
		return googExports;
	}

	private static AliasedElement normalizeExportEntry(String ns) {
		String[] split = ns.split(":");
		if (split.length == 1) {
			return new AliasedElement(split[0].trim());
		}
		return new AliasedElement(split[0].trim(), split[1].trim());
	}

	private static List<GoogRequireOrForwardDeclare> parseGoogRequires(String content) {
		List<GoogRequireOrForwardDeclare> requires = new ArrayList<>();
		// Matches e.g.:
		// goog.require('x.y.z');
		// }
		// const foo = goog.require('x.y.z')
		// const {foo} = goog.require("x.y.z"); <-- match will include the "{}"
		//
		// Groups(1) = short reference ('foo' or '{foo}' or n/a)
		// Groups(2) = required namespace
		Matcher matcher = REQUIRE_PATTERN.matcher(content);
		while (matcher.find()) {
			String requiredNamespace = matcher.group(2);
			String fullText = matcher.group();
			String rawShortReference = matcher.group(1);
			String shortReference = null;
			List<AliasedElement> importedFunctions = new ArrayList<>();
			if (rawShortReference != null && rawShortReference.contains("{")) {
				String[] importedFunctionStrings = rawShortReference.replaceAll("[{}\\s]", "").split(",");
				for (String importedFunctionString : importedFunctionStrings) {
					if (importedFunctionString.contains(":")) {
						String[] elements = importedFunctionString.split(":");
						importedFunctions.add(new AliasedElement(elements[1], elements[0]));
					} else {
						importedFunctions.add(new AliasedElement(importedFunctionString));
					}
				}
			} else if (rawShortReference != null) {
				shortReference = rawShortReference.trim();
			}
			boolean containsForwardDeclare = fullText.contains(".forwardDeclare(");
			GoogRequireOrForwardDeclare.ERequireType requireType;
			if (containsForwardDeclare) {
				requireType = GoogRequireOrForwardDeclare.ERequireType.GOOG_FORWARD_DECLARE;
			} else {
				requireType = GoogRequireOrForwardDeclare.ERequireType.GOOG_REQUIRE;
			}
			requires.add(new GoogRequireOrForwardDeclare(fullText, requiredNamespace, shortReference, importedFunctions,
					requireType));
		}

		return requires;
	}
}
