package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectionPass {

	private static final JsonAdapter<List<ClosureDependency>> JSON_ADAPTER = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, ClosureDependency.class));

	public Set<String> process(File depsFile, File neededNamespacesFile, boolean includeTests) throws IOException {
		HashMap<String, ClosureDependency> depsByProvide = getStringClosureDependencies(depsFile);
		if (depsByProvide == null) return null;

		return calculateTransitiveClosure(neededNamespacesFile, depsByProvide, includeTests);
	}

	private Set<String> calculateTransitiveClosure(File neededNamespacesFile, HashMap<String, ClosureDependency> depsByProvide, boolean includeTests) throws IOException {
		ImmutableList<String> neededNamespaces = Files.asCharSource(neededNamespacesFile, Charsets.UTF_8).readLines();

		Set<String> transitivelyRequiredClosureFiles = new HashSet<>();
		Set<String> unsatisfiedDependencies = new HashSet<>(neededNamespaces);
		Set<String> processedDependencies = new HashSet<>();
		for (String unsatisfiedDependency : unsatisfiedDependencies) {
			ClosureDependency closureDependency = depsByProvide.get(unsatisfiedDependency);
			transitivelyRequiredClosureFiles.add(closureDependency.file);
			processedDependencies.add(unsatisfiedDependency);
			for (String require : closureDependency.requires) {
				if (!processedDependencies.contains(require)) {
					unsatisfiedDependencies.add(require);
				}
			}
			ClosureDependency closureTestDependency = depsByProvide.get(unsatisfiedDependency + "Test");
			if (includeTests && closureTestDependency != null) {
				transitivelyRequiredClosureFiles.add(closureTestDependency.file);
				processedDependencies.add(unsatisfiedDependency);
				for (String require : closureTestDependency.requires) {
					if (!processedDependencies.contains(require)) {
						unsatisfiedDependencies.add(require);
					}
				}
			}
		}
		return transitivelyRequiredClosureFiles;
	}

	private HashMap<String, ClosureDependency> getStringClosureDependencies(File depsFile) throws IOException {
		String content = Files.asCharSource(depsFile, Charsets.UTF_8).read();
		String jsonRepresentation = "[" + content.replaceAll("//.*", "")
				.replaceAll("goog.addDependency\\(([^,]+), ([^]]+]),([^]]+]), ([^)]+)\\);", "{'file': $1, 'provides': $2, 'requires': $3, 'info': $4},")
				.stripTrailing()
				.replace('\'', '"');
		jsonRepresentation = jsonRepresentation.substring(0, jsonRepresentation.length() - 1) + "]";

		List<ClosureDependency> closureDependencies = JSON_ADAPTER.fromJson(jsonRepresentation);
		if (closureDependencies == null) {
			System.err.println("Failed to parse json!");
			return null;
		}

		HashMap<String, ClosureDependency> depsByProvide = new HashMap<>();
		for (ClosureDependency dependency : closureDependencies) {
			for (String provide : dependency.provides) {
				depsByProvide.put(provide, dependency);
			}
		}
		return depsByProvide;
	}
}
