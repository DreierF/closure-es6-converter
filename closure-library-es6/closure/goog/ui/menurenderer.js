// Copyright 2008 The Closure Library Authors. All Rights Reserved.
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
 * @fileoverview Renderer for {@link goog.ui.Menu}s.
 *
 * @author robbyw@google.com (Robby Walker)
 */

goog.provide('goog.ui.MenuRenderer');

goog.require('goog.a11y.aria');
goog.require('goog.a11y.aria.Role');
goog.require('goog.a11y.aria.State');
goog.require('goog.asserts');
goog.require('goog.dom');
goog.require('goog.dom.TagName');
goog.require('goog.ui.ContainerRenderer');
goog.require('goog.ui.Separator');

goog.forwardDeclare('goog.ui.Menu');

/**
   * Default renderer for {@link goog.ui.Menu}s, based on {@link
      * goog.ui.ContainerRenderer}.
   *
   * 
   * 
   */
goog.ui.MenuRenderer = class extends goog.ui.ContainerRenderer {
  /**
  * Default renderer for {@link goog.ui.Menu}s, based on {@link
  * goog.ui.ContainerRenderer}.
  * @param {string=} opt_ariaRole Optional ARIA role used for the element.
  *
   */
  constructor(opt_ariaRole) {
    super( opt_ariaRole || goog.a11y.aria.Role.MENU);
  }

  /**
   * Returns whether the element is a UL or acceptable to our superclass.
   * @param {Element} element Element to decorate.
   * @return {boolean} Whether the renderer can decorate the element.
   * @override
   */
  canDecorate(element) {
    return element.tagName == goog.dom.TagName.UL || super.canDecorate(element);
  }

  /**
   * Inspects the element, and creates an instance of {@link goog.ui.Control} or
   * an appropriate subclass best suited to decorate it.  Overrides the superclass
   * implementation by recognizing HR elements as separators.
   * @param {Element} element Element to decorate.
   * @return {goog.ui.Control?} A new control suitable to decorate the element
   *     (null if none).
   * @override
   */
  getDecoratorForChild(element) {
    return element.tagName == goog.dom.TagName.HR ? new goog.ui.Separator() : super.getDecoratorForChild(element);
  }

  /**
   * Returns whether the given element is contained in the menu's DOM.
   * @param {goog.ui.Menu} menu The menu to test.
   * @param {Element} element The element to test.
   * @return {boolean} Whether the given element is contained in the menu.
   */
  containsElement(menu, element) {
    return goog.dom.contains(menu.getElement(), element);
  }

  /**
   * Returns the CSS class to be applied to the root element of containers
   * rendered using this renderer.
   * @return {string} Renderer-specific CSS class.
   * @override
   */
  getCssClass() {
    return goog.ui.MenuRenderer.CSS_CLASS;
  }

  /** @override */
  initializeDom(container) {
    super.initializeDom(container);

    var element = container.getElement();
    goog.asserts.assert(element, 'The menu DOM element cannot be null.');
    goog.a11y.aria.setState(element, goog.a11y.aria.State.HASPOPUP, 'true');
  }
};

goog.addSingletonGetter(goog.ui.MenuRenderer);


/**
 * Default CSS class to be applied to the root element of toolbars rendered
 * by this renderer.
 * @type {string}
 * @override
 */
goog.ui.MenuRenderer.CSS_CLASS = goog.getCssName('goog-menu');


