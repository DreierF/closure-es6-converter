package eu.cqse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class GoogRequireOrForwardDeclare {

	/** The alias under which a whole namespace is imported
	 * i.e. "const GoogIterable = goog.require('goog.iter.Iterable');"
	 * => shortReference="GoogIterable"
	 */
	@Nullable
	public String shortReference;

	public final List<AliasedElement> importedFunctions;

	@Nonnull
	public final String requiredNamespace;

	@Nullable
	public final String fullText;

	final ERequireType requireType;

	GoogRequireOrForwardDeclare(@Nullable String fullText, @Nonnull String requiredNamespace,
								@Nullable String shortReference, List<AliasedElement> importedFunctions, ERequireType requireType) {
		this.fullText = fullText;
		this.shortReference = shortReference;
		this.importedFunctions = importedFunctions;
		this.requiredNamespace = requiredNamespace;
		this.requireType = requireType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(shortReference, importedFunctions, requiredNamespace);
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
				&& Objects.equals(importedFunctions, that.importedFunctions)
				&& requiredNamespace.equals(that.requiredNamespace);
	}

	enum ERequireType {
		GOOG_REQUIRE,
		GOOG_FORWARD_DECLARE,
		IMPLICIT_STRICT,
		IMPLICIT_LENIENT
	}
}
