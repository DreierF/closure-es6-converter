package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.CommandLineRunner;
import eu.cqse.es6.Es6ClassConversionPass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.join;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.stream.Collectors.toSet;

/**
 * HOWTO:
 * - Adjust TEAMSCALE_UI_DIR to point to a clone of the TS repo.
 * - Make sure you have checked out the branch "experimental/webpack"
 * - Run "./gradlew generateJavascriptDataClasses compileSoy" in this repo
 * <p>
 * Run yarn install to install tsc
 */
public class Es6ModuleMasterConverter {

	private static final File INPUT_DIR = new File("../closure-library");
	private static final File OUTPUT_DIR = new File("../closure-library-converted/lib");
	private static final File TEMP_DIR = new File("../temp");
	private static final boolean INCLUDE_TESTS = false;
	private static final File REQUIRED_NAMESPACES = new File("required-namespaces.txt");

	public static void main(String[] args) throws IOException, InterruptedException {
		convert();

		generateTSDeclarationFiles();

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

	private static void generateTSDeclarationFiles() throws IOException, InterruptedException {
		// Remove old .d.ts files
		Files.walkFileTree(OUTPUT_DIR.toPath(), new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (isDeclarationFile(file)) {
					file.toFile().delete();
				}
				return CONTINUE;
			}
		});

		Path typings = new File("../typings").toPath();
		FileUtils.safeDeleteDir(TEMP_DIR.toPath());
		FileUtils.safeDeleteDir(typings);
		TEMP_DIR.mkdirs();
		FileUtils.copyFolder(OUTPUT_DIR.toPath(), TEMP_DIR.toPath());

		new SpecificFixesApplierForDeclaration(TEMP_DIR.toPath()).fixAllInPlace();

		// Generate .d.ts files in typings
		runTS();

		new DeclarationFixer(typings).fixAllTo(OUTPUT_DIR);

//		FileUtils.safeDeleteDir(TEMP_DIR.toPath());
//		FileUtils.safeDeleteDir(typings);
	}

	private static boolean isDeclarationFile(Path f) {
		return f.toFile().getName().endsWith(".d.ts");
	}

	public static void runTS() throws IOException, InterruptedException {
		Runtime rt = Runtime.getRuntime();
		String[] commands = {"../node_modules/.bin/tsc"};
		Process proc = rt.exec(commands);

		BufferedReader stdInput = new BufferedReader(new
				InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new
				InputStreamReader(proc.getErrorStream()));

		int exitCode = proc.waitFor();

		// Read the output from the command
		System.out.println("Here is the standard output of the command:\n");
		String s = null;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}

		// Read any errors from the attempted command
		System.out.println("Here is the standard error of the command (if any):\n");
		while ((s = stdError.readLine()) != null) {
			System.out.println(s);
		}

		if (exitCode != 0) {
			throw new RuntimeException("d.ts generation exited with errors.");
		}
	}

	private static void convert() throws IOException {
		Set<String> tsRequiredNamespaces = getTsRequiredNamespaces();

		ReaderPass readClosureLib = new ReaderPass();
		readClosureLib.process(INPUT_DIR);

		SelectionPass selectionPass = new SelectionPass();
		Set<File> selectedFiles = selectionPass.process(readClosureLib, INCLUDE_TESTS, tsRequiredNamespaces);
		FileUtils.copyFiles(selectedFiles, INPUT_DIR.toPath(), OUTPUT_DIR.toPath());
		Files.walkFileTree(OUTPUT_DIR.toPath(), new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				File dest = new File(file.toFile().getAbsolutePath().replace("third_party/", "").replace("closure/goog/", ""));
				dest.getParentFile().mkdirs();
				file.toFile().renameTo(dest);
				return CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (dir.toFile().listFiles().length == 0) {
					dir.toFile().delete();
				}
				return super.postVisitDirectory(dir, exc);
			}
		});

		Files.copy(INPUT_DIR.toPath().resolve("closure/goog/base.js"), OUTPUT_DIR.toPath().resolve("google.js"));

		new SpecificFixesApplier(OUTPUT_DIR.toPath()).fixAllInPlace();

		CyclicDependencyRemovalPass cycleRemoval = new CyclicDependencyRemovalPass(OUTPUT_DIR);
		cycleRemoval.process();

		Es6ClassConversionPass es6Conversion = new Es6ClassConversionPass();
		es6Conversion.process(OUTPUT_DIR);

		ReaderPass readInPass = new ReaderPass();
		readInPass.process(OUTPUT_DIR);
		validateProvideRequires(readInPass);
		new ConvertingPass().process(readInPass);
	}

	private static HashSet<String> getTsRequiredNamespaces() throws IOException {
		return new HashSet<>(com.google.common.io.Files.asCharSource(REQUIRED_NAMESPACES, Charsets.UTF_8).readLines());
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
