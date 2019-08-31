package eu.cqse;

import java.util.List;

class GoogProvideOrModule {
	final boolean isModule;
	final String namespace;
	final List<String> exports;
	final String fullMatch;

	GoogProvideOrModule(String namespace, boolean isModule, List<String> exports, String fullMatch) {
		this.isModule = isModule;
		this.namespace = namespace;
		this.exports = exports;
		this.fullMatch = fullMatch;
	}
}
