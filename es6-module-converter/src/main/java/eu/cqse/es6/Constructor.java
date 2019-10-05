package eu.cqse.es6;

import java.util.List;
import java.util.regex.Matcher;

public class Constructor {
	public final String fullMatch;
	public final String docComment;
	public final String classNamespace;
	public final String constructorDefinition;

	public Constructor(String fullMatch, String docComment, String classNamespace, String constructorDefinition) {
		this.fullMatch = fullMatch;
		this.docComment = docComment;
		this.classNamespace = classNamespace;
		this.constructorDefinition = constructorDefinition;
	}

	public String getEs6Constructor(List<ClassMember> classMembers) {
		String constructorComment = docComment
				.replace(" * @final\n", "")
				.replaceAll(" \\* @extends.*\n", "")
				.replace("* */", "*/");

		String constructorDefinition = this.constructorDefinition.replaceFirst("\\s?=\\s*function", "constructor");
		StringBuilder constructorExtensionBuilder = new StringBuilder();
		for (ClassMember classMember : classMembers) {
			constructorExtensionBuilder.append("\n").append(classMember.docComment);
			String declaration = classMember.declaration;
			if (classMember.isField()) {
				declaration = declaration.replaceFirst("\\s?=\\s*", Matcher.quoteReplacement("this." + classMember.memberName) + "$0");
			}
			constructorExtensionBuilder.append(declaration).append("\n\n");
		}
		constructorDefinition = constructorDefinition.replaceAll("};?$", Matcher.quoteReplacement(constructorExtensionBuilder + "}"));
		return constructorComment + constructorDefinition;
	}
}
