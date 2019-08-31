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

	private static final Pattern PROVIDE_OR_MODULE_PATTERN = Pattern
			.compile("(?m)^goog\\s*\\.\\s*(?:provide|module)\\s*\\(['\"]([\\w.]+)['\"]\\s*\\)\\s*;?");

	void process(String inputDirPath) throws IOException {
		File inputDir = new File(inputDirPath);

		if (!inputDir.isDirectory()) {
			throw new IOException("Input dir not found");
		}

		for (File file : Files.fileTraverser().breadthFirst(inputDir)) {
			if (isRelevantJsFile(file))
				processJsFile(file);
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

	static List<GoogProvideOrModule> getProvidedNamespaces(String fileContent) {
		List<GoogProvideOrModule> provides = new ArrayList<>();
		Matcher matcher = PROVIDE_OR_MODULE_PATTERN.matcher(fileContent);
		while (matcher.find()) {
			String fullMatch = matcher.group();
			String namespace = matcher.group(1);
			boolean isModule = matcher.group().contains("module(");
			List<String> exports = new ArrayList<>();
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

	private static List<String> extractExportsOfGoogModule(String fileContent) {
		List<String> exports = new ArrayList<>();
		Pattern dottedExport = Pattern.compile("(?m)^\\s*exports\\.([\\w_]+)\\s*=");
		Matcher dottedExportMatcher = dottedExport.matcher(fileContent);
		while (dottedExportMatcher.find()) {
			exports.add(dottedExportMatcher.group(1).trim());
		}
		Pattern defaultExportPattern = Pattern.compile("(?m)^\\s*exports\\s*=\\s*\\{?([\\w_,\\s+]+)}?");
		Matcher defaultExportMatcher = defaultExportPattern.matcher(fileContent);
		while (defaultExportMatcher.find()) {
			String rawContent = defaultExportMatcher.group(1).trim();
			if (rawContent.contains(",")) {
				exports.addAll(Arrays.stream(rawContent.split(",")).map(String::trim).collect(Collectors.toList()));
			} else {
				exports.add(rawContent);
			}
		}
		return exports;
	}

	static List<GoogRequireOrForwardDeclare> parseGoogRequires(String content) {

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

	protected abstract void processJsFile(File jsFile) throws IOException;

}
