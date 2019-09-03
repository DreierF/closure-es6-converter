package eu.cqse;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public abstract class Es6Processor {

    void process(String inputDirPath) throws IOException {
        File inputDir = new File(inputDirPath);

		if (!inputDir.isDirectory()) {
			throw new IOException("Input dir not found");
		}

		for (File file : Files.fileTraverser().breadthFirst(inputDir)) {
			if (isRelevantJsFile(file)) {
				processJsFile(file);
			}
		}

		System.out.println("\n==== Finished ====");
	}

	private boolean isRelevantJsFile(File file) {
		String lowerCaseFileName = file.getName().toLowerCase();
		Set<String> absolutePathSegments = Sets.newHashSet(Splitter.on(File.separatorChar).split(file.getAbsolutePath()));
		return lowerCaseFileName.endsWith(".js")
				&& !(absolutePathSegments.contains("less") || absolutePathSegments.contains("js-cache") || absolutePathSegments.contains("testing"))
				&& StringUtils.containsOneOf(file.getAbsolutePath(), "closure-library", "src-js",
				"third_party/closure/")
				&& !StringUtils.endsWithOneOf(lowerCaseFileName, "_test.js", "_perf.js", "tester.js",
				"alltests.js", "testhelpers.js", "testing.js", "relativecommontests.js", "mockiframeio.js");
	}

    protected abstract void processJsFile(File jsFile) throws IOException;
}
