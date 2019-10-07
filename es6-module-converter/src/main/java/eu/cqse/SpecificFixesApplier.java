package eu.cqse;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class SpecificFixesApplier {
	private final File outputDir;

	public SpecificFixesApplier(File outputDir) {
		this.outputDir = outputDir;
	}

	public void process() throws IOException {
//		replace("db/error.js", "goog.provide('goog.db.DomErrorLike')", "goog.provide('goog.db.DOMErrorLike')");
		replace("events/browserfeature.js", "goog.scope(function() {", "");
		replace("events/browserfeature.js", "});  // goog.scope", "");
		replace("net/xhrio.js", "goog.scope(function() {", "");
		replace("net/xhrio.js", "var ResponseType = goog.net.XhrIo.ResponseType;\n", "");
		replaceAll("net/xhrio.js", "ResponseType\\.[A-Z_]+", "goog.net.XhrIo.$0");
		replace("net/xhrio.js", "var XhrIo = goog.net.XhrIo;\n", "");
		replace("net/xhrio.js", "XhrIo.base", "goog.net.XhrIo.base");
		replace("net/xhrio.js", "});  // goog.scope", "");
		replace("events/events.js", "*/\ngoog.events.unlistenByKey",
				" * @suppress {checkTypes}\n */\ngoog.events.unlistenByKey");
		replace("iter/iter.js", "var product", "var productVar");
		replace("iter/iter.js", "product,", "productVar,");
		replace("string/path.js", "var baseName", "var baseNameVar");
		replace("string/path.js", "baseName.", "baseNameVar.");
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

	private void replaceAll(String file, String search, String replace) throws IOException {
		File fixedJsFile = new File(outputDir, "closure/goog/" + file);
		String content = FileUtils.getFileContentSafe(fixedJsFile);
		if (!Pattern.compile(search).matcher(content).find()) {
			throw new IllegalStateException(search + " not contained in " + file);
		}
		content = content.replaceAll(search, replace);
		FileUtils.writeFileContent(fixedJsFile, content);
	}
}
