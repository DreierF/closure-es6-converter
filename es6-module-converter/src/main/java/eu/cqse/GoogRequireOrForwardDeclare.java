package eu.cqse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

class GoogRequireOrForwardDeclare {

	@Nullable
	 String shortReference;

	@Nullable
	final String importedFunction;

	@Nonnull
	final String requiredNamespace;

	@Nullable
	final String fullText;

	final boolean isForwardDeclare;

	GoogRequireOrForwardDeclare(@Nullable String fullText, @Nonnull String requiredNamespace,
								@Nullable String shortReference, @Nullable String importedFunction, boolean isForwardDeclare) {
		this.fullText = fullText;
		this.shortReference = shortReference;
		this.importedFunction = importedFunction;
		this.requiredNamespace = requiredNamespace;
		this.isForwardDeclare = isForwardDeclare;
	}

	@Override
	public int hashCode() {
		return Objects.hash(shortReference, importedFunction, requiredNamespace);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		GoogRequireOrForwardDeclare that = (GoogRequireOrForwardDeclare) o;
		return Objects.equals(shortReference, that.shortReference)
				&& Objects.equals(importedFunction, that.importedFunction)
				&& requiredNamespace.equals(that.requiredNamespace);
	}
}
