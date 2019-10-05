package eu.cqse.es6;

import java.util.List;

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
		String constructorComment = docComment.replace(" * @final\n", "").replaceAll(" * @extends.*\n", "");

		String constructorDefinition = this.constructorDefinition.replaceFirst("\\s?=\\s*function", "constructor");
		StringBuilder constructorExtensionBuilder = new StringBuilder();
		for (ClassMember classMember : classMembers) {
			constructorExtensionBuilder.append(classMember.docComment);
			String declaration = classMember.declaration;
			if (classMember.isField()) {
				declaration = declaration.replaceFirst("\\s?=\\s*", "this." + classMember.memberName + "$0");
			}
			constructorExtensionBuilder.append(declaration);
		}
		constructorDefinition = constructorDefinition.replaceAll("};?$", constructorExtensionBuilder + "}");
		return constructorComment + "\n" + constructorDefinition;
	}
}
