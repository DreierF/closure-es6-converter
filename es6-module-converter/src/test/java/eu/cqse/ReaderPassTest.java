package eu.cqse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderPassTest {

	@Test
	void extractExportsOfGoogModuleSingleNamedExport() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\r\ntest:test\r\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("test");
	}

	@Test
	void extractExportsOfGoogModuleTrailingComma() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:test,\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("test");
	}

	@Test
	void extractExportsOfGoogModuleMultipleNamedExports() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:test, \nsome:some\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("test", "some");
	}

	@Test
	void extractExportsOfGoogModuleMultipleNamedExportsWithBlockComment() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:testInt, \nsome\n, /** @package */ \nmore: /** @package : dfg */ moreint};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("testInt", "some", "moreint");
		assertThat(googModuleExports.stream().map(e -> e.exportName.externalName)).containsExactly("test", "some", "more");
	}

	@Test
	void extractExportsOfGoogModuleMultipleNamedExportsWithLineComment() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:testInt, \nsome\n, // Some info \nmore: // @also here with colon(:)\n moreint};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("testInt", "some", "moreint");
		assertThat(googModuleExports.stream().map(e -> e.exportName.externalName)).containsExactly("test", "some", "more");
	}
}