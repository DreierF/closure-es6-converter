package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.CommandLineRunner;
import eu.cqse.es6.Es6ClassConversionPass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

/**
 * HOWTO:
 * - Adjust TEAMSCALE_UI_DIR to point to a clone of the TS repo.
 * - Make sure you have checked out the branch "experimental/webpack"
 * - Run "./gradlew generateJavascriptDataClasses compileSoy" in this repo
 */
public class Es6ModuleMasterConverter {

	private static final File INPUT_DIR = new File("../closure-library");
	private static final File OUTPUT_DIR = new File("../closure-library-converted/lib");
	private static final File TEAMSCALE_UI_DIR = new File("/Users/florian/Documents/CQSE/Teamscale/engine/com.teamscale.ui");
	//    public static final File TEAMSCALE_UI_DIR_CONVERTED = new File(TEAMSCALE_UI_DIR.getAbsolutePath() + ".converted");
	private static final boolean INCLUDE_TESTS = false;
    private static final File REQUIRED_NAMESPACES = new File("required-namespaces.txt");

    private static File[] getUiDirFiles(File teamscaleUiDir) {
		return new File[]{new File(teamscaleUiDir, "src-js"),
				new File(teamscaleUiDir, "generated-src-js/typedefs"),
				new File(teamscaleUiDir, "resources/third_party"),
				new File(teamscaleUiDir, "build/generated/soy")};
	}

	public static void main(String[] args) throws IOException {
    	// Uncomment the below line to update the required-namespaces.txt
    	//updateRequiredNamespaces()

		convert();

		CommandLineRunner.main(new String[]{"-O", "ADVANCED",
				"--warning_level", "VERBOSE",
				"--jscomp_error='*'",
				"--jscomp_off=strictMissingRequire",
				"--jscomp_off=extraRequire",
				"--jscomp_off=deprecated",
				"--jscomp_off=lintChecks",
				"--jscomp_off=analyzerChecks",
				"--jscomp_off=strictMissingProperties",
				"--jscomp_off=strictPrimitiveOperators",
				"--jscomp_off=messageConventions",
				"--jscomp_warning=unusedLocalVariables",
				"--js='" + OUTPUT_DIR + "/**.js'",
				"--js='!./closure-deps/**.js'",
				"--js='!**_test.js'",
				"--js='!**_perf.js'",
				"--js='!**tester.js'",
				"--js='!**promise/testsuiteadapter.js'",
				"--js='!**relativecommontests.js'",
				"--js='!**osapi/osapi.js'",
				"--js='!**svgpan/svgpan.js'",
				"--js='!**alltests.js'",
				"--js='!**node_modules**.js'",
				"--js='!**protractor_spec.js'",
				"--js='!**protractor.conf.js'",
				"--js='!**browser_capabilities.js'",
				"--js='!**generate_closure_unit_tests.js'",
				"--js='!./doc/**.js'",
				"--js='!**debug_loader_integration_tests/testdata/**'",
				"--js_output_file=\"compiled.js\""});

		System.out.println("\r\n==== Finished ====");
	}

	private static void convert() throws IOException {
		Set<String> tsRequiredNamespaces = getTsRequiredNamespaces();

		ReaderPass readClosureLib = new ReaderPass();
		readClosureLib.process(INPUT_DIR);

		SelectionPass selectionPass = new SelectionPass();
		Set<File> selectedFiles = selectionPass.process(readClosureLib, INCLUDE_TESTS, tsRequiredNamespaces);
		FileUtils.copyFiles(selectedFiles, INPUT_DIR.toPath(), OUTPUT_DIR.toPath());
		Files.copy(INPUT_DIR.toPath().resolve("closure/goog/base.js"), OUTPUT_DIR.toPath().resolve("closure/goog/google.js"));
		if (INCLUDE_TESTS) {
			//TODO copy actual tests
			FileUtils.copyFolder(new File(INPUT_DIR, "scripts").toPath(), new File(OUTPUT_DIR, "scripts").toPath());
		}

		SpecificFixesApplier fixer = new SpecificFixesApplier(OUTPUT_DIR);
		fixer.process();

		CyclicDependencyRemovalPass cycleRemoval = new CyclicDependencyRemovalPass(OUTPUT_DIR);
		cycleRemoval.process();

		Es6ClassConversionPass es6Conversion = new Es6ClassConversionPass();
		es6Conversion.process(OUTPUT_DIR);

//        FileUtils.safeDeleteDir(TEAMSCALE_UI_DIR_CONVERTED.toPath());
//        FileUtils.copyFolder(TEAMSCALE_UI_DIR.toPath(), TEAMSCALE_UI_DIR_CONVERTED.toPath());

		ReaderPass readInPass = new ReaderPass();
		readInPass.process(OUTPUT_DIR/*ObjectArrays.concat(OUTPUT_DIR, getUiDirFiles(TEAMSCALE_UI_DIR_CONVERTED))*/);
		validateProvideRequires(readInPass);
		new ConvertingPass().process(readInPass);
	}

	private static HashSet<String> getTsRequiredNamespaces() throws IOException {
		return new HashSet<>(com.google.common.io.Files.asCharSource(REQUIRED_NAMESPACES, Charsets.UTF_8).readLines());
	}

	private static void updateRequiredNamespaces() throws IOException {
		ReaderPass readTs = new ReaderPass();
		readTs.process(getUiDirFiles(TEAMSCALE_UI_DIR));
		Set<String> tsRequiredNamespaces = getTsRequiredNamespaces(readTs);
		com.google.common.io.Files.asCharSink(REQUIRED_NAMESPACES, Charsets.UTF_8)
				.writeLines(tsRequiredNamespaces.stream().sorted());

		System.exit(0);
	}

	private static Set<String> getTsRequiredNamespaces(ReaderPass teamscaleUiDirs) {
		return teamscaleUiDirs.requiresByFile.values().stream()
				.filter(r -> r.requireType != GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_LENIENT)
				.map(require -> require.requiredNamespace)
				.filter(namespace -> namespace.startsWith("goog.")).collect(Collectors.toSet());
	}

	private static void validateProvideRequires(ReaderPass pass1) {
		Preconditions.checkArgument(!pass1.filesByNamespace.keySet().isEmpty(), "No provided namespaces found");
		Preconditions.checkArgument(!pass1.requiresByFile.keySet().isEmpty(), "No goog.requires found in input files");

		Collection<GoogRequireOrForwardDeclare> requires = pass1.requiresByFile.values();
		Set<String> allRequires = requires.stream()
				.filter(r -> r.requireType != GoogRequireOrForwardDeclare.ERequireType.IMPLICIT_LENIENT)
				.map(require -> require.requiredNamespace).collect(toSet());

		List<String> unmatchedDependencies = new ArrayList<>();
		allRequires.forEach(requiredNamespace -> {
			if (!pass1.filesByNamespace.containsKey(requiredNamespace)) {
				unmatchedDependencies.add(requiredNamespace);
			}
		});
		if (!unmatchedDependencies.isEmpty()) {
			throw new RuntimeException("Dependencies not found:" + join(", ", unmatchedDependencies));
		}
	}
}
