package eu.cqse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConvertingPassTest {

	@Test
	void fixGoogDefineKeywords() {
		assertThat(ConvertingPass.fixGoogDefineKeywords("goog.define('goog.userAgent.product.ASSUME_SAFARI', false);\n some(goog.userAgent.product.ASSUME_SAFARI)", globalExports)).isEqualTo("const ASSUME_SAFARI = goog.define('goog.userAgent.product.ASSUME_SAFARI', false);\n some(ASSUME_SAFARI)");
	}
}