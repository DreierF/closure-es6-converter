package eu.cqse.es6;

import java.util.List;
import java.util.regex.Matcher;

import static eu.cqse.JsCodeUtils.indentCode;
import static eu.cqse.JsCodeUtils.multilineSafeNamespacePattern;

public class Constructor extends ClassMember {

	public Constructor(String fullMatch, String docComment, String classNamespace, String declaration) {
		super(fullMatch, docComment, classNamespace, "constructor", declaration);
	}

	@Override
	public String getDocComment() {
		return docComment
				.replaceAll("( \\*)? @final\n?", "")
				.replaceAll("( \\*)? @constructor\n?", "")
				.replaceAll(" \\* @extends.*\n", "")
				.replaceAll(" \\* @implements.*\n", "")
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
			//TODO rewrite for "cannot access this before calling super()"
			// - collect all this.name before super(...
			// - replace first occurrence with let tmp_name;
			// - replace rest as well
			// - add initializations this.name=tmp_name; after super call
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
