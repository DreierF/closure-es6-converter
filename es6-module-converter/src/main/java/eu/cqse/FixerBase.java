package eu.cqse;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static java.nio.file.FileVisitResult.CONTINUE;

public abstract class FixerBase {

	private final Path folder;
	protected final String extension;

	protected String fileContentSafe;
	protected Path filePath;

	protected FixerBase(Path folder, String extension) {
		this.folder = folder;
		this.extension = extension;
	}

	public void fixAllTo(File into) throws IOException {
		Files.walkFileTree(folder, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toFile().getName().endsWith("." + extension)) {
					File fixedFile = into.toPath().resolve(folder.relativize(file)).toFile();
					writeFixTo(file, fixedFile);
				}
				return CONTINUE;
			}
		});
	}

	public void fixAllInPlace() throws IOException {
		Files.walkFileTree(folder, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.toFile().getName().endsWith("." + extension)) {
					writeFixTo(file, file.toFile());
				}
				return CONTINUE;
			}
		});
	}

	public void writeFixTo(Path filePath, File fixedFile) throws IOException {
		fileContentSafe = FileUtils.getFileContentSafe(filePath.toFile());
		this.filePath = filePath;
		fix();
		FileUtils.writeFileContent(fixedFile, fileContentSafe);
	}

	protected abstract void fix();

	protected void adjustIn(String fileName, String search, String replace) {
		if (!filePath.endsWith(fileName + "." + extension)) {
			return;
		}
		if (!fileContentSafe.contains(search)) {
//			throw new IllegalStateException(search + " not contained in " + fileName + " " + filePath);
			System.err.println(search + " not contained in " + fileName + " " + filePath);
		}
		fileContentSafe = fileContentSafe.replace(search, replace);
	}


	protected void adjustIn(String file, Pattern searchPattern, String replace) {
		if (!filePath.endsWith(file + "." + extension)) {
			return;
		}
		if (!searchPattern.matcher(fileContentSafe).find()) {
			throw new IllegalStateException(searchPattern.pattern() + " not contained in " + file);
		}
		fileContentSafe = searchPattern.matcher(fileContentSafe).replaceAll(replace);
	}

	protected void adjustIn(String file, Pattern searchPattern, Function<MatchResult, String> replacer) {
		if (!filePath.endsWith(file + "." + extension)) {
			return;
		}
		if (!searchPattern.matcher(fileContentSafe).find()) {
			throw new IllegalStateException(searchPattern.pattern() + " not contained in " + file);
		}
		fileContentSafe = searchPattern.matcher(fileContentSafe).replaceAll(replacer);
	}

	protected void adjustInAll(String search, String replace) {
		fileContentSafe = fileContentSafe.replace(search, replace);
	}

	protected void adjustInAll(Pattern search, String replace) {
		fileContentSafe = search.matcher(fileContentSafe).replaceAll(replace);
	}

	protected void prependIn(String s, String content) {
		if (filePath.endsWith(s + "." + extension)) {
			fileContentSafe = content + fileContentSafe;
		}
	}

	protected void appendIn(String s, String content) {
		if (filePath.endsWith(s + "." + extension)) {
			fileContentSafe += content;
		}
	}
}
