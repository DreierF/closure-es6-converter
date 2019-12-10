package eu.cqse.es6;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import eu.cqse.FileUtils;
import eu.cqse.JsCodeUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Es6ClassConversionPass {

	private static final Pattern GOOG_INHERITS_PATTERN = Pattern.compile("(?m)^goog\\.inherits\\(\\s*([^,]+),\\s*([^)]+)\\);");
	private static final Pattern CONSTRUCTOR_PATTERN = Pattern.compile("(?m)^(/\\*\\*((?!\\*/|@(?:constructor|interface)).)*@(?:constructor|interface)((?!\\*/).)*\\*/\\s*)((?:const|var|let)\\s+)?([\\w.]+)(\\s?=\\s*function)", Pattern.DOTALL);
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
			FileUtils.writeFileContent(file, content.replaceAll("\n{3,}", "\n\n"));
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
			String definition = JsCodeUtils.getDefinition(content, matcher, 5);
			String fullMatch = matcher.group();
			fullMatch = fullMatch.substring(0, fullMatch.length() - matcher.group(5).length()) + definition;
			ClassMember classMember = new ClassMember(fullMatch, matcher.group(1), matcher.group(3), matcher.group(4), definition);
			classMembers.put(classMember.classNamespace, classMember);
		}
		return classMembers;
	}

	private List<Constructor> getConstructors(String content) {
		List<Constructor> constructors = new ArrayList<>();
		Matcher matcher = CONSTRUCTOR_PATTERN.matcher(content);
		while (matcher.find()) {
			String definition = JsCodeUtils.getDefinition(content, matcher, 6);
			String fullMatch = matcher.group();
			fullMatch = fullMatch.substring(0, fullMatch.length() - matcher.group(6).length()) + definition;
			constructors.add(new Constructor(fullMatch, matcher.group(1), matcher.group(5), definition, matcher.group(4)));
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
