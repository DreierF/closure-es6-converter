// Copyright 2010 The Closure Library Authors. All Rights Reserved.

// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Native browser textarea renderer for {@link goog.ui.Textarea}s.
 */

goog.provide('goog.ui.TextareaRenderer');

goog.require('goog.dom.TagName');
goog.require('goog.ui.Component');
goog.require('goog.ui.ControlRenderer');

/**
   * Renderer for {@link goog.ui.Textarea}s.  Renders and decorates native HTML
   * textarea elements.  Since native HTML textareas have built-in support for
   * many features, overrides many expensive (and redundant) superclass methods to
   * be no-ops.
   *
   */
goog.ui.TextareaRenderer = class extends goog.ui.ControlRenderer {
  /**
  * Renderer for {@link goog.ui.Textarea}s.  Renders and decorates native HTML
  * textarea elements.  Since native HTML textareas have built-in support for
  * many features, overrides many expensive (and redundant) superclass methods to
  * be no-ops.
  *
   */
  constructor() {
    super();
    /**
     * Textareas natively support right-to-left rendering.
     * @override
     */
    this.setRightToLeft = goog.nullFunction;
    /**
     * Textareas natively support keyboard focus.
     * @override
     */
    this.setFocusable = goog.nullFunction;
    /**
     * Textareas don't need ARIA states to support accessibility, so this is
     * a no-op.
     * @override
     */
    this.updateAriaState = goog.nullFunction;
  }

  /** @override */
  getAriaRole() {
    // textareas don't need ARIA roles to be recognized by screen readers.
    return undefined;
  }

  /** @override */
  decorate(control, element) {
    this.setUpTextarea_(control);
    super.decorate(control, element);
    control.setContent(element.value);
    return element;
  }

  /**
   * Returns the textarea's contents wrapped in an HTML textarea element.  Sets
   * the textarea's disabled attribute as needed.
   * @param {goog.ui.Control} textarea Textarea to render.
   * @return {!Element} Root element for the Textarea control (an HTML textarea
   *     element).
   * @override
   */
  createDom(textarea) {
    this.setUpTextarea_(textarea);
    var element = textarea.getDomHelper().createDom(goog.dom.TagName.TEXTAREA, {
      'class': this.getClassNames(textarea).join(' '), 'disabled': !textarea.isEnabled()
    }, textarea.getContent() || '');
    return element;
  }

  /**
   * Overrides {@link goog.ui.TextareaRenderer#canDecorate} by returning true only
   * if the element is an HTML textarea.
   * @param {Element} element Element to decorate.
   * @return {boolean} Whether the renderer can decorate the element.
   * @override
   */
  canDecorate(element) {
    return element.tagName == goog.dom.TagName.TEXTAREA;
  }

  /**
   * Textareas are always focusable as long as they are enabled.
   * @override
   */
  isFocusable(textarea) {
    return textarea.isEnabled();
  }

  /**
   * Textareas also expose the DISABLED state in the HTML textarea's
   * `disabled` attribute.
   * @override
   */
  setState(textarea, state, enable) {
    super.setState(textarea, state, enable);
    var element = textarea.getElement();
    if (element && state == goog.ui.Component.State.DISABLED) {
      element.disabled = enable;
    }
  }

  /**
   * Sets up the textarea control such that it doesn't waste time adding
   * functionality that is already natively supported by browser
   * textareas.
   * @param {goog.ui.Control} textarea Textarea control to configure.
   * @private
   */
  setUpTextarea_(textarea) {
    textarea.setHandleMouseEvents(false);
    textarea.setAutoStates(goog.ui.Component.State.ALL, false);
    textarea.setSupportedState(goog.ui.Component.State.FOCUSED, false);
  }

  /** @override **/
  setContent(element, value) {
    if (element) {
      element.value = value;
    }
  }

  /** @override **/
  getCssClass() {
    return goog.ui.TextareaRenderer.CSS_CLASS;
  }
};

goog.addSingletonGetter(goog.ui.TextareaRenderer);


/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 * @override
 */
goog.ui.TextareaRenderer.CSS_CLASS = goog.getCssName('goog-textarea');


