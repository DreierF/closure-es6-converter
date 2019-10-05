package eu.cqse.es6;

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

	public String buildEs6Class() {
		StringBuilder stringBuilder = new StringBuilder();

		String classComment = constructor.docComment.replaceAll(" \\* @param.*\n", "").replaceAll(" \\* @constructor\n", "");
		stringBuilder.append(classComment);

		stringBuilder.append(constructor.classNamespace).append(" = class ");
		if (googInheritsInfo != null) {
			stringBuilder.append("extends ").append(googInheritsInfo.extendedFullNamespace).append(" ");
		}
		stringBuilder.append("{\n\n");

		stringBuilder.append(constructor.getEs6Constructor(classMembers));

		for (ClassMember classMember : classMembers) {
			if (classMember.isMethod()) {
				stringBuilder.append(classMember.docComment);
				String declaration = classMember.declaration;
				declaration = declaration.replaceFirst("\\s?=\\s*function", classMember.memberName);
				stringBuilder.append(declaration);
			}
		}

		stringBuilder.append("\n").append("}");

		return stringBuilder.toString();
	}

}
