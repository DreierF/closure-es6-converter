package eu.cqse;

import com.google.common.base.Preconditions;

public class ExportedEntity implements Comparable<ExportedEntity> {
	public final String externalName;
	public final String internalName;

	public ExportedEntity(String externalName, String internalName) {
		Preconditions.checkArgument(!externalName.isBlank());
		Preconditions.checkArgument(!internalName.isBlank());
		this.externalName = externalName;
		this.internalName = internalName;
	}

	public ExportedEntity(String externalAndInternalName) {
		this(externalAndInternalName, externalAndInternalName);
	}

	@Override
	public int compareTo(ExportedEntity o) {
		return internalName.compareTo(o.internalName);
	}

	public String toEs6Fragment() {
		if (this.externalName.equals(this.internalName)) {
			return this.internalName;
		} else {
			return this.internalName + " as " + this.externalName;
		}
	}
}
