package eu.cqse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReaderPassTest {

	@Test
	void extractExportsOfGoogModule() {
		List<GoogModuleExport> googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:test\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName)).containsExactly("test");
		googModuleExports = ReaderPass.extractExportsOfGoogModule("exports = {\ntest:test, \nsome:some\n};");
		assertThat(googModuleExports.stream().map(e -> e.exportName)).containsExactly("test", "some");
	}
}