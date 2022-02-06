package eu.cqse;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.MoreFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileUtils {
	public static void processRelevantJsFiles(Consumer<File> processor, File... inputDirPaths) throws IOException {
		for (File inputDir : inputDirPaths) {
			if (!inputDir.isDirectory()) {
				throw new IOException("Input dir " + inputDir + " not found!");
			}

			for (File file : Files.fileTraverser().breadthFirst(inputDir)) {
				if (isRelevantJsFile(file)) {
					processor.accept(file);
				}
			}
		}
	}

	private static boolean isRelevantJsFile(File file) {
		String lowerCaseFileName = file.getName().toLowerCase();
		Set<String> absolutePathSegments = Sets.newHashSet(Splitter.on(File.separatorChar).split(file.getAbsolutePath()));
		return lowerCaseFileName.endsWith(".js")
				&& !(absolutePathSegments.contains("less") || absolutePathSegments.contains("js-cache")
				|| absolutePathSegments.contains("testing") || absolutePathSegments.contains("scripts"))
				&& StringUtils.containsOneOf(file.getAbsolutePath(), "closure-library", "src-js",
				"third_party", "generated-typedefs", "soy")
				&& !StringUtils.endsWithOneOf(lowerCaseFileName, "_test.js", "_perf.js", "tester.js",
				"alltests.js", "testhelpers.js", "testing.js", "relativecommontests.js", "mockiframeio.js");
	}

	public static void safeDeleteDir(Path path) throws IOException {
		if (path.toFile().exists()) {
			MoreFiles.deleteRecursively(path, ALLOW_INSECURE);
		}
	}

	public static void copyFolder(Path src, Path dest) throws IOException {
		java.nio.file.Files.walk(src).forEach(source -> copy(source, dest.resolve(src.relativize(source))));
	}

	private static void copy(Path source, Path dest) {
		try {
			java.nio.file.Files.copy(source, dest, REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static void copyFiles(Set<File> selectedFiles, Path sourceDir, Path destinationDir) throws IOException {
		safeDeleteDir(destinationDir);
		for (File selectedFile : selectedFiles) {
			File destination = destinationDir.resolve(sourceDir.relativize(selectedFile.toPath())).toFile();
			destination.getParentFile().mkdirs();
			java.nio.file.Files.copy(selectedFile.toPath(), destination.toPath(), REPLACE_EXISTING);
		}
	}

	public static String getFileContentSafe(File jsFile) {
		try {
			return Files.asCharSource(jsFile, Charsets.UTF_8).read().replace("\uFEFF", "");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void writeFileContent(File file, String content) throws IOException {
		Files.asCharSink(file, Charsets.UTF_8).write(content);
	}
}
