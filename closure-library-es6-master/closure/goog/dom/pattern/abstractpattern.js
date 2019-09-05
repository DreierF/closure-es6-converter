// Copyright 2007 The Closure Library Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview DOM pattern base class.
 *
 * @author robbyw@google.com (Robby Walker)
 */

goog.module('goog.dom.pattern.AbstractPattern');
goog.module.declareLegacyNamespace();

const TagWalkType = goog.require('goog.dom.TagWalkType');
const MatchType = goog.require('goog.dom.pattern.MatchType');

/**
 * Base pattern class for DOM matching.
 */
class AbstractPattern {
	/**
	 * Base pattern class for DOM matching.
	 */
	constructor() {
		/**
		 * The first node matched by this pattern.
		 * @type {Node}
		 */
		this.matchedNode = null;
	}

	/**
	 * Reset any internal state this pattern keeps.
	 */
	reset() {
		// The base implementation does nothing.
	}

	/**
	 * Test whether this pattern matches the given token.
	 *
	 * @param {Node} token Token to match against.
	 * @param {TagWalkType} type The type of token.
	 * @return {MatchType} `MATCH` if the pattern matches.
	 */
	matchToken(token, type) {
		return MatchType.NO_MATCH;
	}
}

exports = AbstractPattern;