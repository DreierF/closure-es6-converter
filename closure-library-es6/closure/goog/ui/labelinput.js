// Copyright 2006 The Closure Library Authors. All Rights Reserved.
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
 * @fileoverview This behavior is applied to a text input and it shows a text
 * message inside the element if the user hasn't entered any text.
 *
 * This uses the HTML5 placeholder attribute where it is supported.
 *
 * This is ported from http://go/labelinput.js
 *
 * Known issue: Safari does not allow you get to the window object from a
 * document. We need that to listen to the onload event. For now we hard code
 * the window to the current window.
 *
 * Known issue: We need to listen to the form submit event but we attach the
 * event only once (when created or when it is changed) so if you move the DOM
 * node to another form it will not be cleared correctly before submitting.
 *
 * @author arv@google.com (Erik Arvidsson)
 * @see ../demos/labelinput.html
 */

goog.provide('goog.ui.LabelInput');

goog.require('goog.Timer');
goog.require('goog.a11y.aria');
goog.require('goog.a11y.aria.State');
goog.require('goog.asserts');
goog.require('goog.dom');
goog.require('goog.dom.InputType');
goog.require('goog.dom.TagName');
goog.require('goog.dom.classlist');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventType');
goog.require('goog.ui.Component');
goog.require('goog.userAgent');

/**
   * This creates the label input object.
   *
   *
   * 
   * 
   */
goog.ui.LabelInput = class extends goog.ui.Component {
  /**
  * This creates the label input object.
  * @param {string=} opt_label The text to show as the label.
  * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
  *
   */
  constructor(opt_label, opt_domHelper) {
    super( opt_domHelper);

    /**
     * The text to show as the label.
     * @type {string}
     * @private
     */
    this.label_ = opt_label || '';
    /**
     * Variable used to store the element value on keydown and restore it on
     * keypress.  See {@link #handleEscapeKeys_}
     * @type {?string}
     * @private
     */
    this.ffKeyRestoreValue_ = null;
    /**
     * The label restore delay after leaving the input.
     * @type {number} Delay for restoring the label.
     * @protected
     */
    this.labelRestoreDelayMs = 10;
    /**
     * @type {boolean}
     * @private
     */
    this.hasFocus_ = false;
    /**
     * The CSS class name to add to the input when the user has not entered a
     * value.
     */
    this.labelCssClassName = goog.getCssName('label-input-label');


    /** @private {boolean} */
    this.inFocusAndSelect_ = false;


    /** @private {boolean} */
    this.formAttached_ = false;

    /**
     * @type {goog.events.EventHandler}
     * @private
     */
    this.eventHandler_ = null;
  }

  /**
   * Creates the DOM nodes needed for the label input.
   * @override
   */
  createDom() {
    this.setElementInternal(this.getDomHelper().createDom(goog.dom.TagName.INPUT, {'type': goog.dom.InputType.TEXT}));
  }

  /**
   * Decorates an existing HTML input element as a label input. If the element
   * has a "label" attribute then that will be used as the label property for the
   * label input object.
   * @param {Element} element The HTML input element to decorate.
   * @override
   */
  decorateInternal(element) {
    super.decorateInternal(element);
    if (!this.label_) {
      this.label_ = element.getAttribute('label') || '';
    }

    // Check if we're attaching to an element that already has focus.
    if (goog.dom.getActiveElement(goog.dom.getOwnerDocument(element)) == element) {
      this.hasFocus_ = true;
      var el = this.getElement();
      goog.asserts.assert(el);
      goog.dom.classlist.remove(el, this.labelCssClassName);
    }

    if (goog.ui.LabelInput.isPlaceholderSupported_()) {
      this.getElement().placeholder = this.label_;
    }
    var labelInputElement = this.getElement();
    goog.asserts.assert(labelInputElement, 'The label input element cannot be null.');
    goog.a11y.aria.setState(labelInputElement, goog.a11y.aria.State.LABEL, this.label_);
  }

  /** @override */
  enterDocument() {
    super.enterDocument();
    this.attachEvents_();
    this.check_();

    // Make it easy for other closure widgets to play nicely with inputs using
    // LabelInput:
    this.getElement().labelInput_ = this;
  }

  /** @override */
  exitDocument() {
    super.exitDocument();
    this.detachEvents_();

    this.getElement().labelInput_ = null;
  }

  /**
   * Attaches the events we need to listen to.
   * @private
   */
  attachEvents_() {
    var eh = new goog.events.EventHandler(this);
    eh.listen(this.getElement(), goog.events.EventType.FOCUS, this.handleFocus_);
    eh.listen(this.getElement(), goog.events.EventType.BLUR, this.handleBlur_);

    if (goog.ui.LabelInput.isPlaceholderSupported_()) {
      this.eventHandler_ = eh;
      return;
    }

    if (goog.userAgent.GECKO) {
      eh.listen(this.getElement(),
          [goog.events.EventType.KEYPRESS, goog.events.EventType.KEYDOWN, goog.events.EventType.KEYUP], this.handleEscapeKeys_);
    }

    // IE sets defaultValue upon load so we need to test that as well.
    var d = goog.dom.getOwnerDocument(this.getElement());
    var w = goog.dom.getWindow(d);
    eh.listen(w, goog.events.EventType.LOAD, this.handleWindowLoad_);

    this.eventHandler_ = eh;
    this.attachEventsToForm_();
  }

  /**
   * Adds a listener to the form so that we can clear the input before it is
   * submitted.
   * @private
   */
  attachEventsToForm_() {
    // in case we have are in a form we need to make sure the label is not
    // submitted
    if (!this.formAttached_ && this.eventHandler_ && this.getElement().form) {
      this.eventHandler_.listen(this.getElement().form, goog.events.EventType.SUBMIT, this.handleFormSubmit_);
      this.formAttached_ = true;
    }
  }

  /**
   * Stops listening to the events.
   * @private
   */
  detachEvents_() {
    if (this.eventHandler_) {
      this.eventHandler_.dispose();
      this.eventHandler_ = null;
    }
  }

  /** @override */
  disposeInternal() {
    super.disposeInternal();
    this.detachEvents_();
  }

  /**
   * Handler for the focus event.
   * @param {goog.events.Event} e The event object passed in to the event handler.
   * @private
   */
  handleFocus_(e) {
    this.hasFocus_ = true;
    var el = this.getElement();
    goog.asserts.assert(el);
    goog.dom.classlist.remove(el, this.labelCssClassName);
    if (goog.ui.LabelInput.isPlaceholderSupported_()) {
      return;
    }
    if (!this.hasChanged() && !this.inFocusAndSelect_) {
      var me = this;
      var clearValue = function() {
        // Component could be disposed by the time this is called.
        if (me.getElement()) {
          me.getElement().value = '';
        }
      };
      if (goog.userAgent.IE) {
        goog.Timer.callOnce(clearValue, 10);
      } else {
        clearValue();
      }
    }
  }

  /**
   * Handler for the blur event.
   * @param {goog.events.Event} e The event object passed in to the event handler.
   * @private
   */
  handleBlur_(e) {
    // We listen to the click event when we enter focusAndSelect mode so we can
    // fake an artificial focus when the user clicks on the input box. However,
    // if the user clicks on something else (and we lose focus), there is no
    // need for an artificial focus event.
    if (!goog.ui.LabelInput.isPlaceholderSupported_()) {
      this.eventHandler_.unlisten(this.getElement(), goog.events.EventType.CLICK, this.handleFocus_);
      this.ffKeyRestoreValue_ = null;
    }
    this.hasFocus_ = false;
    this.check_();
  }

  /**
   * Handler for key events in Firefox.
   *
   * If the escape key is pressed when a text input has not been changed manually
   * since being focused, the text input will revert to its previous value.
   * Firefox does not honor preventDefault for the escape key. The revert happens
   * after the keydown event and before every keypress. We therefore store the
   * element's value on keydown and restore it on keypress. The restore value is
   * nullified on keyup so that {@link #getValue} returns the correct value.
   *
   * IE and Chrome don't have this problem, Opera blurs in the input box
   * completely in a way that preventDefault on the escape key has no effect.
   *
   * @param {goog.events.BrowserEvent} e The event object passed in to
   *     the event handler.
   * @private
   */
  handleEscapeKeys_(e) {
    if (e.keyCode == 27) {
      if (e.type == goog.events.EventType.KEYDOWN) {
        this.ffKeyRestoreValue_ = this.getElement().value;
      } else if (e.type == goog.events.EventType.KEYPRESS) {
        this.getElement().value = /** @type {string} */ (this.ffKeyRestoreValue_);
      } else if (e.type == goog.events.EventType.KEYUP) {
        this.ffKeyRestoreValue_ = null;
      }
      e.preventDefault();
    }
  }

  /**
   * Handler for the submit event of the form element.
   * @param {goog.events.Event} e The event object passed in to the event handler.
   * @private
   */
  handleFormSubmit_(e) {
    if (!this.hasChanged()) {
      this.getElement().value = '';
      // allow form to be sent before restoring value
      goog.Timer.callOnce(this.handleAfterSubmit_, 10, this);
    }
  }

  /**
   * Restore value after submit
   * @private
   */
  handleAfterSubmit_() {
    if (!this.hasChanged()) {
      this.getElement().value = this.label_;
    }
  }

  /**
   * Handler for the load event the window. This is needed because
   * IE sets defaultValue upon load.
   * @param {Event} e The event object passed in to the event handler.
   * @private
   */
  handleWindowLoad_(e) {
    this.check_();
  }

  /**
   * @return {boolean} Whether the control is currently focused on.
   */
  hasFocus() {
    return this.hasFocus_;
  }

  /**
   * @return {boolean} Whether the value has been changed by the user.
   */
  hasChanged() {
    return !!this.getElement() && this.getElement().value != '' && this.getElement().value != this.label_;
  }

  /**
   * Clears the value of the input element without resetting the default text.
   */
  clear() {
    this.getElement().value = '';

    // Reset ffKeyRestoreValue_ when non-null
    if (this.ffKeyRestoreValue_ != null) {
      this.ffKeyRestoreValue_ = '';
    }
  }

  /**
   * Clears the value of the input element and resets the default text.
   */
  reset() {
    if (this.hasChanged()) {
      this.clear();
      this.check_();
    }
  }

  /**
   * Use this to set the value through script to ensure that the label state is
   * up to date
   * @param {string} s The new value for the input.
   */
  setValue(s) {
    if (this.ffKeyRestoreValue_ != null) {
      this.ffKeyRestoreValue_ = s;
    }
    this.getElement().value = s;
    this.check_();
  }

  /**
   * Returns the current value of the text box, returning an empty string if the
   * search box is the default value
   * @return {string} The value of the input box.
   */
  getValue() {
    if (this.ffKeyRestoreValue_ != null) {
      // Fix the Firefox from incorrectly reporting the value to calling code
      // that attached the listener to keypress before the labelinput
      return this.ffKeyRestoreValue_;
    }
    return this.hasChanged() ? /** @type {string} */ (this.getElement().value) : '';
  }

  /**
   * Sets the label text as aria-label, and placeholder when supported.
   * @param {string} label The text to show as the label.
   */
  setLabel(label) {
    var labelInputElement = this.getElement();

    if (goog.ui.LabelInput.isPlaceholderSupported_()) {
      if (labelInputElement) {
        labelInputElement.placeholder = label;
      }
      this.label_ = label;
    } else if (!this.hasChanged()) {
      // The this.hasChanged() call relies on non-placeholder behavior checking
      // prior to setting this.label_ - it also needs to happen prior to the
      // this.restoreLabel_() call.
      if (labelInputElement) {
        labelInputElement.value = '';
      }
      this.label_ = label;
      this.restoreLabel_();
    }
    // Check if this has been called before DOM structure building
    if (labelInputElement) {
      goog.a11y.aria.setState(labelInputElement, goog.a11y.aria.State.LABEL, this.label_);
    }
  }

  /**
   * @return {string} The text to show as the label.
   */
  getLabel() {
    return this.label_;
  }

  /**
   * Checks the state of the input element
   * @private
   */
  check_() {
    var labelInputElement = this.getElement();
    goog.asserts.assert(labelInputElement, 'The label input element cannot be null.');
    if (!goog.ui.LabelInput.isPlaceholderSupported_()) {
      // if we haven't got a form yet try now
      this.attachEventsToForm_();
    } else if (this.getElement().placeholder != this.label_) {
      this.getElement().placeholder = this.label_;
    }
    goog.a11y.aria.setState(labelInputElement, goog.a11y.aria.State.LABEL, this.label_);

    if (!this.hasChanged()) {
      if (!this.inFocusAndSelect_ && !this.hasFocus_) {
        var el = this.getElement();
        goog.asserts.assert(el);
        goog.dom.classlist.add(el, this.labelCssClassName);
      }

      // Allow browser to catchup with CSS changes before restoring the label.
      if (!goog.ui.LabelInput.isPlaceholderSupported_()) {
        goog.Timer.callOnce(this.restoreLabel_, this.labelRestoreDelayMs, this);
      }
    } else {
      var el = this.getElement();
      goog.asserts.assert(el);
      goog.dom.classlist.remove(el, this.labelCssClassName);
    }
  }

  /**
   * This method focuses the input and selects all the text. If the value hasn't
   * changed it will set the value to the label so that the label text is
   * selected.
   */
  focusAndSelect() {
    // We need to check whether the input has changed before focusing
    var hc = this.hasChanged();
    this.inFocusAndSelect_ = true;
    this.getElement().focus();
    if (!hc && !goog.ui.LabelInput.isPlaceholderSupported_()) {
      this.getElement().value = this.label_;
    }
    this.getElement().select();

    // Since the object now has focus, we won't get a focus event when they
    // click in the input element. The expected behavior when you click on
    // the default text is that it goes away and allows you to type...so we
    // have to fire an artificial focus event when we're in focusAndSelect mode.
    if (goog.ui.LabelInput.isPlaceholderSupported_()) {
      return;
    }
    if (this.eventHandler_) {
      this.eventHandler_.listenOnce(this.getElement(), goog.events.EventType.CLICK, this.handleFocus_);
    }

    // set to false in timer to let IE trigger the focus event
    goog.Timer.callOnce(this.focusAndSelect_, 10, this);
  }

  /**
   * Enables/Disables the label input.
   * @param {boolean} enabled Whether to enable (true) or disable (false) the
   *     label input.
   */
  setEnabled(enabled) {
    this.getElement().disabled = !enabled;
    var el = this.getElement();
    goog.asserts.assert(el);
    goog.dom.classlist.enable(el, goog.getCssName(this.labelCssClassName, 'disabled'), !enabled);
  }

  /**
   * @return {boolean} True if the label input is enabled, false otherwise.
   */
  isEnabled() {
    return !this.getElement().disabled;
  }

  /**
   * @private
   */
  focusAndSelect_() {
    this.inFocusAndSelect_ = false;
  }

  /**
   * Sets the value of the input element to label.
   * @private
   */
  restoreLabel_() {
    // Check again in case something changed since this was scheduled.
    // We check that the element is still there since this is called by a timer
    // and the dispose method may have been called prior to this.
    if (this.getElement() && !this.hasChanged() && !this.hasFocus_) {
      this.getElement().value = this.label_;
    }
  }

  /**
   * Checks browser support for placeholder attribute.
   * @return {boolean} Whether placeholder attribute is supported.
   * @private
   */
  static isPlaceholderSupported_() {
    if (!goog.isDefAndNotNull(goog.ui.LabelInput.supportsPlaceholder_)) {
      goog.ui.LabelInput.supportsPlaceholder_ = ('placeholder' in goog.dom.createElement(goog.dom.TagName.INPUT));
    }
    return goog.ui.LabelInput.supportsPlaceholder_;
  }
};

goog.tagUnsealableClass(goog.ui.LabelInput);

/**
 * Indicates whether the browser supports the placeholder attribute, new in
 * HTML5.
 * @type {?boolean}
 * @private
 */
goog.ui.LabelInput.supportsPlaceholder_;



