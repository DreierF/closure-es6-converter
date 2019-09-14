package eu.cqse;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

public class Es6ModuleMasterConverter {

	private static final String INPUT_DIR = "../closure-library-es6";

	public static void main(String[] args) throws IOException {
//		SelectionPass selectionPass = new SelectionPass();
//		selectionPass.process(new File(INPUT_DIR, "closure/goog/deps.js"), new File("../needed.txt"), true);
		CyclicDependencyRemovalPass cycleRemoval = new CyclicDependencyRemovalPass(INPUT_DIR);
		cycleRemoval.process();

		ReaderPass readInPass = new ReaderPass();
		readInPass.process(INPUT_DIR,
				"/Users/florian/Documents/CQSE/TeamscaleWebpack/engine/com.teamscale.ui/src-js",
				"/Users/florian/Documents/CQSE/TeamscaleWebpack/engine/com.teamscale.ui/resources/generated-typedefs",
				"/Users/florian/Documents/CQSE/TeamscaleWebpack/engine/com.teamscale.ui/class-resources/com/teamscale/ui/build/third_party",
				"/Users/florian/Documents/CQSE/TeamscaleWebpack/engine/com.teamscale.ui/build/generated/soy");
		validateProvideRequires(readInPass);
		new ConvertingPass().process(readInPass);

		System.out.println("\n==== Finished ====");
	}

	private static void validateProvideRequires(ReaderPass pass1) {
		Preconditions.checkArgument(!pass1.filesByNamespace.keySet().isEmpty(), "No provided namespaces found");
		Preconditions.checkArgument(!pass1.requiresByFile.keySet().isEmpty(), "No goog.requires found in input files");

		Collection<GoogRequireOrForwardDeclare> requires = pass1.requiresByFile.values();
		Set<String> allRequires = requires.stream().map(require -> require.requiredNamespace).collect(toSet());

		List<String> unmatchedDependencies = new ArrayList<>();
		allRequires.forEach(requiredNamespace -> {
			if (!pass1.filesByNamespace.containsKey(requiredNamespace)) {
				unmatchedDependencies.add(requiredNamespace);
			}
		});
		if (!unmatchedDependencies.isEmpty()) {
			throw new RuntimeException("Dependencies not found:\n" + join("\n", unmatchedDependencies));
		}
	}
}
