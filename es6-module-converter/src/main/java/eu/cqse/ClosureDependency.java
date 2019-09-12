
package eu.cqse;

import javax.annotation.Nullable;
import java.util.List;

public class ClosureDependency {

    /** The js file name relative to the closure/goog directory. */
	public String file;

	/** The namespaces/classes provided by the file. */
	public List<String> provides = null;

    /** The namespaces/classes directly required by the file. */
	public List<String> requires = null;

	public Info info;

	public static class Info {
	    /** es5 or es6 */
	    @Nullable
		public String lang;

		/** goog or es6 */
		@Nullable
		public String module;
	}
}
