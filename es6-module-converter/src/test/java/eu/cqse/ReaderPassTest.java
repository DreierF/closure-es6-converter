package eu.cqse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderPassTest {

	@Test
	void extractExportsOfGoogModuleSingleNamedExport() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:test\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("test");
	}

	@Test
	void extractExportsOfGoogModuleMultipleNamedExports() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:test, \nsome:some\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("test", "some");
	}

	@Test
	void extractExportsOfGoogModuleMultipleNamedExportsWithComment() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:testInt, \nsome\n, /** @package */ \nmore: /** @package : dfg */ moreint};");
		assertThat(googModuleExports.stream().map(e -> e.exportName.internalName)).containsExactly("testInt", "some", "moreint");
		assertThat(googModuleExports.stream().map(e -> e.exportName.externalName)).containsExactly("test", "some", "more");
	}
}