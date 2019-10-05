package eu.cqse;

import com.google.common.base.Preconditions;
import com.google.common.collect.ObjectArrays;
import com.google.common.io.MoreFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static java.lang.String.join;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.stream.Collectors.toSet;

/**
 * Run "./gradlew generateJavascriptDataClasses compileSoy" before
 * */
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
		SelectionPass selectionPass = new SelectionPass();
		Set<String> selectedFiles = selectionPass.process(new File(INPUT_DIR, "closure/goog/deps.js"), true, getUiDirFiles(TEAMSCALE_UI_DIR));
		copyFiles(selectedFiles);
		CyclicDependencyRemovalPass cycleRemoval = new CyclicDependencyRemovalPass(OUTPUT_DIR);
		cycleRemoval.process();

		if (TEAMSCALE_UI_DIR_CONVERTED.exists()) {
			MoreFiles.deleteRecursively(TEAMSCALE_UI_DIR_CONVERTED.toPath(), ALLOW_INSECURE);
		}
		copyFolder(TEAMSCALE_UI_DIR.toPath(), TEAMSCALE_UI_DIR_CONVERTED.toPath());

		ReaderPass readInPass = new ReaderPass();
		readInPass.process(ObjectArrays.concat(OUTPUT_DIR, getUiDirFiles(TEAMSCALE_UI_DIR_CONVERTED)));
		validateProvideRequires(readInPass);
		new ConvertingPass().process(readInPass);

		System.out.println("\n==== Finished ====");
	}

	private static void copyFiles(Set<String> selectedFiles) throws IOException {
		if (OUTPUT_DIR.exists()) {
			MoreFiles.deleteRecursively(OUTPUT_DIR.toPath(), ALLOW_INSECURE);
		}
		for (String selectedFile : selectedFiles) {
			File destination = new File(OUTPUT_DIR, selectedFile);
			destination.getParentFile().mkdirs();
			Files.copy(new File(INPUT_DIR, selectedFile).toPath(), destination.toPath(), REPLACE_EXISTING);
		}
	}

	public static void copyFolder(Path src, Path dest) throws IOException {
		Files.walk(src)
				.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	}

	private static void copy(Path source, Path dest) {
		try {
			Files.copy(source, dest, REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
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
