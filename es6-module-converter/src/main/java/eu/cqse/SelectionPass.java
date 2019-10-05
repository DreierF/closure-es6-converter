package eu.cqse;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SelectionPass {

	public Set<File> process(ReaderPass depsFile, boolean includeTests, ReaderPass teamscaleUiDir) {
		Set<String> tsRequiredNamespaces = getTsRequiredNamespaces(teamscaleUiDir);
		HashMap<String, ClosureDependency> depsByProvide = getStringClosureDependencies(depsFile);

		return calculateTransitiveClosure(depsByProvide, includeTests, tsRequiredNamespaces);
	}

	private static Set<String> getTsRequiredNamespaces(ReaderPass teamscaleUiDirs) {
		return teamscaleUiDirs.requiresByFile.values().stream().map(require -> require.requiredNamespace).filter(namespace -> namespace.startsWith("goog.")).collect(Collectors.toSet());
	}

	private Set<File> calculateTransitiveClosure(HashMap<String, ClosureDependency> depsByProvide, boolean includeTests, Set<String> tsRequiredNamespaces) {
		Set<File> transitivelyRequiredClosureFiles = new HashSet<>();
		Set<String> unsatisfiedDependencies = new HashSet<>(tsRequiredNamespaces);
		Set<String> processedDependencies = new HashSet<>();
		while (!unsatisfiedDependencies.isEmpty()) {
			for (String unsatisfiedDependency : new HashSet<>(unsatisfiedDependencies)) {
				if (!depsByProvide.containsKey(unsatisfiedDependency)) {
					throw new AssertionError("Dependency " + unsatisfiedDependency + " is unknown!");
				}
				ClosureDependency closureDependency = depsByProvide.get(unsatisfiedDependency);
				transitivelyRequiredClosureFiles.add(closureDependency.file);
				processedDependencies.add(unsatisfiedDependency);
				unsatisfiedDependencies.remove(unsatisfiedDependency);
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
		}
		return transitivelyRequiredClosureFiles;
	}

	private HashMap<String, ClosureDependency> getStringClosureDependencies(ReaderPass closureLib) {
		HashMap<String, ClosureDependency> depsByProvide = new HashMap<>();
		for (String providedNamespace : closureLib.filesByNamespace.keySet()) {
			ClosureDependency dependency = new ClosureDependency();
			dependency.file = closureLib.filesByNamespace.get(providedNamespace);
			dependency.requires = closureLib.requiresByFile.get(dependency.file).stream().map(r -> r.requiredNamespace).collect(Collectors.toList());
			depsByProvide.put(providedNamespace, dependency);
		}
		return depsByProvide;
	}
}
