package eu.cqse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SelectionPass {

	private static final Pattern PROVIDE_OR_MODULE_PATTERN = Pattern
			.compile("(?m)^goog\\s*\\.\\s*(?:provide|module)\\s*\\(['\"]([\\w.]+)['\"]\\s*\\)\\s*;?");

	private static final JsonAdapter<List<ClosureDependency>> JSON_ADAPTER = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, ClosureDependency.class));

	Set<String> process(File depsFile, File neededNamespacesFile, boolean includeTests) throws IOException {
		HashMap<String, ClosureDependency> depsByProvide = getStringClosureDependencies(depsFile);
		if (depsByProvide == null) return null;

		return calculateTransitiveClosure(neededNamespacesFile, depsByProvide, includeTests);
	}

	private Set<String> calculateTransitiveClosure(File neededNamespacesFile, HashMap<String, ClosureDependency> depsByProvide, boolean includeTests) throws IOException {
		ImmutableList<String> neededNamespaces = Files.asCharSource(neededNamespacesFile, Charsets.UTF_8).readLines();

		Set<String> transitivelyRequiredClosureFiles = new HashSet<>();
		Set<String> unsatisfiedDependencies = new HashSet<>(neededNamespaces);
		Set<String> processedDependencies = new HashSet<>();
		for (String unsatisfiedDependency : unsatisfiedDependencies) {
			ClosureDependency closureDependency = depsByProvide.get(unsatisfiedDependency);
			transitivelyRequiredClosureFiles.add(closureDependency.file);
			processedDependencies.add(unsatisfiedDependency);
			for (String require : closureDependency.requires) {
				if (!processedDependencies.contains(require)) {
					unsatisfiedDependencies.add(require);
				}
			}
			ClosureDependency closureTestDependency = depsByProvide.get(unsatisfiedDependency + "Test");
			if (includeTests && closureTestDependency != null) {
				transitivelyRequiredClosureFiles.add(closureTestDependency.file);
				processedDependencies.add(unsatisfiedDependency);
				for (String require : closureTestDependency.requires) {
					if (!processedDependencies.contains(require)) {
						unsatisfiedDependencies.add(require);
					}
				}
			}
		}
		return transitivelyRequiredClosureFiles;
	}

	private HashMap<String, ClosureDependency> getStringClosureDependencies(File depsFile) throws IOException {
		String content = Files.asCharSource(depsFile, Charsets.UTF_8).read();
		String jsonRepresentation = "[" + content.replaceAll("//.*", "")
				.replaceAll("goog.addDependency\\(([^,]+), ([^]]+]),([^]]+]), ([^)]+)\\);", "{'file': $1, 'provides': $2, 'requires': $3, 'info': $4},")
				.stripTrailing()
				.replace('\'', '"');
		jsonRepresentation = jsonRepresentation.substring(0, jsonRepresentation.length() - 1) + "]";

		List<ClosureDependency> closureDependencies = JSON_ADAPTER.fromJson(jsonRepresentation);
		if (closureDependencies == null) {
			System.err.println("Failed to parse json!");
			return null;
		}

		HashMap<String, ClosureDependency> depsByProvide = new HashMap<>();
		for (ClosureDependency dependency : closureDependencies) {
			for (String provide : dependency.provides) {
				depsByProvide.put(provide, dependency);
			}
		}
		return depsByProvide;
	}


	private List<GoogRequireOrForwardDeclare> parseImplicitRequires(String content, File jsFile) {
		List<GoogRequireOrForwardDeclare> requires = new ArrayList<>();
		if (content.contains("goog.dispose(") && !jsFile.getName().equals("disposable.js")) {
			requires.add(new GoogRequireOrForwardDeclare(null, "goog.dispose", null, "dispose", false));
		}
		if (!jsFile.getName().equals("base.js")) {
			requires.add(new GoogRequireOrForwardDeclare(null, "goog", "goog", null, false));
		}
		return requires;
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

	/** For named exports this assumes that the internal name and exported name always match. */
	@VisibleForTesting
	static List<GoogModuleExport> extractExportsOfGoogModule(String fileContent) {
		List<GoogModuleExport> googExports = new ArrayList<>();
		Pattern dottedExport = Pattern.compile("(?m)^\\s*exports\\.([\\w_]+)\\s*=");
		Matcher dottedExportMatcher = dottedExport.matcher(fileContent);
		while (dottedExportMatcher.find()) {
			String identifier = dottedExportMatcher.group(1).trim();
			googExports.add(new GoogModuleExport(identifier, true, dottedExportMatcher.group()));
		}
		Pattern defaultExportPattern = Pattern.compile("(?m)^\\s*exports\\s*=\\s*\\{?([\\w_,\\s+:/*@]+)}?;?");
		Matcher defaultExportMatcher = defaultExportPattern.matcher(fileContent);
		while (defaultExportMatcher.find()) {
			String rawContent = defaultExportMatcher.group(1).trim();
			List<String> exportedNames = new ArrayList<>();
			if (rawContent.contains(",")) {
				exportedNames.addAll(Arrays.stream(rawContent.split(",")).map(SelectionPass::normalizeExportEntry).collect(Collectors.toList()));
			} else {
				exportedNames.add(normalizeExportEntry(rawContent));
			}
			exportedNames.forEach(exportedName -> googExports.add(new GoogModuleExport(exportedName, false, defaultExportMatcher.group())));
		}
		return googExports;
	}

	private static String normalizeExportEntry(String ns) {
		return ns.split(":")[0].replaceAll("/\\**[^/]*(?<=\\*)/", "").trim();
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
				"(?m)^(?:(?:const|let|var)\\s+(\\{?[\\w_]+}?)\\s*=\\s*)?goog\\s*\\.\\s*(?:require|forwardDeclare)[\\s\\r\\n]*\\(['\"]([\\w_.]+)['\"]\\s*\\)\\s*;?");
		Matcher matcher = requirePattern.matcher(content);
		while (matcher.find()) {
			String requiredNamespace = matcher.group(2);
			if (requiredNamespace.endsWith("Template")
					|| StringUtils.equalsOneOf(requiredNamespace, "ts.commons.Constants", "ts.commons.Regex",
					"ts.Style", "ts.admin.InstanceComparisonTemplateDetailView",
					"ts.admin.InstanceComparisonTemplateOverview", "ts.data.ETeamscalePerspective")) {
				continue;
			}
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
			if (!requiredNamespace.equals("goog.dispose")) {
				requires.add(new GoogRequireOrForwardDeclare(fullText, requiredNamespace, shortReference, importedFunction,
						fullText.contains(".forwardDeclare(")));
			}
		}

		return requires;
	}
}
