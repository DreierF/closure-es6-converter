package eu.cqse.es6;

public class ClassMember {
	public final String fullMatch;
	public final String docComment;
	public final String classNamespace;
	public final String memberName;
	public final String declaration;
	public final EMemberType memberType;

	public ClassMember(String fullMatch, String docComment, String classNamespace, String memberName, String declaration) {

		this.fullMatch = fullMatch;
		this.docComment = docComment;
		this.classNamespace = classNamespace;
		this.memberName = memberName;
		this.declaration = declaration;
		if (this.declaration.matches("\\s?=\\s*function.*")) {
			this.memberType = EMemberType.METHOD;
		} else if (this.declaration.equals(";")) {
			this.memberType = EMemberType.UNINITIALIZED_FIELD;
		} else {
			this.memberType = EMemberType.FIELD;
		}
	}

	public boolean isMethod() {
		return memberType == EMemberType.METHOD;
	}

	public boolean isField() {
		return memberType != EMemberType.METHOD;
	}

	private enum EMemberType {
		METHOD, FIELD, UNINITIALIZED_FIELD
	}
}
