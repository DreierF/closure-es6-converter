package eu.cqse.es6;

import java.util.List;
import java.util.regex.Matcher;

import static eu.cqse.JsCodeUtils.indentCode;
import static eu.cqse.JsCodeUtils.multilineSafeNamespacePattern;

public class Constructor extends ClassMember {

	public final String constLetVar;

	public Constructor(String fullMatch, String docComment, String classNamespace, String declaration, String constLetVar) {
		super(fullMatch, docComment, classNamespace, "constructor", declaration, false);
		this.constLetVar = constLetVar;
	}

	@Override
	public String getDocComment() {
		return docComment
				.replaceAll("( \\*)? @final\r?\n?", "")
				.replaceAll("( \\*)? @struct\r?\n?", "")
				.replaceAll("( \\*)? @constructor\r?\n?", "")
				.replaceAll(" \\* @extends.*\r?\n", "")
				.replaceAll(" \\* @implements.*\r?\n", "")
				.replaceAll(" \\* @interface\r?\n", "")
				.replaceAll("( \\*)? @abstract\r?\n", "")
				.replace("* */", "*/");
	}

	@Override
	public String getDeclaration(GoogInheritsInfo googInheritsInfo) {
		String declaration = super.getDeclaration(googInheritsInfo);
		if (googInheritsInfo != null) {
			declaration = declaration
					.replaceAll(multilineSafeNamespacePattern(googInheritsInfo.extendedFullNamespace + ".call") + "\\s*\\(\\s*this,?\\s*", "super(");
			if (!declaration.contains("super(")) {
				declaration = declaration.replaceAll("constructor\\([^)]+\\)\\s*\\{\\s*", "$0super();\r\n\n  ");
			}
		}
		return declaration;
	}

	public String getEs6Representation(List<ClassMember> classMembers, GoogInheritsInfo googInheritsInfo) {
		String constructorDefinition = this.getAsEs6Method(googInheritsInfo);

		StringBuilder constructorExtensionBuilder = new StringBuilder();
		for (ClassMember classMember : classMembers) {
			if (classMember.isField()) {
				constructorExtensionBuilder.append("\r\n");
				constructorExtensionBuilder.append(indentCode(classMember.getAsEs6Field()));
				constructorExtensionBuilder.append("\r\n");
			}
		}
		String insertAfter;
		if (googInheritsInfo != null) {
			insertAfter = "(?m)^\\s*super\\([^;]+;";
		} else {
			insertAfter = "(?m)^\\s*constructor\\([^{]+\\{";
		}
		constructorDefinition = constructorDefinition
				.replaceFirst(insertAfter, "$0" + Matcher.quoteReplacement(constructorExtensionBuilder.toString()))
				.replaceFirst("};$", "}");
		return constructorDefinition;
	}
}
