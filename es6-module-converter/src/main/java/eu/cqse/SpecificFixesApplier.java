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
//		adjustIn("db/error", "goog.provide('goog.db.DomErrorLike')", "goog.provide('goog.db.DOMErrorLike')");
		adjustIn("events/browserfeature", "goog.scope(function() {", "");
		adjustIn("events/browserfeature", "});  // goog.scope", "");
		adjustIn("net/xhrio", "goog.scope(function() {", "");
		adjustIn("net/xhrio", Pattern.compile("var ResponseType = goog\\.net\\.XhrIo\\.ResponseType;\r?\n"), "");
		adjustIn("net/xhrio", Pattern.compile("ResponseType\\.[A-Z_]+"), "goog.net.XhrIo.$0");
		adjustIn("net/xhrio", Pattern.compile("var XhrIo = goog.net.XhrIo;(\r)?\n"), "");
		adjustIn("net/xhrio", "XhrIo.base", "goog.net.XhrIo.base");
		adjustIn("net/xhrio", "});  // goog.scope", "");
		adjustIn("events/events", Pattern.compile("\\*/\r?\ngoog.events.unlistenByKey"),
				" * @suppress {checkTypes}\r\n */\r\ngoog.events.unlistenByKey");
		adjustIn("iter/iter", "var product", "var productVar");
		adjustIn("iter/iter", "product,", "productVar,");
		adjustIn("string/path", "var baseName", "var baseNameVar");
		adjustIn("string/path", "baseName.", "baseNameVar.");
		adjustIn("net/xmlhttp", Pattern.compile("goog\\.net\\.XmlHttp\\.ASSUME_NATIVE_XHR =\r?\n" +
				" {4}goog\\.define\\('goog\\.net\\.XmlHttp\\.ASSUME_NATIVE_XHR', false\\);"), "");
		adjustIn("net/xmlhttp", "goog.net.XmlHttp.ASSUME_NATIVE_XHR", "goog.net.XmlHttpDefines.ASSUME_NATIVE_XHR");
		adjustIn("i18n/datetimesymbols", Pattern.compile("/\\*\\* @type \\{!goog\\.i18n\\.DateTimeSymbolsType\\} \\*/\r?\n" +
				"goog\\.i18n\\.DateTimeSymbols;\r?\n" +
				"\r?\n" +
				"\r?\n" +
				"/\\*\\*\r?\n" +
				" \\* Selected date/time formatting symbols by locale\\.\r?\n" +
				" \\*/"), "/**\r\n" +
				" * Selected date/time formatting symbols by locale.\r\n" +
				" * @type {!goog.i18n.DateTimeSymbolsType}\r\n" +
				" */");


		// Add an empty doc comment so that the commented out code is not attributed to the export statement
		adjustIn("crypt/md5", Pattern.compile("];\r?\n \\*/"), "];\r\n */\r\n/***/");

		// Unused code
//		removeDeclaration("color/color", "goog.color.isNormalizedHexColor_");
//		removeDeclaration("color/color", "goog.color.normalizedHexColorRe_");
//		removeDeclaration("html/sanitizer/htmlsanitizer", "goog.html.sanitizer.HTML_SANITIZER_TEMPLATE_SUPPORTED");
		removeDeclaration("async/delay", "goog.Delay");
		adjustIn("async/delay", Pattern.compile("goog\\.provide\\('goog\\.Delay'\\);\r?\n"), "");
		adjustIn("events/eventhandler", Pattern.compile("@template\\s+EVENTOBJ,\\s*THIS"), "@template EVENTOBJ, THIS\r\n \\* @suppress{checkTypes}");
		adjustIn("events/events", Pattern.compile("@template T,EVENTOBJ"), "@template T,EVENTOBJ\r\n \\* @suppress{checkTypes}");
		adjustIn("events/events", "return listener;", "return /** @type {!Function} */ (listener);");
		adjustIn("debug/logger", "msg = msg();", "msg = /** @type{!Function} */ (msg);\r\n\tmsg = msg();");
		adjustIn("uri/uri", "this.queryData_.setValues(key, values);", "this.queryData_.setValues(key, /** @type {!Array<?>} */ (values));");
		adjustIn("ui/menuitem", Pattern.compile("@return \\{\\?string} The keyboard accelerator text, or null if the menu item\r?\n\\s*\\*\\s*doesn't have one\\."),  //
				"@suppress {checkTypes}\r\n \\* @return {?string} The keyboard accelerator text, or null if the menu item doesn't have one.");
		addSuppressCommmentToCommentBlock("dom/classes", "@deprecated Use goog.dom.classlist.addRemove instead.");
		addSuppressCommmentToCommentBlock("a11y/aria/aria", "Sets the state or property of an element.");
		addSuppressCommmentToCommentBlock("html/safehtml", "Gets value allowed in \"style\" attribute");
		addSuppressCommmentToCommentBlock("html/safehtml", "Creates a new SafeHtml object by joining the parts with separator.");
		addSuppressCommmentToCommentBlock("html/safestyle", "@throws {Error} If invalid name is provided.");
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
