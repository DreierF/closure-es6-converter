package eu.cqse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ReaderPass extends Es6Processor {

	private static final Pattern PROVIDE_OR_MODULE_PATTERN = Pattern
			.compile("(?m)^goog\\s*\\.\\s*(?:provide|module)\\s*\\(['\"]([\\w.]+)['\"]\\s*\\)\\s*;?");

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
		Pattern defaultExportPattern = Pattern.compile("(?m)^\\s*exports\\s*=\\s*\\{?([\\w_,\\s+:]+)}?;?");
		Matcher defaultExportMatcher = defaultExportPattern.matcher(fileContent);
		while (defaultExportMatcher.find()) {
			String rawContent = defaultExportMatcher.group(1).trim();
			List<String> exportedNames = new ArrayList<>();
			if (rawContent.contains(",")) {
				exportedNames.addAll(Arrays.stream(rawContent.split(",")).map(ns -> ns.split(":")[0]).map(String::trim).collect(Collectors.toList()));
			} else {
				exportedNames.add(rawContent.split(":")[0].trim());
			}
			exportedNames.forEach(exportedName -> googExports.add(new GoogModuleExport(exportedName, false, defaultExportMatcher.group())));
		}
		return googExports;
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
			requires.add(new GoogRequireOrForwardDeclare(fullText, requiredNamespace, shortReference, importedFunction,
					fullText.contains(".forwardDeclare(")));
		}

		return requires;
	}
}
