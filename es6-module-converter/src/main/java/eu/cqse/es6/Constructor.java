package eu.cqse.es6;

import java.util.List;
import java.util.regex.Matcher;

import static eu.cqse.JsCodeUtils.indentCode;
import static eu.cqse.JsCodeUtils.multilineSafeNamespacePattern;

public class Constructor extends ClassMember {

	public final String constLetVar;

	public Constructor(String fullMatch, String docComment, String classNamespace, String declaration, String constLetVar) {
		super(fullMatch, docComment, classNamespace, "constructor", declaration);
		this.constLetVar = constLetVar;
	}

	@Override
	public String getDocComment() {
		return docComment
				.replaceAll("( \\*)? @final\n?", "")
				.replaceAll("( \\*)? @constructor\n?", "")
				.replaceAll(" \\* @extends.*\n", "")
				.replaceAll(" \\* @implements.*\n", "")
				.replaceAll(" \\* @interface\n", "")
				.replaceAll("( \\*)? @abstract\n", "")
				.replace("* */", "*/");
	}

	@Override
	public String getDeclaration(GoogInheritsInfo googInheritsInfo) {
		String declaration = super.getDeclaration(googInheritsInfo);
		if (googInheritsInfo != null) {
			declaration = declaration
					.replaceAll(multilineSafeNamespacePattern(googInheritsInfo.extendedFullNamespace + ".call") + "\\s*\\(\\s*this,?\\s*", "super(");
			if (!declaration.contains("super(")) {
				declaration = declaration.replaceAll("constructor\\([^)]+\\)\\s*\\{\\s*", "$0super();\n\n  ");
			}
		}
		return declaration;
	}

	public String getEs6Representation(List<ClassMember> classMembers, GoogInheritsInfo googInheritsInfo) {
		String constructorDefinition = this.getAsEs6Method(googInheritsInfo);

		StringBuilder constructorExtensionBuilder = new StringBuilder();
		for (ClassMember classMember : classMembers) {
			if (classMember.isField()) {
				constructorExtensionBuilder.append("\n");
				constructorExtensionBuilder.append(indentCode(classMember.getAsEs6Field()));
				constructorExtensionBuilder.append("\n\n");
			}
		}
		constructorDefinition = constructorDefinition.replaceAll("};?$", Matcher.quoteReplacement(constructorExtensionBuilder + "}"));
		return constructorDefinition;
	}
}
