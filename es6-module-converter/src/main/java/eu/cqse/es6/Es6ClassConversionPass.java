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
	private static final Pattern CLASS_MEMBER_PATTERN = Pattern.compile("(?m)^(/\\*\\*(.(?!\\*/))*\\*/\\s*)([\\w.]+)\\.prototype\\.(\\w+)" +
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
			String definition = getDefinition(content, matcher, 6);
			ClassMember classMember = new ClassMember(matcher.group(), matcher.group(1), matcher.group(4), matcher.group(5), definition);
			classMembers.put(classMember.classNamespace, classMember);
		}
		return classMembers;
	}

	private String getDefinition(String content, Matcher matcher, int definitionStartGroup) {
		String definition = matcher.group(definitionStartGroup);
		if (!definition.equals(";")) {
			definition = content.substring(matcher.start(definitionStartGroup), getDefinitionEnd(content, matcher.end()));
		}
		return definition;
	}

	private int getDefinitionEnd(String content, int matcherEnd) {
		int end = matcherEnd;
		int nesting = 0;
		while (content.charAt(end) != ';' || nesting > 0) {
			char currentChar = content.charAt(end);
			if (currentChar == '}' && content.charAt(end + 1) == '\n' && content.charAt(end + 2) == '\n' && nesting == 0) {
				return end + 1;
			}
			if (OPENING.contains(currentChar)) {
				nesting++;
			} else if (CLOSING.contains(currentChar)) {
				nesting--;
			}
			end++;
		}
		return end + 1;
	}

	private List<Constructor> getConstructors(String content) {
		List<Constructor> constructors = new ArrayList<>();
		Matcher matcher = CONSTRUCTOR_PATTERN.matcher(content);
		while (matcher.find()) {
			String definition = getDefinition(content, matcher, 5);
			constructors.add(new Constructor(matcher.group(), matcher.group(1), matcher.group(4), definition));
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
}
