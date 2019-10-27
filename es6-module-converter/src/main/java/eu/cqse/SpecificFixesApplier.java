package eu.cqse;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecificFixesApplier {
	private final File outputDir;

	public SpecificFixesApplier(File outputDir) {
		this.outputDir = outputDir;
	}

	private static final String DOCUMENTED_PATTERN = "(?ms)^(/\\*\\*((?!\\*/).)*\\*/\\s*)";

	public void process() throws IOException {
//		replace("db/error.js", "goog.provide('goog.db.DomErrorLike')", "goog.provide('goog.db.DOMErrorLike')");
		replace("events/browserfeature.js", "goog.scope(function() {", "");
		replace("events/browserfeature.js", "});  // goog.scope", "");
		replace("net/xhrio.js", "goog.scope(function() {", "");
		replace("net/xhrio.js", "var ResponseType = goog.net.XhrIo.ResponseType;\n", "");
		replaceAll("net/xhrio.js", Pattern.compile("ResponseType\\.[A-Z_]+"), "goog.net.XhrIo.$0");
		replace("net/xhrio.js", "var XhrIo = goog.net.XhrIo;\n", "");
		replace("net/xhrio.js", "XhrIo.base", "goog.net.XhrIo.base");
		replace("net/xhrio.js", "});  // goog.scope", "");
		replace("events/events.js", "*/\ngoog.events.unlistenByKey",
				" * @suppress {checkTypes}\n */\ngoog.events.unlistenByKey");
		replace("iter/iter.js", "var product", "var productVar");
		replace("iter/iter.js", "product,", "productVar,");
		replace("string/path.js", "var baseName", "var baseNameVar");
		replace("string/path.js", "baseName.", "baseNameVar.");
		replace("net/xmlhttp.js", "goog.net.XmlHttp.ASSUME_NATIVE_XHR =\n" +
				"    goog.define('goog.net.XmlHttp.ASSUME_NATIVE_XHR', false);", "");
		replace("net/xmlhttp.js", "goog.net.XmlHttp.ASSUME_NATIVE_XHR", "goog.net.XmlHttpDefines.ASSUME_NATIVE_XHR");
		replace("i18n/datetimesymbols.js", "/** @type {!goog.i18n.DateTimeSymbolsType} */\n" +
				"goog.i18n.DateTimeSymbols;\n" +
				"\n" +
				"\n" +
				"/**\n" +
				" * Selected date/time formatting symbols by locale.\n" +
				" */", "/**\n" +
				" * Selected date/time formatting symbols by locale.\n" +
				" * @type {!goog.i18n.DateTimeSymbolsType}\n" +
				" */");


		// Add an empty doc comment so that the commented out code is not attributed to the export statement
		replace("crypt/md5.js", "];\n */", "];\n */\n/***/");

		// Unused code
//		removeDeclaration("color/color.js", "goog.color.isNormalizedHexColor_");
//		removeDeclaration("color/color.js", "goog.color.normalizedHexColorRe_");
//		removeDeclaration("html/sanitizer/htmlsanitizer.js", "goog.html.sanitizer.HTML_SANITIZER_TEMPLATE_SUPPORTED");
		removeDeclaration("async/delay.js", "goog.Delay");
		replace("async/delay.js", "goog.provide('goog.Delay');\n", "");
	}

	private void replace(String file, String search, String replace) throws IOException {
		File fixedJsFile = new File(outputDir, "closure/goog/" + file);
		String content = FileUtils.getFileContentSafe(fixedJsFile);
		if (!content.contains(search)) {
			throw new IllegalStateException(search + " not contained in " + file);
		}
		content = content.replace(search, replace);
		FileUtils.writeFileContent(fixedJsFile, content);
	}

	private void replaceAll(String file, Pattern searchPattern, String replace) throws IOException {
		File fixedJsFile = new File(outputDir, "closure/goog/" + file);
		String content = FileUtils.getFileContentSafe(fixedJsFile);
		if (!searchPattern.matcher(content).find()) {
			throw new IllegalStateException(searchPattern.pattern() + " not contained in " + file);
		}
		content = searchPattern.matcher(content).replaceAll(replace);
		FileUtils.writeFileContent(fixedJsFile, content);
	}

	private void removeDeclaration(String file, String search) throws IOException {
		Pattern pattern = Pattern.compile(DOCUMENTED_PATTERN + Pattern.quote(search) +
				"(\\s*=\\s*)");
		File fixedJsFile = new File(outputDir, "closure/goog/" + file);
		String content = FileUtils.getFileContentSafe(fixedJsFile);
		Matcher matcher = pattern.matcher(content);
		if (!matcher.find()) {
			throw new IllegalStateException(search + " not contained in " + file);
		}
		String definition = JsCodeUtils.getDefinition(content, matcher, 3);
		String fullMatch = matcher.group();
		fullMatch = fullMatch.substring(0, fullMatch.length() - matcher.group(3).length()) + definition;
		content = content.replace(fullMatch, "");

		FileUtils.writeFileContent(fixedJsFile, content);
	}
}
