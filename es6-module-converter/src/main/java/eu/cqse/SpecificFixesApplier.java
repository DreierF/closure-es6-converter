package eu.cqse;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecificFixesApplier extends FixerBase {

	public SpecificFixesApplier(Path folder) {
		super(folder, "js");
	}

	private static final String DOCUMENTED_PATTERN = "(?ms)^(/\\*\\*((?!\\*/).)*\\*/\\s*)";

	public void fix() {
		adjustIn("i18n/localefeature", Pattern.compile("(?ms)(?<!^)exports\\."), "");
		adjustIn("debug/tracer", Pattern.compile("(?ms)^goog\\.debug\\.Trace_ ="), "let Trace_ =");
		adjustIn("debug/tracer", "goog.debug.Trace_", "Trace_");
		adjustIn("promise/thenable", "goog.requireType('goog.Promise');", "");
		adjustIn("net/xhrio", "goog.scope(function() {", "");
		adjustIn("net/xhrio", Pattern.compile("var ResponseType = goog\\.net\\.XhrIo\\.ResponseType;\r?\n"), "");
		adjustIn("net/xhrio", Pattern.compile("ResponseType\\.[A-Z_]+"), "goog.net.XhrIo.$0");
		adjustIn("net/xhrio", Pattern.compile("var XhrIo = goog\\.net\\.XhrIo;(\r)?\n"), "");
		adjustIn("net/xhrio", "XhrIo.base", "goog.net.XhrIo.base");
		adjustIn("net/xhrio", "});  // goog.scope", "");
		adjustIn("events/events", Pattern.compile("\\*/\r?\ngoog\\.events\\.unlistenByKey"),
				" * @suppress {checkTypes}\r\n */\r\ngoog.events.unlistenByKey");
		adjustIn("iter/iter", "const product", "var productVar");
		adjustIn("iter/iter", "product,", "productVar,");
		adjustIn("string/path", "const baseName", "const baseNameVar");
		adjustIn("string/path", "baseName.", "baseNameVar.");
		adjustIn("net/xmlhttp", Pattern.compile("goog\\.net\\.XmlHttp\\.ASSUME_NATIVE_XHR =\r?\n" +
				" {4}goog\\.define\\('goog\\.net\\.XmlHttp\\.ASSUME_NATIVE_XHR', false\\);"), "");
		adjustIn("net/xmlhttp", "goog.net.XmlHttp.ASSUME_NATIVE_XHR", "goog.net.XmlHttpDefines.ASSUME_NATIVE_XHR");
		adjustIn("i18n/datetimesymbols", Pattern.compile("/\\*\\* @type \\{!goog\\.i18n\\.DateTimeSymbolsType} \\*/\r?\n" +
				"goog\\.i18n\\.DateTimeSymbols;\r?\n" +
				"\r?\n" +
				"\r?\n" +
				"/\\*\\*\r?\n" +
				" \\* Selected date/time formatting symbols by locale\\.\r?\n" +
				" \\*/"), "/**\r\n" +
				" * Selected date/time formatting symbols by locale.\r\n" +
				" * @type {!goog.i18n.DateTimeSymbolsType}\r\n" +
				" */");

		adjustInAll(Pattern.compile("(goog\\.inherits[^;]+;)[\r\n]goog\\.addSingletonGetter\\(([^)]+)\\);"), "$1\n" +
				"/** @type {?$2} @suppress {underscore,checkTypes} @override */\n" +
				"$2.instance_ = undefined;\n" +
				"/** @override @return {!$2} @suppress {checkTypes} */\n" +
				"$2.getInstance = function() {\n" +
				"  if ($2.instance_) {\n" +
				"    return /** @type {!$2} */ ($2.instance_);\n" +
				"  }\n" +
				"  return /** @type {!$2} */ ($2.instance_) = new $2();\n" +
				"};");

		adjustInAll(Pattern.compile("\\s+'use strict';"), "");
		adjustInAll(Pattern.compile("(\\{[?!]?)goog\\.global\\.Intl"), "$1Intl");

		adjustInAll(Pattern.compile("goog\\.addSingletonGetter\\(([^)]+)\\);"), "/** @type {undefined|!$1} @suppress {underscore,checkTypes}*/\n" +
				"$1.instance_ = undefined;\n" +
				"/** @return {!$1} @suppress {checkTypes} */\n" +
				"$1.getInstance = function() {\n" +
				"  if ($1.instance_) {\n" +
				"    return /** @type {!$1} */ ($1.instance_);\n" +
				"  }\n" +
				"  return /** @type {!$1} */ ($1.instance_) = new $1();\n" +
				"};");

		// Add an empty doc comment so that the commented out code is not attributed to the export statement
		adjustIn("crypt/md5", Pattern.compile("];\r?\n \\*/"), "];\r\n */\r\n/***/");

		// Unused code
		adjustIn("events/eventhandler", Pattern.compile("@template\\s+EVENTOBJ,\\s*THIS"), "@template EVENTOBJ, THIS\r\n \\* @suppress{checkTypes}");
		adjustIn("events/events", Pattern.compile("@template T,EVENTOBJ"), "@template T,EVENTOBJ\r\n \\* @suppress{checkTypes}");
		adjustIn("events/events", "return listener;", "return /** @type {!Function} */ (listener);");
		adjustIn("uri/uri", "this.queryData_.setValues(key, values);", "this.queryData_.setValues(key, /** @type {!Array<?>} */ (values));");
		adjustIn("ui/menuitem", Pattern.compile("@return \\{\\?string} The keyboard accelerator text, or null if the menu item\r?\n\\s*\\*\\s*doesn't have one\\."),  //
				"@suppress {checkTypes}\r\n \\* @return {?string} The keyboard accelerator text, or null if the menu item doesn't have one.");

		addSuppressCommmentToCommentBlock("dom/classes", "@deprecated Use goog.dom.classlist.addRemove instead.");
		addSuppressCommmentToCommentBlock("a11y/aria/aria", "Sets the state or property of an element.");
		addSuppressCommmentToCommentBlock("html/safehtml", "Gets value allowed in \"style\" attribute");
		addSuppressCommmentToCommentBlock("html/safehtml", "Creates a new SafeHtml object by joining the parts with separator.");
		addSuppressCommmentToCommentBlock("html/safestyle", "@throws {!Error} If invalid name is provided.");
		addSuppressCommmentToCommentBlock("html/safestyle", "Creates a new SafeStyle object by concatenating the values.");
		addSuppressCommmentToCommentBlock("html/safestylesheet", "Creates a new SafeStyleSheet object by concatenating values.");
		addSuppressCommmentToCommentBlock("events/events", "@return {?boolean} indicating whether the listener was there to remove.");
		addSuppressCommmentToCommentBlock("json/json", "Serializes a generic value to a JSON string");
		addSuppressCommmentToCommentBlock("promise/promise", "with the return value or rejected with the thrown value of the callback.");
		addSuppressCommmentToCommentBlock("ui/ac/renderer", "@return {string} The regex-ready token.");
		addSuppressCommmentToCommentBlock("ui/controlrenderer", "must override this method accordingly.");
		addSuppressCommmentToCommentBlock("ui/control", "@return {string} Text caption of the control or empty string if none.");
		addSuppressCommmentToCommentBlock("ui/menuitem", "Returns the text caption of the component while ignoring accelerators.");
		addSuppressCommmentToCommentBlock("ui/zippy", "@type {?function():Element}");
		addSuppressCommmentToCommentBlock("timer/timer", "@template SCOPE");
	}

	private void addSuppressCommmentToCommentBlock(String file, String existingTextInComment) {
		adjustIn(file, existingTextInComment, existingTextInComment + "\r\n * @suppress{checkTypes}");
	}


	private void removeDeclaration(String file, String search) {
		if (!filePath.endsWith(file + "." + extension)) {
			return;
		}
		Pattern pattern = Pattern.compile(DOCUMENTED_PATTERN + Pattern.quote(search) +
				"(\\s*=\\s*)");
		Matcher matcher = pattern.matcher(fileContentSafe);
		if (!matcher.find()) {
			throw new IllegalStateException(search + " not contained in " + file);
		}
		String definition = JsCodeUtils.getDefinition(fileContentSafe, matcher, 3);
		String fullMatch = matcher.group();
		fullMatch = fullMatch.substring(0, fullMatch.length() - matcher.group(3).length()) + definition;
		fileContentSafe = fileContentSafe.replace(fullMatch, "");
	}
}
