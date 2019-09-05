// Copyright 2012 The Closure Library Authors. All Rights Reserved.
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
 * @fileoverview Soy data primitives.
 *
 * The goal is to encompass data types used by Soy, especially to mark content
 * as known to be "safe".
 *
 * @author gboyer@google.com (Garrett Boyer)
 */

goog.provide('goog.soy.data.SanitizedContent');
goog.provide('goog.soy.data.SanitizedContentKind');
goog.provide('goog.soy.data.SanitizedCss');
goog.provide('goog.soy.data.SanitizedHtml');
goog.provide('goog.soy.data.SanitizedHtmlAttribute');
goog.provide('goog.soy.data.SanitizedJs');
goog.provide('goog.soy.data.SanitizedTrustedResourceUri');
goog.provide('goog.soy.data.SanitizedUri');

goog.require('goog.Uri');
goog.require('goog.asserts');
goog.require('goog.html.SafeHtml');
goog.require('goog.html.SafeScript');
goog.require('goog.html.SafeStyle');
goog.require('goog.html.SafeStyleSheet');
goog.require('goog.html.SafeUrl');
goog.require('goog.html.TrustedResourceUrl');
goog.require('goog.html.uncheckedconversions');
goog.require('goog.i18n.bidi.Dir');
goog.require('goog.string.Const');


/**
 * A type of textual content.
 *
 * This is an enum of type Object so that these values are unforgeable.
 *
 * @enum {!Object}
 */
goog.soy.data.SanitizedContentKind = {

  /**
   * A snippet of HTML that does not start or end inside a tag, comment, entity,
   * or DOCTYPE; and that does not contain any executable code
   * (JS, {@code <object>}s, etc.) from a different trust domain.
   */
  HTML: goog.DEBUG ? {sanitizedContentKindHtml: true} : {},

  /**
   * Executable JavaScript code or expression, safe for insertion in a
   * script-tag or event handler context, known to be free of any
   * attacker-controlled scripts. This can either be side-effect-free
   * JavaScript (such as JSON) or JavaScript that's entirely under Google's
   * control.
   */
  JS: goog.DEBUG ? {sanitizedContentJsChars: true} : {},

  /** A properly encoded portion of a URI. */
  URI: goog.DEBUG ? {sanitizedContentUri: true} : {},

  /** A resource URI not under attacker control. */
  TRUSTED_RESOURCE_URI:
      goog.DEBUG ? {sanitizedContentTrustedResourceUri: true} : {},

  /**
   * Repeated attribute names and values. For example,
   * {@code dir="ltr" foo="bar" onclick="trustedFunction()" checked}.
   */
  ATTRIBUTES: goog.DEBUG ? {sanitizedContentHtmlAttribute: true} : {},

  // TODO: Consider separating rules, declarations, and values into
  // separate types, but for simplicity, we'll treat explicitly blessed
  // SanitizedContent as allowed in all of these contexts.
  /**
   * A CSS3 declaration, property, value or group of semicolon separated
   * declarations.
   */
  STYLE: goog.DEBUG ? {sanitizedContentStyle: true} : {},

  /** A CSS3 style sheet (list of rules). */
  CSS: goog.DEBUG ? {sanitizedContentCss: true} : {}

  // TEXT doesn't produce SanitizedContent anymore, use renderText.
};

/**
 * @abstract
   * A string-like object that carries a content-type and a content direction.
   *
   * IMPORTANT! Do not create these directly, nor instantiate the subclasses.
   * Instead, use a trusted, centrally reviewed library as endorsed by your team
   * to generate these objects. Otherwise, you risk accidentally creating
   * SanitizedContent that is attacker-controlled and gets evaluated unescaped in
   * templates.
   *
   *
   */
goog.soy.data.SanitizedContent = class {

  /**
  * A string-like object that carries a content-type and a content direction.
  *
  * IMPORTANT! Do not create these directly, nor instantiate the subclasses.
  * Instead, use a trusted, centrally reviewed library as endorsed by your team
  * to generate these objects. Otherwise, you risk accidentally creating
  * SanitizedContent that is attacker-controlled and gets evaluated unescaped in
  * templates.
   */
  constructor() {
    /**
     * The content's direction; null if unknown and thus to be estimated when
     * necessary.
     * @type {?goog.i18n.bidi.Dir}
     */
    this.contentDir = null;

    /**
     * The context in which this content is safe from XSS attacks.
     * @type {goog.soy.data.SanitizedContentKind}
     */
    this.contentKind;

    /**
     * The already-safe content.
     * @protected {string}
     */
    this.content;
  }

  /**
   * Gets the already-safe content.
   * @return {string}
   */
  getContent() {
    return this.content;
  }

  /** @override */
  toString() {
    return this.content;
  }

  /**
 * Converts sanitized content of kind HTML into SafeHtml
   * @return {!goog.html.SafeHtml}
 * @throws {!Error} when the content kind is not HTML.
   */
  toSafeHtml() {
    if (this.contentKind !== goog.soy.data.SanitizedContentKind.HTML) {
    throw new Error('Sanitized content was not of kind HTML.');
    }
    return goog.html.uncheckedconversions.safeHtmlFromStringKnownToSatisfyTypeContract(
        goog.string.Const.from('Soy SanitizedContent of kind HTML produces ' + 'SafeHtml-contract-compliant value.'),
        this.toString(), this.contentDir);
  }

  /**
   * Converts sanitized content of kind URI into SafeUrl without modification.
   * @return {!goog.html.SafeUrl}
   * @throws {Error} when the content kind is not URI.
   */
  toSafeUrl() {
    if (this.contentKind !== goog.soy.data.SanitizedContentKind.URI) {
      throw new Error('Sanitized content was not of kind URI.');
    }
    return goog.html.uncheckedconversions.safeUrlFromStringKnownToSatisfyTypeContract(goog.string.Const.from(
          'Soy SanitizedContent of kind URI produces ' + 'SafeHtml-contract-compliant value.'), this.toString());
  }
};

/**
 * Content of type {@link goog.soy.data.SanitizedContentKind.HTML}.
 *
 * The content is a string of HTML that can safely be embedded in a PCDATA
 * context in your app.  If you would be surprised to find that an HTML
 * sanitizer produced `s` (e.g.  it runs code or fetches bad URLs) and
 * you wouldn't write a template that produces `s` on security or privacy
 * grounds, then don't pass `s` here. The default content direction is
 * unknown, i.e. to be estimated when necessary.
 *
 */
goog.soy.data.SanitizedHtml = class extends goog.soy.data.SanitizedContent {
    /**
     * Content of type {@link goog.soy.data.SanitizedContentKind.HTML}.
     *
     * The content is a string of HTML that can safely be embedded in a PCDATA
     * context in your app.  If you would be surprised to find that an HTML
     * sanitizer produced `s` (e.g.  it runs code or fetches bad URLs) and
     * you wouldn't write a template that produces `s` on security or privacy
     * grounds, then don't pass `s` here. The default content direction is
     * unknown, i.e. to be estimated when necessary.
     *
     */
  constructor() {
    super();

    /** @override */
    this.contentKind = goog.soy.data.SanitizedContentKind.HTML;
  }


  /**
   * Checks if the value could be used as the Soy type {html}.
   * @param {*} value
   * @return {boolean}
   */
  static isCompatibleWith(value) {
    return goog.isString(value) || value instanceof goog.soy.data.SanitizedHtml || value instanceof goog.html.SafeHtml;}
};


/**
 * Checks if the value could be used as the Soy type {html}.
 * Strict: disallows strings.
 * @param {*} value
 * @return {boolean}
 */
goog.soy.data.SanitizedHtml.isCompatibleWithStrict = function(value) {
  return value instanceof goog.soy.data.SanitizedHtml ||
      value instanceof goog.html.SafeHtml;
};


/**
   * Content of type {@link goog.soy.data.SanitizedContentKind.JS}.
   *
   * The content is JavaScript source that when evaluated does not execute any
   * attacker-controlled scripts. The content direction is LTR.
   */
goog.soy.data.SanitizedJs = class extends goog.soy.data.SanitizedContent {
  /**
  * Content of type {@link goog.soy.data.SanitizedContentKind.JS}.
  *
  * The content is JavaScript source that when evaluated does not execute any
  * attacker-controlled scripts. The content direction is LTR.
   */
  constructor() {
    super();

    /** @override */
    this.contentKind =
    goog.soy.data.SanitizedContentKind.JS;

    /** @override */
    this.contentDir = goog.i18n.bidi.Dir.LTR;
  }


  /**
   * Checks if the value could be used as the Soy type {js}.
   * @param {*} value
   * @return {boolean}
   */
  static isCompatibleWith(value) {
    return goog.isString(value) || value instanceof goog.soy.data.SanitizedJs || value instanceof goog.html.SafeScript;
  }

    /**
     * Checks if the value could be used as the Soy type {js}.
     * Strict: disallows strings.
     * @param {*} value
     * @return {boolean}
     */
    static isCompatibleWithStrict(value) {
        return value instanceof goog.soy.data.SanitizedJs ||
            value instanceof goog.html.SafeHtml;
    }
};


/**
   * Content of type {@link goog.soy.data.SanitizedContentKind.URI}.
   *
   * The content is a URI chunk that the caller knows is safe to emit in a
   * template. The content direction is LTR.
   */
goog.soy.data.SanitizedUri = class extends goog.soy.data.SanitizedContent {
  /**
  * Content of type {@link goog.soy.data.SanitizedContentKind.URI}.
  *
  * The content is a URI chunk that the caller knows is safe to emit in a
  * template. The content direction is LTR.
   */
  constructor() {
    super();
    /** @override */
    this.contentKind =
    goog.soy.data.SanitizedContentKind.URI;

    /** @override */
    this.contentDir = goog.i18n.bidi.Dir.LTR;
  }


  /**
   * Checks if the value could be used as the Soy type {uri}.
   * @param {*} value
   * @return {boolean}
   */
  static isCompatibleWith(value) {
    return goog.isString(value) || value instanceof goog.soy.data.SanitizedUri || value instanceof goog.html.SafeUrl ||
      value instanceof goog.html.TrustedResourceUrl ||
      value instanceof goog.Uri;
};


/**
 * Checks if the value could be used as the Soy type {uri}.
 * Strict: disallows strings.
 * @param {*} value
 * @return {boolean}
 */
goog.soy.data.SanitizedUri.isCompatibleWithStrict = function(value) {
  return value instanceof goog.soy.data.SanitizedUri || value instanceof goog.html.SafeUrl || value instanceof goog.html.TrustedResourceUrl || value instanceof goog.Uri;
  }
};

/**
   * Content of type
   * {@link goog.soy.data.SanitizedContentKind.TRUSTED_RESOURCE_URI}.
   *
   * The content is a TrustedResourceUri chunk that is not under attacker control.
   * The content direction is LTR.
   *
   */
goog.soy.data.SanitizedTrustedResourceUri = class extends goog.soy.data.SanitizedContent {
  /**
  * Content of type
  * {@link goog.soy.data.SanitizedContentKind.TRUSTED_RESOURCE_URI}.
  *
  * The content is a TrustedResourceUri chunk that is not under attacker control.
  * The content direction is LTR.
  *
   */
  constructor() {
    super();

    /** @override */
    this.contentKind =
    goog.soy.data.SanitizedContentKind.TRUSTED_RESOURCE_URI;

    /** @override */
    this.contentDir = goog.i18n.bidi.Dir.LTR;
  }


  /**
   * Converts sanitized content into TrustedResourceUrl without modification.
   * @return {!goog.html.TrustedResourceUrl}
   */
  toTrustedResourceUrl() {
    return goog.html.uncheckedconversions.trustedResourceUrlFromStringKnownToSatisfyTypeContract(goog.string.Const.from(
        'Soy SanitizedContent of kind TRUSTED_RESOURCE_URI produces ' + 'TrustedResourceUrl-contract-compliant value.'),
        this.toString());
  }


  /**
   * Checks if the value could be used as the Soy type {trusted_resource_uri}.
   * @param {*} value
   * @return {boolean}
   */
  static isCompatibleWith(value) {
    return goog.isString(value) || value instanceof goog.soy.data.SanitizedTrustedResourceUri || value instanceof goog.html.TrustedResourceUrl;
};


/**
 * Checks if the value could be used as the Soy type {trusted_resource_uri}.
 * Strict: disallows strings.
 * @param {*} value
 * @return {boolean}
 */
goog.soy.data.SanitizedTrustedResourceUri.isCompatibleWithStrict = function(
    value) {
  return value instanceof goog.soy.data.SanitizedTrustedResourceUri || value instanceof goog.html.TrustedResourceUrl;
  }
};

/**
   * Content of type {@link goog.soy.data.SanitizedContentKind.ATTRIBUTES}.
   *
   * The content should be safely embeddable within an open tag, such as a
   * key="value" pair. The content direction is LTR.
   *
   *
   *
   */
goog.soy.data.SanitizedHtmlAttribute = class extends goog.soy.data.SanitizedContent  {
  /**
  * Content of type {@link goog.soy.data.SanitizedContentKind.ATTRIBUTES}.
  *
  * The content should be safely embeddable within an open tag, such as a
  * key="value" pair. The content direction is LTR.
  *
  *
   */
  constructor() {
    super();

    /** @override */
    this.contentKind =
    goog.soy.data.SanitizedContentKind.ATTRIBUTES;

    /** @override */
    this.contentDir = goog.i18n.bidi.Dir.LTR;
  }


  /**
   * Checks if the value could be used as the Soy type {attribute}.
   * @param {*} value
   * @return {boolean}
   */
  static isCompatibleWith(value) {
  return goog.isString(value) ||
      value instanceof goog.soy.data.SanitizedHtmlAttribute;
  }

    /**
     * Checks if the value could be used as the Soy type {attribute}.
     * Strict: disallows strings.
     * @param {*} value
     * @return {boolean}
     */
    static isCompatibleWithStrict(value) {
        return value instanceof goog.soy.data.SanitizedHtmlAttribute;
    };
};

/**
   * Content of type {@link goog.soy.data.SanitizedContentKind.CSS}.
   *
   * The content is non-attacker-exploitable CSS, such as {@code @import url(x)}.
   * The content direction is LTR.
   *
   */
goog.soy.data.SanitizedCss = class extends goog.soy.data.SanitizedContent {

  /**
  * Content of type {@link goog.soy.data.SanitizedContentKind.CSS}.
  *
  * The content is non-attacker-exploitable CSS, such as {@code @import url(x)}.
  * The content direction is LTR.
  *
   */
  constructor() {
      super();
      /** @override */
      this.contentKind = goog.soy.data.SanitizedContentKind.CSS;
      /** @override */
      this.contentDir = goog.i18n.bidi.Dir.LTR;
  }

/**
 * Checks if the value could be used as the Soy type {css}.
 * @param {*} value
 * @return {boolean}
 */
static isCompatibleWith(value) {
  return goog.isString(value) || value instanceof goog.soy.data.SanitizedCss ||
      value instanceof goog.html.SafeStyle ||
      value instanceof goog.html.SafeStyleSheet;
};


/**
 * Checks if the value could be used as the Soy type {css}.
 * Strict: disallows strings.
 * @param {*} value
 * @return {boolean}
 */
static isCompatibleWithStrict(value) {
  return value instanceof goog.soy.data.SanitizedCss ||
      value instanceof goog.html.SafeStyle ||
      value instanceof goog.html.SafeStyleSheet;
};


  /**
   * Converts SanitizedCss into SafeStyleSheet.
   * Note: SanitizedCss in Soy represents both SafeStyle and SafeStyleSheet in
   * Closure. It's about to be split so that SanitizedCss represents only
   * SafeStyleSheet.
   * @return {!goog.html.SafeStyleSheet}
   */
  toSafeStyleSheet() {
    var value = this.toString();
    goog.asserts.assert(
      /[@{]|^\s*$/.test(value),
      'value doesn\'t look like style sheet: ' + value);
  return goog.html.uncheckedconversions
      .safeStyleSheetFromStringKnownToSatisfyTypeContract(
          goog.string.Const.from(
              'Soy SanitizedCss produces SafeStyleSheet-contract-compliant ' +'value.'), value);
  }

  /**
   * Checks if the value could be used as the Soy type {css}.
   * @param {*} value
   * @return {boolean}
   */
  static isCompatibleWith(value) {
    return goog.isString(value) || value instanceof goog.soy.data.SanitizedCss || value instanceof goog.soy.data.UnsanitizedText || value instanceof goog.html.SafeStyle ||  // TODO(jakubvrana): Delete.
        value instanceof goog.html.SafeStyleSheet;
  }
};

