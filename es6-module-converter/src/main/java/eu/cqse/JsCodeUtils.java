package eu.cqse;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsCodeUtils {
	private static final ImmutableList<Character> OPENING = ImmutableList.of('(', '[', '{');
	private static final ImmutableList<Character> CLOSING = ImmutableList.of(')', ']', '}');

	static final String IDENTIFIER_PATTERN = "\\w_$";

	public static String safeReplaceString(String methodOrConstantName) {
		return Matcher.quoteReplacement(methodOrConstantName);
	}

	public static String multilineSafeNamespacePattern(String namespace) {
		return Pattern.quote(namespace).replace(".", "\\E\\s*\\.\\s*\\Q");
	}

	public static String indentCode(String declaration) {
		return declaration.replaceAll("(?m)^", "  ");
	}

	public static String getDefinition(String content, Matcher matcher, int definitionStartGroup) {
		String definition = matcher.group(definitionStartGroup);
		if (definition.equals(";")) {
			return ";";
		}
		return content.substring(matcher.start(definitionStartGroup), getDefinitionEnd(content, matcher.end()));
	}

	public static String getInferredParameterList(String docComment) {
		Matcher matcher = Pattern.compile("\\* @param\\s?\\{[^{}]+(}|\\{[^{}]+}) (\\w+)").matcher(docComment);
		List<String> parameterList = new ArrayList<>();
		while (matcher.find()) {
			parameterList.add(matcher.group(2));
		}
		return StringUtils.concat(parameterList, ", ");
	}

	private static int getDefinitionEnd(String content, int matcherEnd) {
		int end = matcherEnd;
		int nesting = 0;
		EScannerState state = EScannerState.TOP_LEVEL;
		while (true) {
			char currentChar = content.charAt(end);
			switch (state) {
				case IN_SINGLE_LINE_COMMENT:
					if (currentChar == '\n') {
						state = EScannerState.TOP_LEVEL;
					}
					break;
				case IN_REGEX:
					if (currentChar == '\\') {
						end++;
					} else if (currentChar == '[') {
						state = EScannerState.IN_REGEX_CHARACTER_GROUP;
					} else if (currentChar == '/') {
						state = EScannerState.TOP_LEVEL;
					}
					break;
				case IN_REGEX_CHARACTER_GROUP:
					if (currentChar == '\\') {
						end++;
					} else if (currentChar == ']') {
						state = EScannerState.IN_REGEX;
					}
					break;
				case IN_BLOCK_COMMENT:
					if (currentChar == '*' && content.charAt(end + 1) == '/') {
						end++;
						state = EScannerState.TOP_LEVEL;
					}
					break;
				case IN_SINGLE_QUOTED_STRING:
					if (currentChar == '\\') {
						end++;
					} else if (currentChar == '\'') {
						state = EScannerState.TOP_LEVEL;
					}
					break;
				case IN_DOUBLE_QUOTED_STRING:
					if (currentChar == '\\') {
						end++;
					} else if (currentChar == '"') {
						state = EScannerState.TOP_LEVEL;
					}
					break;
				case TOP_LEVEL:
					if (nesting == 0 && (currentChar == ';' ||
							(currentChar == '}' && content.charAt(end + 1) == '\n' && content.charAt(end + 2) == '\n'))) {
						return end + 1;
					}
					if (currentChar == '/') {
						if (content.charAt(end + 1) == '/') {
							state = EScannerState.IN_SINGLE_LINE_COMMENT;
						} else if (content.charAt(end + 1) == '*') {
							state = EScannerState.IN_BLOCK_COMMENT;
						} else if (isRegexStart(content, end)) {
							state = EScannerState.IN_REGEX;
						}
					} else if (currentChar == '\'') {
						state = EScannerState.IN_SINGLE_QUOTED_STRING;
					} else if (currentChar == '"') {
						state = EScannerState.IN_DOUBLE_QUOTED_STRING;
					}
					if (OPENING.contains(currentChar)) {
						nesting++;
					} else if (CLOSING.contains(currentChar)) {
						nesting--;
					}
					break;
			}
			end++;
			if (end >= content.length()) {
				throw new IllegalArgumentException("Did not find definition end in: " + content.substring(matcherEnd - 30));
			}
		}
	}

	private static boolean isRegexStart(String content, int end) {
		if(content.charAt(end+1) == '=') { // /=
			return false;
		}
		String startContent = content.substring(end+1);
		if (Pattern.compile("\\s+\\(?\\d.*").matcher(startContent).lookingAt()) {
			return false;
		} else if (Pattern.compile("\\s+\\(?\\w+.*").matcher(startContent).lookingAt()) {
			return false;
		}
		return !startContent.startsWith(" this.");
	}

	private enum EScannerState {
		TOP_LEVEL, IN_REGEX, IN_REGEX_CHARACTER_GROUP, IN_BLOCK_COMMENT, IN_SINGLE_QUOTED_STRING, IN_DOUBLE_QUOTED_STRING, IN_SINGLE_LINE_COMMENT
	}
}
