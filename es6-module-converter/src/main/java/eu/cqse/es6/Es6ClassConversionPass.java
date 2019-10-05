package eu.cqse.es6;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import eu.cqse.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Es6ClassConversionPass {

	private static final ImmutableList<Character> OPENING = ImmutableList.of('(', '[', '{');
	private static final ImmutableList<Character> CLOSING = ImmutableList.of(')', ']', '}');
	private static final Pattern GOOG_INHERITS_PATTERN = Pattern.compile("(?m)^goog\\.inherits\\(\\s*([^,]+),\\s*([^)]+)\\);");
	private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("(?m)^(/\\*\\*((?!\\*/|@constructor).)*@constructor((?!\\*/).)*\\*/\\s*)([\\w.]+)(\\s?=\\s*function)", Pattern.DOTALL);
	private static final Pattern CLASS_MEMBER_PATTERN = Pattern.compile("(?m)^(/\\*\\*((?!\\*/).)*\\*/\\s*)([\\w.]+)\\.prototype\\.(\\w+)" +
			"(;|\\s?=\\s*)", Pattern.DOTALL);

	public void process(File inputDir) throws IOException {
		FileUtils.processRelevantJsFiles(this::processJsFile, inputDir);
	}

	private void processJsFile(File file) {
		String content = FileUtils.getFileContentSafe(file);

		Map<String, GoogInheritsInfo> inherits = getInherits(content);
		List<Constructor> constructors = getConstructors(content);
		ListMultimap<String, ClassMember> classMembers = getClassMembers(content);

		List<ClassContainer> classContainers = groupByClass(inherits, constructors, classMembers);

		for (ClassContainer classContainer : classContainers) {
			String es6Class = classContainer.buildEs6Class();
			if (classContainer.googInheritsInfo != null) {
				content = content.replace(classContainer.googInheritsInfo.fullMatch, "");
			}
			for (ClassMember classMember : classContainer.classMembers) {
				content = content.replace(classMember.fullMatch, "");
			}
			content = content.replace(classContainer.constructor.fullMatch, es6Class);
		}

		try {
			FileUtils.writeFileContent(file, content);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<ClassContainer> groupByClass(Map<String, GoogInheritsInfo> inherits, List<Constructor> constructors, ListMultimap<String, ClassMember> classMembers) {
		List<ClassContainer> classContainers = new ArrayList<>();
		for (Constructor constructor : constructors) {
			ClassContainer classContainer = new ClassContainer(constructor);
			classContainer.setInherits(inherits.get(constructor.classNamespace));
			if (classMembers.containsKey(constructor.classNamespace)) {
				classContainer.setClassMembers(classMembers.get(constructor.classNamespace));
			}
			classContainers.add(classContainer);
		}
		return classContainers;
	}

	private ListMultimap<String, ClassMember> getClassMembers(String content) {
		ListMultimap<String, ClassMember> classMembers = ArrayListMultimap.create();
		Matcher matcher = CLASS_MEMBER_PATTERN.matcher(content);
		while (matcher.find()) {
			String definition = getDefinition(content, matcher, 5);
			String fullMatch = matcher.group();
			fullMatch = fullMatch.substring(0, fullMatch.length() - matcher.group(5).length()) + definition;
			ClassMember classMember = new ClassMember(fullMatch, matcher.group(1), matcher.group(3), matcher.group(4), definition);
			classMembers.put(classMember.classNamespace, classMember);
		}
		return classMembers;
	}

	private String getDefinition(String content, Matcher matcher, int definitionStartGroup) {
		String definition = matcher.group(definitionStartGroup);
		if (definition.equals(";")) {
			return ";";
		}
		return content.substring(matcher.start(definitionStartGroup), getDefinitionEnd(content, matcher.end()));
	}

	private int getDefinitionEnd(String content, int matcherEnd) {
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
					} else if (currentChar == '/') {
						state = EScannerState.TOP_LEVEL;
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
						} else if (content.charAt(end + 1) == '[') {
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
				throw new IllegalArgumentException("Did not find definition end in: " + content.substring(matcherEnd - 10));
			}
		}
	}

	private List<Constructor> getConstructors(String content) {
		List<Constructor> constructors = new ArrayList<>();
		Matcher matcher = CONSTRUCTOR_PATTERN.matcher(content);
		while (matcher.find()) {
			String definition = getDefinition(content, matcher, 5);
			String fullMatch = matcher.group();
			fullMatch = fullMatch.substring(0, fullMatch.length() - matcher.group(5).length()) + definition;
			constructors.add(new Constructor(fullMatch, matcher.group(1), matcher.group(4), definition));
		}
		return constructors;
	}

	private Map<String, GoogInheritsInfo> getInherits(String content) {
		Map<String, GoogInheritsInfo> inheritsInfos = new HashMap<>();
		Matcher matcher = GOOG_INHERITS_PATTERN.matcher(content);
		while (matcher.find()) {
			GoogInheritsInfo googInheritsInfo = new GoogInheritsInfo(matcher.group(), matcher.group(1), matcher.group(2));
			inheritsInfos.put(googInheritsInfo.fullClassNamespace, googInheritsInfo);
		}
		return inheritsInfos;
	}

	private enum EScannerState {
		TOP_LEVEL, IN_REGEX, IN_BLOCK_COMMENT, IN_SINGLE_QUOTED_STRING, IN_DOUBLE_QUOTED_STRING, IN_SINGLE_LINE_COMMENT
	}
}
