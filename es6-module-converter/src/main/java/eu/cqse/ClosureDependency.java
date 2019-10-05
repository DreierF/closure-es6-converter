
package eu.cqse;

import java.io.File;
import java.util.List;

public class ClosureDependency {

	/** The js file name relative to the closure/goog directory. */
	public File file;

	/** The namespaces/classes directly required by the file. */
	public List<String> requires = null;

}
