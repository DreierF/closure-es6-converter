package eu.cqse;

import com.google.common.base.Preconditions;
import com.google.common.collect.ObjectArrays;
import eu.cqse.es6.Es6ClassConversionPass;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

/**
 * Run "./gradlew generateJavascriptDataClasses compileSoy" before
 */
public class Es6ModuleMasterConverter {

	private static final File INPUT_DIR = new File("../closure-library");
	private static final File OUTPUT_DIR = new File("../closure-library-converted");
	private static final File TEAMSCALE_UI_DIR = new File("/Users/florian/Documents/CQSE/TeamscaleWebpack/engine/com.teamscale.ui");
	public static final File TEAMSCALE_UI_DIR_CONVERTED = new File(TEAMSCALE_UI_DIR.getAbsolutePath() + ".converted");

	private static File[] getUiDirFiles(File teamscaleUiDir) {
		return new File[]{new File(teamscaleUiDir, "src-js"),
				new File(teamscaleUiDir, "resources/generated-typedefs"),
				new File(teamscaleUiDir, "class-resources/com/teamscale/ui/build/third_party"),
				new File(teamscaleUiDir, "build/generated/soy")};
	}

	public static void main(String[] args) throws IOException {
		ReaderPass readClosureLib = new ReaderPass();
		readClosureLib.process(INPUT_DIR);

		ReaderPass readTs = new ReaderPass();
		readTs.process(getUiDirFiles(TEAMSCALE_UI_DIR));

		SelectionPass selectionPass = new SelectionPass();
		Set<File> selectedFiles = selectionPass.process(readClosureLib, true, readTs);
		selectedFiles.add(new File(INPUT_DIR, "closure/goog/base.js"));
		FileUtils.copyFiles(selectedFiles, INPUT_DIR.toPath(), OUTPUT_DIR.toPath());
		CyclicDependencyRemovalPass cycleRemoval = new CyclicDependencyRemovalPass(OUTPUT_DIR);
		cycleRemoval.process();

		Es6ClassConversionPass es6Conversion = new Es6ClassConversionPass();
		es6Conversion.process(OUTPUT_DIR);

		FileUtils.safeDeleteDir(TEAMSCALE_UI_DIR_CONVERTED.toPath());
		FileUtils.copyFolder(TEAMSCALE_UI_DIR.toPath(), TEAMSCALE_UI_DIR_CONVERTED.toPath());

		ReaderPass readInPass = new ReaderPass();
		readInPass.process(ObjectArrays.concat(OUTPUT_DIR, getUiDirFiles(TEAMSCALE_UI_DIR_CONVERTED)));
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
