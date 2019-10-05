package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SelectionPass {

	private static final JsonAdapter<List<ClosureDependency>> JSON_ADAPTER = new Moshi.Builder().build()
			.adapter(Types.newParameterizedType(List.class, ClosureDependency.class));

	public Set<String> process(File depsFile, boolean includeTests, File... teamscaleUiDir) throws IOException {
		Set<String> tsRequiredNamespaces = getTsRequiredNamespaces(teamscaleUiDir);
		HashMap<String, ClosureDependency> depsByProvide = getStringClosureDependencies(depsFile);
		if (depsByProvide == null) {
			return null;
		}

		return calculateTransitiveClosure(depsByProvide, includeTests, tsRequiredNamespaces);
	}

	private static Set<String> getTsRequiredNamespaces(File... teamscaleUiDirs) throws IOException {
		Set<String> tsRequiredNamespaces = new HashSet<>();
		for (File teamscaleUiDir : teamscaleUiDirs) {
			for (File file : Files.fileTraverser().breadthFirst(teamscaleUiDir)) {
				if (file.isFile()) {
					Pattern p = Pattern.compile("goog\\.(?:require|forwardDeclare)\\(['\"](goog\\..*)['\"]\\)");
					String content = Files.asCharSource(file, Charsets.UTF_8).read();
					Matcher matcher = p.matcher(content);
					while (matcher.find()) {
						tsRequiredNamespaces.add(matcher.group(1));
					}
				}
			}
		}
		return tsRequiredNamespaces;
	}

	private Set<String> calculateTransitiveClosure(HashMap<String, ClosureDependency> depsByProvide, boolean includeTests, Set<String> tsRequiredNamespaces) throws IOException {
		Set<String> transitivelyRequiredClosureFiles = new HashSet<>();
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
		return transitivelyRequiredClosureFiles.stream().map(f -> "closure/goog/" + f).collect(Collectors.toSet());
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
