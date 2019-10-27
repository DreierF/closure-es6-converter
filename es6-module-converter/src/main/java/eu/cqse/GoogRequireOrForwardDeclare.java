package eu.cqse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class GoogRequireOrForwardDeclare {

	@Nullable
	public String shortReference;

	@Nullable
	public final String importedFunction;

	@Nonnull
	public final String requiredNamespace;

	@Nullable
	public final String fullText;

	final ERequireType requireType;

	GoogRequireOrForwardDeclare(@Nullable String fullText, @Nonnull String requiredNamespace,
								@Nullable String shortReference, @Nullable String importedFunction, ERequireType requireType) {
		this.fullText = fullText;
		this.shortReference = shortReference;
		this.importedFunction = importedFunction;
		this.requiredNamespace = requiredNamespace;
		this.requireType = requireType;
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

	enum ERequireType {
		GOOG_REQUIRE,
		GOOG_FORWARD_DECLARE,
		IMPLICIT_STRICT,
		IMPLICIT_LENIENT
	}
}
