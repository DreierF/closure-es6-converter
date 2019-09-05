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
 * @fileoverview A menu class for showing popups.  A single popup can be
 * attached to multiple anchor points.  The menu will try to reposition itself
 * if it goes outside the viewport.
 *
 * Decoration is the same as goog.ui.Menu except that the outer DIV can have a
 * 'for' property, which is the ID of the element which triggers the popup.
 *
 * Decorate Example:
 * <button id="dButton">Decorated Popup</button>
 * <div id="dMenu" for="dButton" class="goog-menu">
 *   <div class="goog-menuitem">A a</div>
 *   <div class="goog-menuitem">B b</div>
 *   <div class="goog-menuitem">C c</div>
 *   <div class="goog-menuitem">D d</div>
 *   <div class="goog-menuitem">E e</div>
 *   <div class="goog-menuitem">F f</div>
 * </div>
 *
 * TESTED=FireFox 2.0, IE6, Opera 9, Chrome.
 * TODO(user): Key handling is flakey in Opera and Chrome
 *
 * @see ../demos/popupmenu.html
 */

goog.provide('goog.ui.PopupMenu');

goog.require('goog.events');
goog.require('goog.events.BrowserEvent');
goog.require('goog.events.BrowserEvent.MouseButton');
goog.require('goog.events.EventType');
goog.require('goog.events.KeyCodes');
goog.require('goog.positioning.AnchoredViewportPosition');
goog.require('goog.positioning.Corner');
goog.require('goog.positioning.MenuAnchoredPosition');
goog.require('goog.positioning.Overflow');
goog.require('goog.positioning.ViewportClientPosition');
goog.require('goog.structs.Map');
goog.require('goog.style');
goog.require('goog.ui.Component');
goog.require('goog.ui.Menu');
goog.require('goog.ui.PopupBase');

/**
   * A basic menu class.
   *
   *
   ; defaults to {@link goog.ui.MenuRenderer}.
   *
   *
   */
goog.ui.PopupMenu = class extends goog.ui.Menu {
  /**
  * A basic menu class.
  * @param {?goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
  * @param {?goog.ui.MenuRenderer=} opt_renderer Renderer used to render or
  *     decorate the container; defaults to {@link goog.ui.MenuRenderer}.
  *
   */
  constructor(opt_domHelper, opt_renderer) {
    super( opt_domHelper, opt_renderer);

    this.setAllowAutoFocus(true);

    // Popup menus are hidden by default.
    this.setVisible(false, true);

    /**
     * Map of attachment points for the menu.  Key -> Object
     * @type {!goog.structs.Map}
     * @private
     */
    this.targets_ = new goog.structs.Map();
    /**
     * If true, then if the menu will toggle off if it is already visible.
     * @type {boolean}
     * @private
     */
    this.toggleMode_ = false;/**
 * If true, then the browser context menu will override the menu activation when
 * the shift key is held down.
 * @type {boolean}
 * @private
 */
goog.ui.PopupMenu.prototype.shiftOverride_ = false;

    /**
     * Time that the menu was last shown.
     * @type {number}
     * @private
     */
    this.lastHide_ = 0;
    /**
     * Current element where the popup menu is anchored.
     * @type {?Element}
     * @private
     */
    this.currentAnchor_ = null;
  }

  /**
   * Decorate an existing HTML structure with the menu. Menu items will be
   * constructed from elements with classname 'goog-menuitem', separators will be
   * made from HR elements.
   * @param {?Element} element Element to decorate.
   * @override
 * @suppress {strictMissingProperties} Part of the go/strict_warnings_migration
   */
  decorateInternal(element) {
    super.decorateInternal(element);
    // 'for' is a custom attribute for attaching the menu to a click target
    var htmlFor = element.getAttribute('for') || element.htmlFor;
    if (htmlFor) {
      this.attach(this.getDomHelper().getElement(htmlFor), goog.positioning.Corner.BOTTOM_LEFT);
    }
  }

  /** @override */
  enterDocument() {
    super.enterDocument();

    this.targets_.forEach(this.attachEvent_, this);

    var handler = this.getHandler();
    handler.listen(this, goog.ui.Component.EventType.ACTION, this.onAction_);
    handler.listen(this.getDomHelper().getDocument(), goog.events.EventType.MOUSEDOWN, this.onDocClick, true);
  }

  /**
   * Attaches the menu to a new popup position and anchor element.  A menu can
   * only be attached to an element once, since attaching the same menu for
   * multiple positions doesn't make sense.
   *
   * @param {?Element} element Element whose click event should trigger the menu.
   * @param {?goog.positioning.Corner=} opt_targetCorner Corner of the target that
   *     the menu should be anchored to.
   * @param {goog.positioning.Corner=} opt_menuCorner Corner of the menu that
   *     should be anchored.
   * @param {boolean=} opt_contextMenu Whether the menu should show on
   *     {@link goog.events.EventType.CONTEXTMENU} events, false if it should
   *     show on {@link goog.events.EventType.MOUSEDOWN} events. Default is
   *     MOUSEDOWN.
   * @param {?goog.math.Box=} opt_margin Margin for the popup used in positioning
   *     algorithms.
   */
  attach(element, opt_targetCorner, opt_menuCorner, opt_contextMenu, opt_margin) {

    if (this.isAttachTarget(element)) {
      // Already in the popup, so just return.
      return;
    }

    var target = this.createAttachTarget(element, opt_targetCorner, opt_menuCorner, opt_contextMenu, opt_margin);

    if (this.isInDocument()) {
      this.attachEvent_(target);
    }

    // Add a listener for keyboard actions on the menu.
    var handler = goog.partial(this.onMenuKeyboardAction_, element);
    if (this.getElement()) {
      this.getHandler().listen(this.getElement(), goog.events.EventType.KEYDOWN, handler);
    }
  }

  /**
   * Handles keyboard actions on the PopupMenu, according to
   * http://www.w3.org/WAI/PF/aria-practices/#menubutton.
   *
   * <p>If the ESC key is pressed, the menu is hidden (which is handled by
   * this.onAction_), and the focus is returned to the element whose click event
   * triggered opening of the menu.
   *
   * <p>If the SPACE or ENTER keys are pressed, the highlighted menu item's
   * listeners are fired.
   *
   * @param {Element} element Element whose click event triggered the menu.
   * @param {!goog.events.BrowserEvent} e The key down event.
   * @private
   */
  onMenuKeyboardAction_(element, e) {
    if (e.keyCode == goog.events.KeyCodes.ESC) {
      element.focus();
      return;
    }
    var highlightedItem = this.getChildAt(this.getHighlightedIndex());
    if (!highlightedItem) {
      return;
    }
    var targetElement = highlightedItem.getElement();
    // Create an event to pass to the menu item's listener.
    var event = new goog.events.BrowserEvent(e.getBrowserEvent(), targetElement);
    event.target = targetElement;
    // If an item is highlighted and the user presses the SPACE/ENTER key, the
    // event target is the menu rather than the menu item, so we manually fire
    // the listener of the correct menu item.
    if (e.keyCode == goog.events.KeyCodes.SPACE || e.keyCode == goog.events.KeyCodes.ENTER) {
      goog.events.fireListeners(targetElement, goog.events.EventType.KEYDOWN, false, event);
    }
    // After activating a menu item the PopupMenu should be hidden (already
    // implemented in this.onAction_ for ENTER/MOUSEDOWN).
    if (e.keyCode == goog.events.KeyCodes.SPACE) {
      this.hide();
    }
  }

  /**
   * Creates an object describing how the popup menu should be attached to the
   * anchoring element based on the given parameters. The created object is
   * stored, keyed by `element` and is retrievable later by invoking
   * {@link #getAttachTarget(element)} at a later point.
   *
   * Subclass may add more properties to the returned object, as needed.
   *
   * @param {?Element} element Element whose click event should trigger the menu.
   * @param {?goog.positioning.Corner=} opt_targetCorner Corner of the target that
   *     the menu should be anchored to.
   * @param {?goog.positioning.Corner=} opt_menuCorner Corner of the menu that
   *     should be anchored.
   * @param {boolean=} opt_contextMenu Whether the menu should show on
   *     {@link goog.events.EventType.CONTEXTMENU} events, false if it should
   *     show on {@link goog.events.EventType.MOUSEDOWN} events. Default is
   *     MOUSEDOWN.
   * @param {?goog.math.Box=} opt_margin Margin for the popup used in positioning
   *     algorithms.
   *
   * @return {?Object} An object that describes how the popup menu should be
   *     attached to the anchoring element.
   *
   * @protected
   */
  createAttachTarget(element, opt_targetCorner, opt_menuCorner, opt_contextMenu, opt_margin) {
    if (!element) {
      return null;
    }

    var target = {
      element_: element,
      targetCorner_: opt_targetCorner,
      menuCorner_: opt_menuCorner,
      eventType_: opt_contextMenu ? goog.events.EventType.CONTEXTMENU : goog.events.EventType.MOUSEDOWN,
      margin_: opt_margin
    };

    this.targets_.set(goog.getUid(element), target);

    return target;
  }

  /**
   * Returns the object describing how the popup menu should be attach to given
   * element or `null`. The object is created and the association is formed
   * when {@link #attach} is invoked.
   *
   * @param {?Element} element DOM element.
   * @return {?Object} The object created when {@link attach} is invoked on
   *     `element`. Returns `null` if the element does not trigger
   *     the menu (i.e. {@link attach} has never been invoked on
   *     `element`).
   * @protected
   */
  getAttachTarget(element) {
    return element ? /** @type {?Object} */ (this.targets_.get(goog.getUid(element))) : null;
  }

  /**
   * @param {?Element} element Any DOM element.
   * @return {boolean} Whether clicking on the given element will trigger the
   *     menu.
   *
   * @protected
   */
  isAttachTarget(element) {
    return element ? this.targets_.containsKey(goog.getUid(element)) : false;
  }

  /**
   * @return {?Element} The current element where the popup is anchored, if it's
   *     visible.
   */
  getAttachedElement() {
    return this.currentAnchor_;
  }

  /**
   * Attaches two event listeners to a target. One with corresponding event type,
   * and one with the KEYDOWN event type for accessibility purposes.
   * @param {?Object} target The target to attach an event to.
   * @private
 * @suppress {strictMissingProperties} Part of the go/strict_warnings_migration
   */
  attachEvent_(target) {
    this.getHandler().listen(target.element_, target.eventType_, this.onTargetClick_);
    if (target.eventType_ != goog.events.EventType.CONTEXTMENU) {
      this.getHandler().listen(target.element_, goog.events.EventType.KEYDOWN, this.onTargetKeyboardAction_);
    }
  }

  /**
   * Detaches all listeners
   */
  detachAll() {
    if (this.isInDocument()) {
      var keys = this.targets_.getKeys();
      for (var i = 0; i < keys.length; i++) {
        this.detachEvent_(/** @type {!Object} */ (this.targets_.get(keys[i])));
      }
    }

    this.targets_.clear();
  }

  /**
   * Detaches a menu from a given element.
   * @param {?Element} element Element whose click event should trigger the menu.
   */
  detach(element) {
    if (!this.isAttachTarget(element)) {
      throw new Error('Menu not attached to provided element, unable to detach.');
    }

    var key = goog.getUid(element);
    if (this.isInDocument()) {
      this.detachEvent_(/** @type {!Object} */ (this.targets_.get(key)));
    }

    this.targets_.remove(key);
  }

  /**
   * Detaches an event listener to a target
   * @param {!Object} target The target to detach events from.
   * @private
 * @suppress {strictMissingProperties} Part of the go/strict_warnings_migration
   */
  detachEvent_(target) {
    this.getHandler().unlisten(target.element_, target.eventType_, this.onTargetClick_);
  }

  /**
   * Sets whether the menu should toggle if it is already open.  For context
   * menus this should be false, for toolbar menus it makes more sense to be true.
   * @param {boolean} toggle The new toggle mode.
   */
  setToggleMode(toggle) {
    this.toggleMode_ = toggle;
}

/**
 * Sets whether the browser context menu will override the menu activation when
 * the shift key is held down.
 * @param {boolean} shiftOverride
 */
setShiftOverride(shiftOverride) {
  this.shiftOverride_ = shiftOverride;
};

  /**
   * Gets whether the menu is in toggle mode
   * @return {boolean} toggle.
   */
  getToggleMode() {
    return this.toggleMode_;
}

/**
 * Gets whether the browser context menu will override the menu activation when
 * the shift key is held down.
 * @return {boolean}
 */
getShiftOverride() {
  return this.shiftOverride_;
};


  /**
   * Show the menu using given positioning object.
   * @param {?goog.positioning.AbstractPosition} position The positioning
   *     instance.
   * @param {goog.positioning.Corner=} opt_menuCorner The corner of the menu to be
   *     positioned.
   * @param {?goog.math.Box=} opt_margin A margin specified in pixels.
   * @param {?Element=} opt_anchor The element which acts as visual anchor for
   *     this menu.
   */
  showWithPosition(position, opt_menuCorner, opt_margin, opt_anchor) {
    var isVisible = this.isVisible();
    if (this.isOrWasRecentlyVisible() && this.toggleMode_) {
      this.hide();
      return;
    }

    // Set current anchor before dispatching BEFORE_SHOW. This is typically useful
    // when we would need to make modifications based on the current anchor to the
    // menu just before displaying it.
    this.currentAnchor_ = opt_anchor || null;

    // Notify event handlers that the menu is about to be shown.
    if (!this.dispatchEvent(goog.ui.Component.EventType.BEFORE_SHOW)) {
      return;
    }

    var menuCorner = typeof opt_menuCorner != 'undefined' ? opt_menuCorner : goog.positioning.Corner.TOP_START;

    // This is a little hacky so that we can position the menu with minimal
    // flicker.

    if (!isVisible) {
      // On IE, setting visibility = 'hidden' on a visible menu
      // will cause a blur, forcing the menu to close immediately.
      this.getElement().style.visibility = 'hidden';
    }

    goog.style.setElementShown(this.getElement(), true);
    position.reposition(this.getElement(), menuCorner, opt_margin);

    if (!isVisible) {
      this.getElement().style.visibility = 'visible';
    }

    this.setHighlightedIndex(-1);

    // setVisible dispatches a goog.ui.Component.EventType.SHOW event, which may
    // be canceled to prevent the menu from being shown.
    this.setVisible(true);
  }

  /**
   * Show the menu at a given attached target.
   * @param {!Object} target Popup target.
   * @param {number} x The client-X associated with the show event.
   * @param {number} y The client-Y associated with the show event.
   * @protected
 * @suppress {strictMissingProperties} Part of the go/strict_warnings_migration
   */
  showMenu(target, x, y) {
    var position = goog.isDef(target.targetCorner_) ? new goog.positioning.AnchoredViewportPosition(target.element_, target.targetCorner_, true) : new goog.positioning.ViewportClientPosition(x, y);
    if (position.setLastResortOverflow) {
      // This is a ViewportClientPosition, so we can set the overflow policy.
      // Allow the menu to slide from the corner rather than clipping if it is
      // completely impossible to fit it otherwise.
      position.setLastResortOverflow(goog.positioning.Overflow.ADJUST_X | goog.positioning.Overflow.ADJUST_Y);
    }
    this.showWithPosition(position, target.menuCorner_, target.margin_, target.element_);
  }

  /**
   * Shows the menu immediately at the given client coordinates.
   * @param {number} x The client-X associated with the show event.
   * @param {number} y The client-Y associated with the show event.
   * @param {goog.positioning.Corner=} opt_menuCorner Corner of the menu that
   *     should be anchored.
   */
  showAt(x, y, opt_menuCorner) {
    this.showWithPosition(new goog.positioning.ViewportClientPosition(x, y), opt_menuCorner);
  }

  /**
   * Shows the menu immediately attached to the given element
   * @param {?Element} element The element to show at.
   * @param {goog.positioning.Corner} targetCorner The corner of the target to
   *     anchor to.
   * @param {goog.positioning.Corner=} opt_menuCorner Corner of the menu that
   *     should be anchored.
   */
  showAtElement(element, targetCorner, opt_menuCorner) {
    this.showWithPosition(new goog.positioning.MenuAnchoredPosition(element, targetCorner, true), opt_menuCorner, null, element);
  }

  /**
   * Hides the menu.
   */
  hide() {
    if (!this.isVisible()) {
      return;
    }

    // setVisible dispatches a goog.ui.Component.EventType.HIDE event, which may
    // be canceled to prevent the menu from being hidden.
    this.setVisible(false);
    if (!this.isVisible()) {
      // HIDE event wasn't canceled; the menu is now hidden.
      this.lastHide_ = goog.now();
      this.currentAnchor_ = null;
    }
  }

  /**
   * Returns whether the menu is currently visible or was visible within about
   * 150 ms ago.  This stops the menu toggling back on if the toggleMode == false.
   * @return {boolean} Whether the popup is currently visible or was visible
   *     within about 150 ms ago.
   */
  isOrWasRecentlyVisible() {
    return this.isVisible() || this.wasRecentlyHidden();
  }

  /**
   * Used to stop the menu toggling back on if the toggleMode == false.
   * @return {boolean} Whether the menu was recently hidden.
   * @protected
   */
  wasRecentlyHidden() {
    return goog.now() - this.lastHide_ < goog.ui.PopupBase.DEBOUNCE_DELAY_MS;
  }

  /**
   * Dismiss the popup menu when an action fires.
   * @param {?goog.events.Event=} opt_e The optional event.
   * @private
   */
  onAction_(opt_e) {
    this.hide();
  }

  /**
   * Handles a browser click event on one of the popup targets.
   * @param {?goog.events.BrowserEvent} e The browser event.
   * @private
   */
  onTargetClick_(e) {
  if (this.shiftOverride_ && e.shiftKey &&
      e.button == goog.events.BrowserEvent.MouseButton.RIGHT) {
    return;
  }
    this.onTargetActivation_(e);
  }

  /**
   * Handles a KEYDOWN browser event on one of the popup targets.
   * @param {!goog.events.BrowserEvent} e The browser event.
   * @private
   */
  onTargetKeyboardAction_(e) {
    if (e.keyCode == goog.events.KeyCodes.SPACE || e.keyCode == goog.events.KeyCodes.ENTER || e.keyCode == goog.events.KeyCodes.DOWN) {
      this.onTargetActivation_(e);
    }
    // If the popupmenu is opened using the DOWN key, the focus should be on the
    // first menu item.
    if (e.keyCode == goog.events.KeyCodes.DOWN) {
      this.highlightFirst();
    }
  }

  /**
   * Handles a browser event on one of the popup targets.
   * @param {?goog.events.BrowserEvent} e The browser event.
   * @private
 * @suppress {strictMissingProperties} Part of the go/strict_warnings_migration
   */
  onTargetActivation_(e) {
    var keys = this.targets_.getKeys();
    for (var i = 0; i < keys.length; i++) {
      var target = /** @type {!Object} */ (this.targets_.get(keys[i]));
      if (target.element_ == e.currentTarget) {
        this.showMenu(target, (e.clientX), (e.clientY));
        e.preventDefault();
        e.stopPropagation();
        return;
      }
    }
  }

  /**
   * Handles click events that propagate to the document.
   * @param {!goog.events.BrowserEvent} e The browser event.
   * @protected
   */
  onDocClick(e) {
    if (this.isVisible() && !this.containsElement(/** @type {!Element} */ (e.target))) {
      this.hide();
    }
  }

  /**
   * Handles the key event target losing focus.
   * @param {?goog.events.BrowserEvent} e The browser event.
   * @protected
   * @override
   */
  handleBlur(e) {
    super.handleBlur(e);
    this.hide();
  }

  /** @override */
  disposeInternal() {
    // Always call the superclass' disposeInternal() first (Bug 715885).
    super.disposeInternal();

    // Disposes of the attachment target map.
    if (this.targets_) {
      this.targets_.clear();
      delete this.targets_;
    }
  }
};

goog.tagUnsealableClass(goog.ui.PopupMenu);


