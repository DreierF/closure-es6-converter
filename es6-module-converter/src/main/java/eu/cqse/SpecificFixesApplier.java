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
        replaceAll("net/xhrio.js", Pattern.compile("var ResponseType = goog\\.net\\.XhrIo\\.ResponseType;\r?\n"), "");
        replaceAll("net/xhrio.js", Pattern.compile("ResponseType\\.[A-Z_]+"), "goog.net.XhrIo.$0");
        replaceAll("net/xhrio.js", Pattern.compile("var XhrIo = goog.net.XhrIo;(\r)?\n"), "");
        replace("net/xhrio.js", "XhrIo.base", "goog.net.XhrIo.base");
        replace("net/xhrio.js", "});  // goog.scope", "");
        replaceAll("events/events.js", Pattern.compile("\\*/\r?\ngoog.events.unlistenByKey"),
                " * @suppress {checkTypes}\r\n */\r\ngoog.events.unlistenByKey");
        replace("iter/iter.js", "var product", "var productVar");
        replace("iter/iter.js", "product,", "productVar,");
        replace("string/path.js", "var baseName", "var baseNameVar");
        replace("string/path.js", "baseName.", "baseNameVar.");
        replaceAll("net/xmlhttp.js", Pattern.compile("goog\\.net\\.XmlHttp\\.ASSUME_NATIVE_XHR =\r?\n" +
                " {4}goog\\.define\\('goog\\.net\\.XmlHttp\\.ASSUME_NATIVE_XHR', false\\);"), "");
        replace("net/xmlhttp.js", "goog.net.XmlHttp.ASSUME_NATIVE_XHR", "goog.net.XmlHttpDefines.ASSUME_NATIVE_XHR");
        replaceAll("i18n/datetimesymbols.js", Pattern.compile("/\\*\\* @type \\{!goog\\.i18n\\.DateTimeSymbolsType\\} \\*/\r?\n" +
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
        replaceAll("crypt/md5.js", Pattern.compile("];\r?\n \\*/"), "];\r\n */\r\n/***/");

        // Unused code
//		removeDeclaration("color/color.js", "goog.color.isNormalizedHexColor_");
//		removeDeclaration("color/color.js", "goog.color.normalizedHexColorRe_");
//		removeDeclaration("html/sanitizer/htmlsanitizer.js", "goog.html.sanitizer.HTML_SANITIZER_TEMPLATE_SUPPORTED");
        removeDeclaration("async/delay.js", "goog.Delay");
        replaceAll("async/delay.js", Pattern.compile("goog\\.provide\\('goog\\.Delay'\\);\r?\n"), "");
        replaceAll("events/eventhandler.js", Pattern.compile("@template\\s+EVENTOBJ,\\s*THIS"), "@template EVENTOBJ, THIS\r\n \\* @suppress{checkTypes}");
        replaceAll("events/events.js", Pattern.compile("@template T,EVENTOBJ"), "@template T,EVENTOBJ\r\n \\* @suppress{checkTypes}");
        replace("events/events.js", "return listener;", "return /** @type {!Function} */ (listener);");
        replace("debug/logger.js", "msg = msg();", "msg = /** @type{!Function} */ (msg);\r\n\tmsg = msg();");
        replace("uri/uri.js", "this.queryData_.setValues(key, values);", "this.queryData_.setValues(key, /** @type {!Array<?>} */ (values));");
        replaceAll("ui/menuitem.js", Pattern.compile("@return \\{\\?string} The keyboard accelerator text, or null if the menu item\r?\n\\s*\\*\\s*doesn't have one\\."),  //
                "@suppress {checkTypes}\r\n \\* @return {?string} The keyboard accelerator text, or null if the menu item doesn't have one.");
        addSuppressCommmentToCommentBlock("dom/classes.js", "@deprecated Use goog.dom.classlist.addRemove instead.");
        addSuppressCommmentToCommentBlock("a11y/aria/aria.js", "Sets the state or property of an element.");
        addSuppressCommmentToCommentBlock("html/safehtml.js", "Gets value allowed in \"style\" attribute");
        addSuppressCommmentToCommentBlock("html/safehtml.js", "Creates a new SafeHtml object by joining the parts with separator.");
        addSuppressCommmentToCommentBlock("html/safestyle.js", "@throws {Error} If invalid name is provided.");
        addSuppressCommmentToCommentBlock("html/safestyle.js", "Creates a new SafeStyle object by concatenating the values.");
        addSuppressCommmentToCommentBlock("html/safestylesheet.js", "Creates a new SafeStyleSheet object by concatenating values.");
        addSuppressCommmentToCommentBlock("events/events.js", "@return {?boolean} indicating whether the listener was there to remove.");
        addSuppressCommmentToCommentBlock("json/json.js", "Serializes a generic value to a JSON string");
        addSuppressCommmentToCommentBlock("promise/promise.js", "with the return value or rejected with the thrown value of the callback.");
        addSuppressCommmentToCommentBlock("ui/ac/renderer.js", "@return {string} The regex-ready token.");
        addSuppressCommmentToCommentBlock("ui/controlrenderer.js", "must override this method accordingly.");
        addSuppressCommmentToCommentBlock("ui/control.js", "@return {string} Text caption of the control or empty string if none.");
        addSuppressCommmentToCommentBlock("ui/menuitem.js", "Returns the text caption of the component while ignoring accelerators.");
        addSuppressCommmentToCommentBlock("ui/zippy.js", "@type {?function():Element}");
        addSuppressCommmentToCommentBlock("timer/timer.js", "@template SCOPE");
    }

    private void addSuppressCommmentToCommentBlock(String file, String existingTextInComment) throws IOException {
        replace(file, existingTextInComment, existingTextInComment + "\r\n * @suppress{checkTypes}");
    }

    private void replace(String file, String search, String replace) throws IOException {
        File fixedJsFile = new File(outputDir, file);
        String content = FileUtils.getFileContentSafe(fixedJsFile);
        if (!content.contains(search)) {
            throw new IllegalStateException(search + " not contained in " + file);
        }
        content = content.replace(search, replace);
        FileUtils.writeFileContent(fixedJsFile, content);
    }

    private void replaceAll(String file, Pattern searchPattern, String replace) throws IOException {
        File fixedJsFile = new File(outputDir, file);
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
        File fixedJsFile = new File(outputDir, file);
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
