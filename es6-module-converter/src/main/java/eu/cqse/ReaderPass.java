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

	void process(File... inputDirPaths) throws IOException {
		FileUtils.processRelevantJsFiles(this::processJsFile, inputDirPaths);
//		insertProvidesAndRequiresForFile(new File(inputDirPaths[0], "third_party/closure/goog/dojo/dom/query.js"), List.of(new GoogProvideOrModule("goog.dom.query", true, List.of(), null)), List.of());
//		insertProvidesAndRequiresForFile(new File(inputDirPaths[0], "third_party/closure/goog/mochikit/async/deferred.js"), List.of(new GoogProvideOrModule("goog.async.Deferred", true, List.of(), null)), List.of());
	}

	private void processJsFile(File jsFile) {
		String content = FileUtils.getFileContentSafe(jsFile);

		if (content.contains("goog.setTestOnly();")) {
			System.out.println("WARN: " + jsFile.getName() + " (" + jsFile.getAbsolutePath() + ") seems to be test-only, skipping file.");
			return;
		}

		List<GoogProvideOrModule> providesOrModules = getProvidedNamespaces(content);
		if (providesOrModules.isEmpty()) {
			if (!jsFile.getName().endsWith("Externs.js") && !jsFile.getName().equals("Index.js")) {
				System.out.println(
						"INFO: " + jsFile.getAbsolutePath() + " does not seem to goog.provide or goog.module anything");
			}
			return;
		}

		List<GoogRequireOrForwardDeclare> googRequires = parseGoogRequires(content);
		addImplicitTypeOnlyGoogRequires(googRequires, content);
		insertProvidesAndRequiresForFile(jsFile, providesOrModules, googRequires);
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
				if (namespace.contains(".") && !requires.contains(namespace) && !namespace.equals("Array.")) {
					googRequires.add(new GoogRequireOrForwardDeclare(null, namespace, null, null,
							GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_LENIENT));
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
				exports.addAll(extractExportsOfGoogModule(fileContent));
				if (exports.isEmpty()) {
					throw new RuntimeException(
							"Namespace '" + namespace + " is provided as goog.module, but not exports were found");
				}
			}
			provides.add(new GoogProvideOrModule(namespace, isModule, exports, fullMatch));
		}
		return provides;
	}

	@VisibleForTesting
	static List<GoogModuleExport> extractExportsOfGoogModule(String fileContent) {
		List<GoogModuleExport> googExports = new ArrayList<>();
		Pattern dottedExport = Pattern.compile("(?m)^\\s*exports\\.([\\w_]+)\\s*=");
		Matcher dottedExportMatcher = dottedExport.matcher(fileContent);
		while (dottedExportMatcher.find()) {
			String identifier = dottedExportMatcher.group(1).trim();
			googExports.add(new GoogModuleExport(new AliasedElement(identifier, identifier), true, dottedExportMatcher.group()));
		}
		Pattern defaultExportPattern = Pattern.compile("(?m)^\\s*exports\\s*=\\s*\\{?(([$\\w_,\\s+:*]|//.*|/\\**[^/]*(?<=\\*)/)+)}?;?");
		Matcher defaultExportMatcher = defaultExportPattern.matcher(fileContent);
		while (defaultExportMatcher.find()) {
			String rawContent = defaultExportMatcher.group(1).replaceAll("/\\**[^/]*(?<=\\*)/", "").replaceAll("//.*", "");
			List<AliasedElement> exportedNames = new ArrayList<>();
			if (rawContent.contains(",")) {
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
		Pattern requirePattern = Pattern.compile(
				"(?m)^(?:(?:const|let|var)\\s+(\\{?[\\w_]+}?)\\s*=\\s*)?goog\\s*\\.\\s*(?:require|forwardDeclare)[\\s\\r\\n]*\\(\\s*['\"]([\\w_.]+)['\"]\\s*\\)\\s*;?");
		Matcher matcher = requirePattern.matcher(content);
		while (matcher.find()) {
			String requiredNamespace = matcher.group(2);
			String fullText = matcher.group();
			String rawShortReference = matcher.group(1);
			String shortReference = null;
			String importedFunction = null;
			if (rawShortReference != null && rawShortReference.contains("{")) {
				if (rawShortReference.contains(",")) {
					throw new RuntimeException(
							"Found multiple function imports in '" + fullText + " ', which is unsupported.");
				}
				importedFunction = rawShortReference.replaceAll("[{}]", "");
			} else {
				shortReference = rawShortReference;
			}
			boolean containsForwardDeclare = fullText.contains(".forwardDeclare(");
			GoogRequireOrForwardDeclare.ERequireType requireType;
			if (containsForwardDeclare) {
				requireType = GoogRequireOrForwardDeclare.ERequireType.GOOG_FORWARD_DECLARE;
			} else {
				requireType = GoogRequireOrForwardDeclare.ERequireType.GOOG_REQUIRE;
			}
			requires.add(new GoogRequireOrForwardDeclare(fullText, requiredNamespace, shortReference, importedFunction,
					requireType));
		}

		return requires;
	}
}
