package eu.cqse.es6;

import eu.cqse.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.cqse.JsCodeUtils.multilineSafeNamespacePattern;

public class ClassMember {
	private static final Pattern METHOD_DELEGATION_PATTERN = Pattern.compile("\\s*=\\s*[\\w_.\\s]+" + multilineSafeNamespacePattern(".prototype.") + "([\\w_]+);");
	private static final Pattern FUNCTION_DELEGATION_PATTERN = Pattern.compile("\\s*=\\s*[\\w_.\\s]+\\.([\\w_]+);");

	public final String fullMatch;
	public final String docComment;
	public final String classNamespace;
	public final String memberName;
	public final String declaration;

	public ClassMember(String fullMatch, String docComment, String classNamespace, String memberName, String declaration) {
		this.fullMatch = fullMatch;
		this.docComment = docComment;
		this.classNamespace = classNamespace;
		this.memberName = memberName;
		this.declaration = declaration;
	}

	public boolean isMethod() {
		return this.declaration.matches("\\s?=\\s*function.*")
				|| docComment.contains("@param")
				|| docComment.contains("@return")
				|| isExplicitAbstractMethod(declaration);
	}

	public boolean isField() {
		return !isMethod();
	}

	public String getDocComment() {
		if (isExplicitAbstractMethod(declaration)) {
			return docComment
					.replaceAll("\\s*\\* @type \\{function\\(\\) : void}\n", "")
					.replaceAll("(\\s*)\\*/\\s*$", "$1* @abstract$0");
		}
		if (hasNoInitializer(declaration)) {
			if (!docComment.matches("@type \\{.*undefined.*}")) {
				return docComment.replaceAll("@type \\{(.*)}", "@type {$1|undefined}");
			}
		}
		return docComment;
	}

	public String getDeclaration(GoogInheritsInfo googInheritsInfo) {
		String declaration = this.declaration;
		if (hasNoInitializer(declaration) || isExplicitAbstractMethod(declaration)) {
			return memberName + "(" + getInferredParameterList() + ") {}";
		} else if (isFunctionDelegation(declaration)) {
			Matcher matcher = METHOD_DELEGATION_PATTERN.matcher(declaration);
			if (matcher.find()) {
				String delegate = matcher.group(1);
				return memberName + "(" + getInferredParameterList() + ") {\n  return this." + delegate + "();\n}";
			} else {
				String delegate = declaration.replaceFirst("\\s*=\\s*(.*);", "$1");
				return memberName + "(" + getInferredParameterList() + ") {\n  return " + delegate + "();\n}";
			}
		} else {
			declaration = declaration.replaceFirst("\\s?=\\s*function", memberName);
			if (googInheritsInfo != null) {
				declaration = declaration
						.replaceAll(multilineSafeNamespacePattern(googInheritsInfo.fullClassNamespace + ".base")
								+ "\\(\\s*this,\\s*'constructor',?\\s*", "super(");
				declaration = declaration
						.replaceAll(multilineSafeNamespacePattern(googInheritsInfo.fullClassNamespace + ".base")
								+ "\\(\\s*this,\\s*'(\\w+)',?\\s*", "super.$1(");
			}
			return declaration;
		}
	}

	private boolean isFunctionDelegation(String declaration) {
		return FUNCTION_DELEGATION_PATTERN.matcher(declaration).matches();
	}

	public boolean isAbstract() {
		return this.docComment.contains("@abstract") || isExplicitAbstractMethod(this.declaration);
	}

	private boolean hasNoInitializer(String declaration) {
		return declaration.equals(";");
	}

	protected boolean isExplicitAbstractMethod(String declaration) {
		return declaration.matches("\\s*=\\s*goog\\.abstractMethod;");
	}

	protected String getAsEs6Method(GoogInheritsInfo googInheritsInfo) {
		String docComment = getDocComment();
		String declaration = getDeclaration(googInheritsInfo);
		return docComment + declaration;
	}

	/**
	 * Infers the parameters for method definitions that are defined only as
	 * `goog.crypt.BlockCipher.prototype.decrypt;` Where only the doc comment
	 * gives clues how the method should look like.
	 */
	private String getInferredParameterList() {
		Matcher matcher = Pattern.compile("\\* @parm\\s?\\{[^{}]+(}|\\{[^{}]+}) (\\w+)").matcher(this.docComment);
		List<String> parameterList = new ArrayList<>();
		while (matcher.find()) {
			parameterList.add(matcher.group(2));
		}
		return StringUtils.concat(parameterList, ", ");
	}

	public String getEs6Representation(GoogInheritsInfo googInheritsInfo) {
		if (isField()) {
			return getAsEs6Field();
		} else {
			return getAsEs6Method(googInheritsInfo);
		}
	}

	public String getAsEs6Field() {
		String docComment = this.docComment;
		String declaration = this.declaration;
		if (hasNoInitializer(declaration)) {
			declaration = " = undefined;";
		}
		declaration = declaration.replaceFirst("\\s?=\\s*", Matcher.quoteReplacement("this." + memberName) + "$0");
		return docComment + declaration;
	}
}
