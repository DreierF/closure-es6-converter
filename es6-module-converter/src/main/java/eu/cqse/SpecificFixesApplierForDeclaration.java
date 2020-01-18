package eu.cqse;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpecificFixesApplierForDeclaration {
	private final File outputDir;

	public SpecificFixesApplierForDeclaration(File outputDir) {
		this.outputDir = outputDir;
	}

	public void process() throws IOException {
		replaceInAll("@record", "@interface");
		replaceInAll(Pattern.compile("@(type|param|return)\\s+\\{([A-Z]\\w+)}"), "@$1 {?$2}");
		replace("debug/tracer.js", "Trace_.TracerCallbacks", "TracerCallbacks");
		replace("ui/palette.js", "Palette.CurrentCell_", "CurrentCell_");
		replace("ui/palette.js", "CurrentCell_ = class ", "class CurrentCell_ ");
		replace("i18n/datetimesymbols.js", "@struct", "");

		replace("events/keycodes.js", "@enum {number}", "");
		replace("events/keycodes.js", "let KeyCodes = ", "class KeyCodes ");
		replaceAll("events/keycodes.js", Pattern.compile("([A-Z_0-9]+):\\s*(\\d+),?"), "$1 = $2;");

		replace("positioning/positioning.js", "@enum {number}", "");
		replace("positioning/positioning.js", "let Corner =", "class Corner ");
		replaceAll("positioning/positioning.js", Pattern.compile("(?m)((?:TOP|BOTTOM)_(?:LEFT|RIGHT|CENTER|START|END)):\\s*([^,}]*),?"), "$1 = 0;");

		replaceAll("ui/dialog.js", Pattern.compile("([A-Z]+): Dialog\\.(?:[^,}]*)"), "$1: ''");
		replaceAll("ui/dialog.js", Pattern.compile("([A-Z]+): \\{\r?\n"), "$1 = {\n");
		replaceAll("ui/dialog.js", Pattern.compile("(key|caption): DefaultButton[^,}]*"), "$1: ''");

		replace("net/errorcode.js", "@enum {number}", "");
		replace("net/httpstatus.js", "@enum {number}", "");
	}

	private void replaceInAll(String search, String replace) throws IOException {
		replaceInAll(Pattern.compile(Pattern.quote(search)), Matcher.quoteReplacement(replace));
	}

	private void replaceInAll(Pattern search, String replace) throws IOException {
		Files.walkFileTree(this.outputDir.toPath(),new SimpleFileVisitor<>(){

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if(file.toFile().getName().endsWith(".js")){
							String content = FileUtils.getFileContentSafe(file.toFile());
							content = search.matcher(content).replaceAll(replace);
							try {
								FileUtils.writeFileContent(file.toFile(), content);
							} catch (IOException e) {
								e.printStackTrace();
							}
				}
						return FileVisitResult.CONTINUE;
			}
		});
	}

	private void replace(String file, String search, String replace) throws IOException {
		File fixedJsFile = new File(outputDir, file);
		String content = FileUtils.getFileContentSafe(fixedJsFile);
		if (!content.contains(search)) {
			throw new IllegalStateException(search + " not contained in " + file);
		}
		content = content.replace(search, replace);
		FileUtils.writeFileContent(fixedJsFile, content);
	}

	private void replaceAll(String file, Pattern searchPattern, String replace) throws IOException {
		File fixedJsFile = new File(outputDir, file);
		String content = FileUtils.getFileContentSafe(fixedJsFile);
		if (!searchPattern.matcher(content).find()) {
			throw new IllegalStateException(searchPattern.pattern() + " not contained in " + file);
		}
		content = searchPattern.matcher(content).replaceAll(replace);
		FileUtils.writeFileContent(fixedJsFile, content);
	}
}
