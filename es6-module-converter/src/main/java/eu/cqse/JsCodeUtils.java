package eu.cqse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsCodeUtils {
	public static String safeReplaceString(String methodOrConstantName) {
		return Matcher.quoteReplacement(methodOrConstantName);
	}

	public static String multilineSafeNamespacePattern(String namespace) {
		return Pattern.quote(namespace).replace(".", "\\E\\s*\\.\\s*\\Q");
	}

	public static String indentCode(String declaration) {
		return declaration.replaceAll("(?m)^", "  ");
	}
}
