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
 * @fileoverview A combo box control that allows user input with
 * auto-suggestion from a limited set of options.
 *
 * @see ../demos/combobox.html
 */

goog.provide('goog.ui.ComboBox');
goog.provide('goog.ui.ComboBoxItem');

goog.require('goog.Timer');
goog.require('goog.asserts');
goog.require('goog.dom');
goog.require('goog.dom.InputType');
goog.require('goog.dom.TagName');
goog.require('goog.dom.classlist');
goog.require('goog.events.EventType');
goog.require('goog.events.InputHandler');
goog.require('goog.events.KeyCodes');
goog.require('goog.events.KeyHandler');
goog.require('goog.log');
goog.require('goog.positioning.Corner');
goog.require('goog.positioning.MenuAnchoredPosition');
goog.require('goog.string');
goog.require('goog.style');
goog.require('goog.ui.Component');
goog.require('goog.ui.ItemEvent');
goog.require('goog.ui.LabelInput');
goog.require('goog.ui.Menu');
goog.require('goog.ui.MenuItem');
goog.require('goog.ui.MenuSeparator');
goog.require('goog.ui.registry');
goog.require('goog.userAgent');

/**
   * A ComboBox control.
   *
   *
   *
   *
   *
   */
goog.ui.ComboBox = class extends goog.ui.Component {
  /**
  * A ComboBox control.
  * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
  * @param {goog.ui.Menu=} opt_menu Optional menu component.
  *     This menu is disposed of by this control.
  * @param {goog.ui.LabelInput=} opt_labelInput Optional label input.
  *     This label input is disposed of by this control.
  *
   */
  constructor(opt_domHelper, opt_menu, opt_labelInput) {
    super(opt_domHelper);

    /**
     * A LabelInput control that manages the focus/blur state of the input box.
     * @type {goog.ui.LabelInput?}
     * @private
     */
    this.labelInput_ = opt_labelInput || new goog.ui.LabelInput();

    /**
     * Whether the combo box is enabled.
     * @type {boolean}
     * @private
     */
    this.enabled_ = true;

    // TODO(user): Allow lazy creation of menus/menu items
    /**
     * Drop down menu for the combo box.  Will be created at construction time.
     * @type {goog.ui.Menu?}
     * @private
     */
    this.menu_ = opt_menu || new goog.ui.Menu(this.getDomHelper());

    this.setupMenu_();
    /**
     * A logger to help debugging of combo box behavior.
     * @type {goog.log.Logger}
     * @private
     */
    this.logger_ = goog.log.getLogger('goog.ui.ComboBox');
    /**
     * Input handler to take care of firing events when the user inputs text in
     * the input.
     * @type {goog.events.InputHandler?}
     * @private
     */
    this.inputHandler_ = null;
    /**
     * The last input token.
     * @type {?string}
     * @private
     */
    this.lastToken_ = null;

    /**
     * The cached visible count.
     * @type {number}
     * @private
     */
    this.visibleCount_ = -1;
    /**
     * The input element.
     * @type {Element}
     * @private
     */
    this.input_ = null;
    /**
     * The match function.  The first argument for the match function will be
     * a MenuItem's caption and the second will be the token to evaluate.
     * @type {Function}
     * @private
     */
    this.matchFunction_ = goog.string.startsWith;
    /**
     * Element used as the combo boxes button.
     * @type {Element}
     * @private
     */
    this.button_ = null;
    /**
     * Default text content for the input box when it is unchanged and unfocussed.
     * @type {string}
     * @private
     */
    this.defaultText_ = '';
    /**
     * Name for the input box created
     * @type {string}
     * @private
     */
    this.fieldName_ = '';
    /**
     * Timer identifier for delaying the dismissal of the combo menu.
     * @type {?number}
     * @private
     */
    this.dismissTimer_ = null;
    /**
     * True if the unicode inverted triangle should be displayed in the dropdown
     * button. Defaults to false.
     * @type {boolean} useDropdownArrow
     * @private
     */
    this.useDropdownArrow_ = false;

    /**
     * Keyboard event handler to manage key events dispatched by the input element.
     * @type {goog.events.KeyHandler}
     * @private
     */
    this.keyHandler_ = null;
  }

  /**
   * Create the DOM objects needed for the combo box.  A span and text input.
   * @override
   */
  createDom() {
    this.input_ = this.getDomHelper().createDom(goog.dom.TagName.INPUT, {
      name: this.fieldName_, type: goog.dom.InputType.TEXT, autocomplete: 'off'
    });
    this.button_ = this.getDomHelper().createDom(goog.dom.TagName.SPAN, goog.getCssName('goog-combobox-button'));
    this.setElementInternal(
        this.getDomHelper().createDom(goog.dom.TagName.SPAN, goog.getCssName('goog-combobox'), this.input_, this.button_));
    if (this.useDropdownArrow_) {
      goog.dom.setTextContent(this.button_, '\u25BC');
      goog.style.setUnselectable(this.button_, true /* unselectable */);
    }
    this.input_.setAttribute('label', this.defaultText_);
    this.labelInput_.decorate(this.input_);
    this.menu_.setFocusable(false);
    if (!this.menu_.isInDocument()) {
      this.addChild(this.menu_, true);
    }
  }

  /**
   * Enables/Disables the combo box.
   * @param {boolean} enabled Whether to enable (true) or disable (false) the
   *     combo box.
   */
  setEnabled(enabled) {
    this.enabled_ = enabled;
    this.labelInput_.setEnabled(enabled);
    goog.dom.classlist.enable(goog.asserts.assert(this.getElement()), goog.getCssName('goog-combobox-disabled'), !enabled);
  }

  /**
   * @return {boolean} Whether the menu item is enabled.
   */
  isEnabled() {
    return this.enabled_;
  }

  /** @override */
  enterDocument() {
    super.enterDocument();

    var handler = this.getHandler();
    handler.listen(this.getElement(), goog.events.EventType.MOUSEDOWN, this.onComboMouseDown_);
    handler.listen(this.getDomHelper().getDocument(), goog.events.EventType.MOUSEDOWN, this.onDocClicked_);

    handler.listen(this.input_, goog.events.EventType.BLUR, this.onInputBlur_);

    this.keyHandler_ = new goog.events.KeyHandler(this.input_);
    handler.listen(this.keyHandler_, goog.events.KeyHandler.EventType.KEY, this.handleKeyEvent);

    this.inputHandler_ = new goog.events.InputHandler(this.input_);
    handler.listen(this.inputHandler_, goog.events.InputHandler.EventType.INPUT, this.onInputEvent_);

    handler.listen(this.menu_, goog.ui.Component.EventType.ACTION, this.onMenuSelected_);
  }

  /** @override */
  exitDocument() {
    this.keyHandler_.dispose();
    delete this.keyHandler_;
    this.inputHandler_.dispose();
    this.inputHandler_ = null;
    super.exitDocument();
  }

  /**
   * Combo box currently can't decorate elements.
   * @return {boolean} The value false.
   * @override
   */
  canDecorate() {
    return false;
  }

  /** @override */
  disposeInternal() {
    super.disposeInternal();

    this.clearDismissTimer_();

    this.labelInput_.dispose();
    this.menu_.dispose();

    this.labelInput_ = null;
    this.menu_ = null;
    this.input_ = null;
    this.button_ = null;
  }

  /**
   * Dismisses the menu and resets the value of the edit field.
   */
  dismiss() {
    this.clearDismissTimer_();
    this.hideMenu_();
    this.menu_.setHighlightedIndex(-1);
  }

  /**
   * Adds a new menu item at the end of the menu.
   * @param {goog.ui.MenuItem} item Menu item to add to the menu.
   */
  addItem(item) {
    this.menu_.addChild(item, true);
    this.visibleCount_ = -1;
  }

  /**
   * Adds a new menu item at a specific index in the menu.
   * @param {goog.ui.MenuItem} item Menu item to add to the menu.
   * @param {number} n Index at which to insert the menu item.
   */
  addItemAt(item, n) {
    this.menu_.addChildAt(item, n, true);
    this.visibleCount_ = -1;
  }

  /**
   * Removes an item from the menu and disposes it.
   * @param {goog.ui.MenuItem} item The menu item to remove.
   */
  removeItem(item) {
    var child = this.menu_.removeChild(item, true);
    if (child) {
      child.dispose();
      this.visibleCount_ = -1;
    }
  }

  /**
   * Remove all of the items from the ComboBox menu
   */
  removeAllItems() {
    for (var i = this.getItemCount() - 1; i >= 0; --i) {
      this.removeItem(this.getItemAt(i));
    }
  }

  /**
   * Removes a menu item at a given index in the menu.
   * @param {number} n Index of item.
   */
  removeItemAt(n) {
    var child = this.menu_.removeChildAt(n, true);
    if (child) {
      child.dispose();
      this.visibleCount_ = -1;
    }
  }

  /**
   * Returns a reference to the menu item at a given index.
   * @param {number} n Index of menu item.
   * @return {goog.ui.MenuItem?} Reference to the menu item.
   */
  getItemAt(n) {
    return /** @type {goog.ui.MenuItem?} */ (this.menu_.getChildAt(n));
  }

  /**
   * Returns the number of items in the list, including non-visible items,
   * such as separators.
   * @return {number} Number of items in the menu for this combobox.
   */
  getItemCount() {
    return this.menu_.getChildCount();
  }

  /**
   * @return {goog.ui.Menu} The menu that pops up.
   */
  getMenu() {
    return this.menu_;
  }

  /**
   * @return {Element} The input element.
   */
  getInputElement() {
    return this.input_;
  }

  /**
   * @return {goog.ui.LabelInput} A LabelInput control that manages the
   *     focus/blur state of the input box.
   */
  getLabelInput() {
    return this.labelInput_;
  }

  /**
   * @return {number} The number of visible items in the menu.
   * @private
   */
  getNumberOfVisibleItems_() {
    if (this.visibleCount_ == -1) {
      var count = 0;
      for (var i = 0, n = this.menu_.getChildCount(); i < n; i++) {
        var item = this.menu_.getChildAt(i);
        if (!(item instanceof goog.ui.MenuSeparator) && item.isVisible()) {
          count++;
        }
      }
      this.visibleCount_ = count;
    }

    return this.visibleCount_;
  }

  /**
   * Sets the match function to be used when filtering the combo box menu.
   * @param {Function} matchFunction The match function to be used when filtering
   *     the combo box menu.
   */
  setMatchFunction(matchFunction) {
    this.matchFunction_ = matchFunction;
  }

  /**
   * @return {Function} The match function for the combox box.
   */
  getMatchFunction() {
    return this.matchFunction_;
  }

  /**
   * Sets the default text for the combo box.
   * @param {string} text The default text for the combo box.
   */
  setDefaultText(text) {
    this.defaultText_ = text;
    if (this.labelInput_) {
      this.labelInput_.setLabel(this.defaultText_);
    }
  }

  /**
   * @return {string} text The default text for the combox box.
   */
  getDefaultText() {
    return this.defaultText_;
  }

  /**
   * Sets the field name for the combo box.
   * @param {string} fieldName The field name for the combo box.
   */
  setFieldName(fieldName) {
    this.fieldName_ = fieldName;
  }

  /**
   * @return {string} The field name for the combo box.
   */
  getFieldName() {
    return this.fieldName_;
  }

  /**
   * Set to true if a unicode inverted triangle should be displayed in the
   * dropdown button.
   * This option defaults to false for backwards compatibility.
   * @param {boolean} useDropdownArrow True to use the dropdown arrow.
   */
  setUseDropdownArrow(useDropdownArrow) {
    this.useDropdownArrow_ = !!useDropdownArrow;
  }

  /**
   * Sets the current value of the combo box.
   * @param {string} value The new value.
   */
  setValue(value) {
    if (this.labelInput_.getValue() != value) {
      this.labelInput_.setValue(value);
      this.handleInputChange_();
    }
  }

  /**
   * @return {string} The current value of the combo box.
   */
  getValue() {
    return this.labelInput_.getValue();
  }

  /**
   * @return {string} HTML escaped token.
   */
  getToken() {
    // TODO(user): Remove HTML escaping and fix the existing calls.
    return goog.string.htmlEscape(this.getTokenText_());
  }

  /**
   * @return {string} The token for the current cursor position in the
   *     input box, when multi-input is disabled it will be the full input value.
   * @private
   */
  getTokenText_() {
    // TODO(user): Implement multi-input such that getToken returns a substring
    // of the whole input delimited by commas.
    return goog.string.trim(this.labelInput_.getValue().toLowerCase());
  }

  /**
   * @private
   */
  setupMenu_() {
    var sm = this.menu_;
    sm.setVisible(false);
    sm.setAllowAutoFocus(false);
    sm.setAllowHighlightDisabled(true);
  }

  /**
   * Shows the menu if it isn't already showing.  Also positions the menu
   * correctly, resets the menu item visibilities and highlights the relevant
   * item.
   * @param {boolean} showAll Whether to show all items, with the first matching
   *     item highlighted.
   * @private
   */
  maybeShowMenu_(showAll) {
    var isVisible = this.menu_.isVisible();
    var numVisibleItems = this.getNumberOfVisibleItems_();

    if (isVisible && numVisibleItems == 0) {
      goog.log.fine(this.logger_, 'no matching items, hiding');
      this.hideMenu_();

    } else if (!isVisible && numVisibleItems > 0) {
      if (showAll) {
        goog.log.fine(this.logger_, 'showing menu');
        this.setItemVisibilityFromToken_('');
        this.setItemHighlightFromToken_(this.getTokenText_());
      }
      // In Safari 2.0, when clicking on the combox box, the blur event is
      // received after the click event that invokes this function. Since we want
      // to cancel the dismissal after the blur event is processed, we have to
      // wait for all event processing to happen.
      goog.Timer.callOnce(this.clearDismissTimer_, 1, this);

      this.showMenu_();
    }

    this.positionMenu();
  }

  /**
   * Positions the menu.
   * @protected
   */
  positionMenu() {
    if (this.menu_ && this.menu_.isVisible()) {
      var position = new goog.positioning.MenuAnchoredPosition(this.getElement(), goog.positioning.Corner.BOTTOM_START, true);
      position.reposition(this.menu_.getElement(), goog.positioning.Corner.TOP_START);
    }
  }

  /**
   * Show the menu and add an active class to the combo box's element.
   * @private
   */
  showMenu_() {
    this.menu_.setVisible(true);
    goog.dom.classlist.add(goog.asserts.assert(this.getElement()), goog.getCssName('goog-combobox-active'));
  }

  /**
   * Hide the menu and remove the active class from the combo box's element.
   * @private
   */
  hideMenu_() {
    this.menu_.setVisible(false);
    goog.dom.classlist.remove(goog.asserts.assert(this.getElement()), goog.getCssName('goog-combobox-active'));
  }

  /**
   * Clears the dismiss timer if it's active.
   * @private
   */
  clearDismissTimer_() {
    if (this.dismissTimer_) {
      goog.Timer.clear(this.dismissTimer_);
      this.dismissTimer_ = null;
    }
  }

  /**
   * Event handler for when the combo box area has been clicked.
   * @param {goog.events.BrowserEvent} e The browser event.
   * @private
   */
  onComboMouseDown_(e) {
    // We only want this event on the element itself or the input or the button.
    if (this.enabled_ && (e.target == this.getElement() || e.target == this.input_ || goog.dom.contains(this.button_, /** @type {Node} */ (e.target)))) {
      if (this.menu_.isVisible()) {
        goog.log.fine(this.logger_, 'Menu is visible, dismissing');
        this.dismiss();
      } else {
        goog.log.fine(this.logger_, 'Opening dropdown');
        this.maybeShowMenu_(true);
        if (goog.userAgent.OPERA) {
          // select() doesn't focus <input> elements in Opera.
          this.input_.focus();
        }
        this.input_.select();
        this.menu_.setMouseButtonPressed(true);
        // Stop the click event from stealing focus
        e.preventDefault();
      }
    }
    // Stop the event from propagating outside of the combo box
    e.stopPropagation();
  }

  /**
   * Event handler for when the document is clicked.
   * @param {goog.events.BrowserEvent} e The browser event.
   * @private
   */
  onDocClicked_(e) {
    if (!goog.dom.contains(this.menu_.getElement(), /** @type {Node} */ (e.target))) {
      this.dismiss();
    }
  }

  /**
   * Handle the menu's select event.
   * @param {goog.events.Event} e The event.
   * @private
   */
  onMenuSelected_(e) {
    var item = /** @type {!goog.ui.MenuItem} */ (e.target);
    // Stop propagation of the original event and redispatch to allow the menu
    // select to be cancelled at this level. i.e. if a menu item should cause
    // some behavior such as a user prompt instead of assigning the caption as
    // the value.
    if (this.dispatchEvent(new goog.ui.ItemEvent(goog.ui.Component.EventType.ACTION, this, item))) {
      var caption = item.getCaption();
      goog.log.fine(this.logger_, 'Menu selection: ' + caption + '. Dismissing menu');
      if (this.labelInput_.getValue() != caption) {
        this.labelInput_.setValue(caption);
        this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
      }
      this.dismiss();
    }
    e.stopPropagation();
  }

  /**
   * Event handler for when the input box looses focus -- hide the menu
   * @param {goog.events.BrowserEvent} e The browser event.
   * @private
   */
  onInputBlur_(e) {
    this.clearDismissTimer_();
    this.dismissTimer_ = goog.Timer.callOnce(this.dismiss, goog.ui.ComboBox.BLUR_DISMISS_TIMER_MS, this);
  }

  /**
   * Handles keyboard events from the input box.  Returns true if the combo box
   * was able to handle the event, false otherwise.
   * @param {goog.events.KeyEvent} e Key event to handle.
   * @return {boolean} Whether the event was handled by the combo box.
   * @protected
   * @suppress {visibility} performActionInternal
   */
  handleKeyEvent(e) {
    var isMenuVisible = this.menu_.isVisible();

    // Give the menu a chance to handle the event.
    if (isMenuVisible && this.menu_.handleKeyEvent(e)) {
      return true;
    }

    // The menu is either hidden or didn't handle the event.
    var handled = false;
    switch (e.keyCode) {
      case goog.events.KeyCodes.ESC:
        // If the menu is visible and the user hit Esc, dismiss the menu.
        if (isMenuVisible) {
          goog.log.fine(this.logger_, 'Dismiss on Esc: ' + this.labelInput_.getValue());
          this.dismiss();
          handled = true;
        }
        break;
      case goog.events.KeyCodes.TAB:
        // If the menu is open and an option is highlighted, activate it.
        if (isMenuVisible) {
          var highlighted = this.menu_.getHighlighted();
          if (highlighted) {
            goog.log.fine(this.logger_, 'Select on Tab: ' + this.labelInput_.getValue());
            highlighted.performActionInternal(e);
            handled = true;
          }
        }
        break;
      case goog.events.KeyCodes.UP:
      case goog.events.KeyCodes.DOWN:
        // If the menu is hidden and the user hit the up/down arrow, show it.
        if (!isMenuVisible) {
          goog.log.fine(this.logger_, 'Up/Down - maybe show menu');
          this.maybeShowMenu_(true);
          handled = true;
        }
        break;
    }

    if (handled) {
      e.preventDefault();
    }

    return handled;
  }

  /**
   * Handles the content of the input box changing.
   * @param {goog.events.Event} e The INPUT event to handle.
   * @private
   */
  onInputEvent_(e) {
    // If the key event is text-modifying, update the menu.
    goog.log.fine(this.logger_, 'Key is modifying: ' + this.labelInput_.getValue());
    this.handleInputChange_();
  }

  /**
   * Handles the content of the input box changing, either because of user
   * interaction or programmatic changes.
   * @private
   */
  handleInputChange_() {
    var token = this.getTokenText_();
    this.setItemVisibilityFromToken_(token);
    if (goog.dom.getActiveElement(this.getDomHelper().getDocument()) == this.input_) {
      // Do not alter menu visibility unless the user focus is currently on the
      // combobox (otherwise programmatic changes may cause the menu to become
      // visible).
      this.maybeShowMenu_(false);
    }
    var highlighted = this.menu_.getHighlighted();
    if (token == '' || !highlighted || !highlighted.isVisible()) {
      this.setItemHighlightFromToken_(token);
    }
    this.lastToken_ = token;
    this.dispatchEvent(goog.ui.Component.EventType.CHANGE);
  }

  /**
   * Loops through all menu items setting their visibility according to a token.
   * @param {string} token The token.
   * @private
   */
  setItemVisibilityFromToken_(token) {
    var isVisibleItem = false;
    var count = 0;
    var recheckHidden = !this.matchFunction_(token, this.lastToken_);

    for (var i = 0, n = this.menu_.getChildCount(); i < n; i++) {
      var item = this.menu_.getChildAt(i);
      if (item instanceof goog.ui.MenuSeparator) {
        // Ensure that separators are only shown if there is at least one visible
        // item before them.
        item.setVisible(isVisibleItem);
        isVisibleItem = false;
      } else if (item instanceof goog.ui.MenuItem) {
        if (!item.isVisible() && !recheckHidden) continue;

        var caption = item.getCaption();
        var visible = this.isItemSticky_(item) || caption && this.matchFunction_(caption.toLowerCase(), token);
        if (typeof item.setFormatFromToken == 'function') {
          item.setFormatFromToken(token);
        }
        item.setVisible(!!visible);
        isVisibleItem = visible || isVisibleItem;

      } else {
        // Assume all other items are correctly using their visibility.
        isVisibleItem = item.isVisible() || isVisibleItem;
      }

      if (!(item instanceof goog.ui.MenuSeparator) && item.isVisible()) {
        count++;
      }
    }

    this.visibleCount_ = count;
  }

  /**
   * Highlights the first token that matches the given token.
   * @param {string} token The token.
   * @private
   */
  setItemHighlightFromToken_(token) {
    if (token == '') {
      this.menu_.setHighlightedIndex(-1);
      return;
    }

    for (var i = 0, n = this.menu_.getChildCount(); i < n; i++) {
      var item = this.menu_.getChildAt(i);
      var caption = item.getCaption();
      if (caption && this.matchFunction_(caption.toLowerCase(), token)) {
        this.menu_.setHighlightedIndex(i);
        item = /** @type {*} */ (item);
        if (item.setFormatFromToken) {
          item.setFormatFromToken(token);
        }
        return;
      }
    }
    this.menu_.setHighlightedIndex(-1);
  }

  /**
   * Returns true if the item has an isSticky method and the method returns true.
   * @param {goog.ui.MenuItem} item The item.
   * @return {boolean} Whether the item has an isSticky method and the method
   *     returns true.
   * @private
   * @suppress {checkTypes}
   */
  isItemSticky_(item) {
    item = /** @type {*} */ (item);
    return typeof item.isSticky == 'function' && item.isSticky();
  }
};

goog.tagUnsealableClass(goog.ui.ComboBox);


/**
 * Number of milliseconds to wait before dismissing combobox after blur.
 * @type {number}
 */
goog.ui.ComboBox.BLUR_DISMISS_TIMER_MS = 250;

/**
 * Class for combo box items.
 */
goog.ui.ComboBoxItem = class extends goog.ui.MenuItem {

  /**
   * Class for combo box items.
   * @param {goog.ui.ControlContent} content Text caption or DOM structure to
   *     display as the content of the item (use to add icons or styling to
   *     menus).
   * @param {*=} opt_data Identifying data for the menu item.
   * @param {goog.dom.DomHelper=} opt_domHelper Optional dom helper used for dom
   *     interactions.
   * @param {goog.ui.MenuItemRenderer=} opt_renderer Optional renderer.
   */
  constructor(content, opt_data, opt_domHelper, opt_renderer) {
    super(content, opt_data, opt_domHelper, opt_renderer);
    /**
     * Whether the menu item is sticky, non-sticky items will be hidden as the
     * user types.
     * @type {boolean}
     * @private
     */
    this.isSticky_ = false;
  }

  /**
   * Sets the menu item to be sticky or not sticky.
   * @param {boolean} sticky Whether the menu item should be sticky.
   */
  setSticky(sticky) {
    this.isSticky_ = sticky;
  }

  /**
   * @return {boolean} Whether the menu item is sticky.
   */
  isSticky() {
    return this.isSticky_;
  }

  /**
   * Sets the format for a menu item based on a token, bolding the token.
   * @param {string} token The token.
   */
  setFormatFromToken(token) {
    if (this.isEnabled()) {
      var caption = this.getCaption();
      var index = caption.toLowerCase().indexOf(token);
      if (index >= 0) {
        var domHelper = this.getDomHelper();
        this.setContent([domHelper.createTextNode(caption.substr(0, index)),
          domHelper.createDom(goog.dom.TagName.B, null, caption.substr(index, token.length)),
          domHelper.createTextNode(caption.substr(index + token.length))]);
      }
    }
  }
};

goog.tagUnsealableClass(goog.ui.ComboBoxItem);


// Register a decorator factory function for goog.ui.ComboBoxItems.
goog.ui.registry.setDecoratorByClassName(
    goog.getCssName('goog-combobox-item'), function() {
      // ComboBoxItem defaults to using MenuItemRenderer.
      return new goog.ui.ComboBoxItem(null);
    });


