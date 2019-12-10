package eu.cqse.es6;

import eu.cqse.JsCodeUtils;

import java.util.ArrayList;
import java.util.List;

public class ClassContainer {
	public final Constructor constructor;
	public GoogInheritsInfo googInheritsInfo;
	public List<ClassMember> classMembers = new ArrayList<>();

	public ClassContainer(Constructor constructor) {
		this.constructor = constructor;
	}

	public void setInherits(GoogInheritsInfo googInheritsInfo) {
		this.googInheritsInfo = googInheritsInfo;
	}

	public void setClassMembers(List<ClassMember> classMembers) {
		if (classMembers != null) {
			this.classMembers.addAll(classMembers);
		}
	}

	public String getDocComment() {
		String docComment = constructor.docComment
				.replaceAll(" \\* @param.*\n?", "")
				.replaceAll("( \\*)? @constructor\n?", "");
		boolean isAbstractClass = classMembers.stream().anyMatch(ClassMember::isAbstract) && !docComment.contains("@interface") && !docComment.contains("@abstract");
		if (isAbstractClass) {
			return docComment.replaceAll("(\\s*)\\*/\\s*$", "$1* @abstract$0");
		}
		return docComment;
	}

	public String buildEs6Class() {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(getDocComment());

		if (constructor.constLetVar != null) {
			stringBuilder.append(constructor.constLetVar);
		}

		stringBuilder.append(constructor.classNamespace).append(" = class ");
		if (googInheritsInfo != null) {
			stringBuilder.append("extends ").append(googInheritsInfo.extendedFullNamespace).append(" ");
		}
		stringBuilder.append("{");

		stringBuilder.append("\n\n");
		stringBuilder.append(JsCodeUtils.indentCode(constructor.getEs6Representation(classMembers, googInheritsInfo)));

		for (ClassMember classMember : classMembers) {
			if (classMember.isMethod()) {
				stringBuilder.append("\n\n");
				stringBuilder.append(JsCodeUtils.indentCode(classMember.getEs6Representation(googInheritsInfo)));
			}
		}

		stringBuilder.append("\n").append("}");

		return stringBuilder.toString();
	}

}
