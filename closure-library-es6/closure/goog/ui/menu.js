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
 * @fileoverview A base menu class that supports key and mouse events. The menu
 * can be bound to an existing HTML structure or can generate its own DOM.
 *
 * To decorate, the menu should be bound to an element containing children
 * with the classname 'goog-menuitem'.  HRs will be classed as separators.
 *
 * Decorate Example:
 * <div id="menu" class="goog-menu" tabIndex="0">
 *   <div class="goog-menuitem">Google</div>
 *   <div class="goog-menuitem">Yahoo</div>
 *   <div class="goog-menuitem">MSN</div>
 *   <hr>
 *   <div class="goog-menuitem">New...</div>
 * </div>
 * <script>
 *
 * var menu = new goog.ui.Menu();
 * menu.decorate(goog.dom.getElement('menu'));
 *
 * TESTED=FireFox 2.0, IE6, Opera 9, Chrome.
 * TODO(user): Key handling is flaky in Opera and Chrome
 * TODO(user): Rename all references of "item" to child since menu is
 * essentially very generic and could, in theory, host a date or color picker.
 *
 * @see ../demos/menu.html
 * @see ../demos/menus.html
 */

goog.provide('goog.ui.Menu');

goog.require('goog.dom.TagName');
goog.require('goog.math.Coordinate');
goog.require('goog.string');
goog.require('goog.style');
goog.require('goog.ui.Component.EventType');
goog.require('goog.ui.Component.State');
goog.require('goog.ui.Container');
goog.require('goog.ui.Container.Orientation');
goog.require('goog.ui.MenuHeader');
goog.require('goog.ui.MenuItem');
goog.require('goog.ui.MenuRenderer');
goog.require('goog.ui.MenuSeparator');

// The dependencies MenuHeader, MenuItem, and MenuSeparator are implicit.
// There are no references in the code, but we need to load these
// classes before goog.ui.Menu.



// TODO(robbyw): Reverse constructor argument order for consistency.
/**
   * A basic menu class.
   * 
   * 
   */
goog.ui.Menu = class extends goog.ui.Container {
  /**
  * A basic menu class.
  * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
  * @param {goog.ui.MenuRenderer=} opt_renderer Renderer used to render or
  *     decorate the container; defaults to {@link goog.ui.MenuRenderer}.
  *
   */
  constructor(opt_domHelper, opt_renderer) {
    super( goog.ui.Container.Orientation.VERTICAL, opt_renderer || goog.ui.MenuRenderer.getInstance(), opt_domHelper);

    // Unlike Containers, Menus aren't keyboard-accessible by default.  This line
    // preserves backwards compatibility with code that depends on menus not
    // receiving focus - e.g. `goog.ui.MenuButton`.
    this.setFocusable(false);
    /**
     * Whether the menu can move the focus to its key event target when it is
     * shown.  Default = true
     * @type {boolean}
     * @private
     */
    this.allowAutoFocus_ = true;
    /**
     * Whether the menu should use windows style behavior and allow disabled menu
     * items to be highlighted (though not selectable).  Defaults to false
     * @type {boolean}
     * @private
     */
    this.allowHighlightDisabled_ = false;


    /**
     * Coordinates of the mousedown event that caused this menu to be made visible.
     * Used to prevent the consequent mouseup event due to a simple click from
     * activating a menu item immediately. Considered protected; should only be used
     * within this package or by subclasses.
     * @type {goog.math.Coordinate|undefined}
     */
    this.openingCoords = undefined;
  }

  /**
   * Returns the CSS class applied to menu elements, also used as the prefix for
   * derived styles, if any.  Subclasses should override this method as needed.
   * Considered protected.
   * @return {string} The CSS class applied to menu elements.
   * @protected
   * @deprecated Use getRenderer().getCssClass().
   */
  getCssClass() {
    return this.getRenderer().getCssClass();
  }

  /**
   * Returns whether the provided element is to be considered inside the menu for
   * purposes such as dismissing the menu on an event.  This is so submenus can
   * make use of elements outside their own DOM.
   * @param {Element} element The element to test for.
   * @return {boolean} Whether the provided element is to be considered inside
   *     the menu.
   */
  containsElement(element) {
    const renderer = /** @type {!goog.ui.MenuRenderer} */ (this.getRenderer());
    if (renderer.containsElement(this, element)) {
      return true;
    }

    for (var i = 0, count = this.getChildCount(); i < count; i++) {
      var child = this.getChildAt(i);
      if (typeof child.containsElement == 'function' && child.containsElement(element)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Adds a new menu item at the end of the menu.
   * @param {goog.ui.MenuHeader|goog.ui.MenuItem|goog.ui.MenuSeparator} item Menu
   *     item to add to the menu.
   * @deprecated Use {@link #addChild} instead, with true for the second argument.
   */
  addItem(item) {
    this.addChild(item, true);
  }

  /**
   * Adds a new menu item at a specific index in the menu.
   * @param {goog.ui.MenuHeader|goog.ui.MenuItem|goog.ui.MenuSeparator} item Menu
   *     item to add to the menu.
   * @param {number} n Index at which to insert the menu item.
   * @deprecated Use {@link #addChildAt} instead, with true for the third
   *     argument.
   */
  addItemAt(item, n) {
    this.addChildAt(item, n, true);
  }

  /**
   * Removes an item from the menu and disposes of it.
   * @param {goog.ui.MenuHeader|goog.ui.MenuItem|goog.ui.MenuSeparator} item The
   *     menu item to remove.
   * @deprecated Use {@link #removeChild} instead.
   */
  removeItem(item) {
    var removedChild = this.removeChild(item, true);
    if (removedChild) {
      removedChild.dispose();
    }
  }

  /**
   * Removes a menu item at a given index in the menu and disposes of it.
   * @param {number} n Index of item.
   * @deprecated Use {@link #removeChildAt} instead.
   */
  removeItemAt(n) {
    var removedChild = this.removeChildAt(n, true);
    if (removedChild) {
      removedChild.dispose();
    }
  }

  /**
   * Returns a reference to the menu item at a given index.
   * @param {number} n Index of menu item.
   * @return {goog.ui.MenuHeader|goog.ui.MenuItem|goog.ui.MenuSeparator|null}
   *     Reference to the menu item.
   * @deprecated Use {@link #getChildAt} instead.
   */
  getItemAt(n) {
    return /** @type {goog.ui.MenuItem?} */ (this.getChildAt(n));
  }

  /**
   * Returns the number of items in the menu (including separators).
   * @return {number} The number of items in the menu.
   * @deprecated Use {@link #getChildCount} instead.
   */
  getItemCount() {
    return this.getChildCount();
  }

  /**
   * Returns an array containing the menu items contained in the menu.
   * @return {!Array<goog.ui.MenuItem>} An array of menu items.
   * @deprecated Use getChildAt, forEachChild, and getChildCount.
   */
  getItems() {
    // TODO(user): Remove reference to getItems and instead use getChildAt,
    // forEachChild, and getChildCount
    var children = [];
    this.forEachChild(function(child) { children.push(child); });
    return children;
  }

  /**
   * Sets the position of the menu relative to the view port.
   * @param {number|goog.math.Coordinate} x Left position or coordinate obj.
   * @param {number=} opt_y Top position.
   */
  setPosition(x, opt_y) {
    // NOTE(user): It is necessary to temporarily set the display from none, so
    // that the position gets set correctly.
    var visible = this.isVisible();
    if (!visible) {
      goog.style.setElementShown(this.getElement(), true);
    }
    goog.style.setPageOffset(this.getElement(), x, opt_y);
    if (!visible) {
      goog.style.setElementShown(this.getElement(), false);
    }
  }

  /**
   * Gets the page offset of the menu, or null if the menu isn't visible
   * @return {goog.math.Coordinate?} Object holding the x-y coordinates of the
   *     menu or null if the menu is not visible.
   */
  getPosition() {
    return this.isVisible() ? goog.style.getPageOffset(this.getElement()) : null;
  }

  /**
   * Sets whether the menu can automatically move focus to its key event target
   * when it is set to visible.
   * @param {boolean} allow Whether the menu can automatically move focus to its
   *     key event target when it is set to visible.
   */
  setAllowAutoFocus(allow) {
    this.allowAutoFocus_ = allow;
    if (allow) {
      this.setFocusable(true);
    }
  }

  /**
   * @return {boolean} Whether the menu can automatically move focus to its key
   *     event target when it is set to visible.
   */
  getAllowAutoFocus() {
    return this.allowAutoFocus_;
  }

  /**
   * Sets whether the menu will highlight disabled menu items or skip to the next
   * active item.
   * @param {boolean} allow Whether the menu will highlight disabled menu items or
   *     skip to the next active item.
   */
  setAllowHighlightDisabled(allow) {
    this.allowHighlightDisabled_ = allow;
  }

  /**
   * @return {boolean} Whether the menu will highlight disabled menu items or skip
   *     to the next active item.
   */
  getAllowHighlightDisabled() {
    return this.allowHighlightDisabled_;
  }

  /**
   * @override
   * @param {boolean} show Whether to show or hide the menu.
   * @param {boolean=} opt_force If true, doesn't check whether the menu
   *     already has the requested visibility, and doesn't dispatch any events.
   * @param {goog.events.Event=} opt_e Mousedown event that caused this menu to
   *     be made visible (ignored if show is false).
   */
  setVisible(show, opt_force, opt_e) {
    var visibilityChanged = super.setVisible(show, opt_force);
    if (visibilityChanged && show && this.isInDocument() && this.allowAutoFocus_) {
      this.getKeyEventTarget().focus();
    }
    opt_e = /** @type{?goog.events.BrowserEvent|undefined} */ (opt_e);
    if (show && opt_e && goog.isNumber(opt_e.clientX)) {
      this.openingCoords = new goog.math.Coordinate(opt_e.clientX, opt_e.clientY);
    } else {
      this.openingCoords = null;
    }
    return visibilityChanged;
  }

  /** @override */
  handleEnterItem(e) {
    if (this.allowAutoFocus_) {
      this.getKeyEventTarget().focus();
    }

    return super.handleEnterItem(e);
  }

  /**
   * Highlights the next item that begins with the specified string.  If no
   * (other) item begins with the given string, the selection is unchanged.
   * @param {string} charStr The prefix to match.
   * @return {boolean} Whether a matching prefix was found.
   */
  highlightNextPrefix(charStr) {
    var re = new RegExp('^' + goog.string.regExpEscape(charStr), 'i');
    return this.highlightHelper(function(index, max) {
      // Index is >= -1 because it is set to -1 when nothing is selected.
      var start = index < 0 ? 0 : index;
      var wrapped = false;

      // We always start looking from one after the current, because we
      // keep the current selection only as a last resort. This makes the
      // loop a little awkward in the case where there is no current
      // selection, as we need to stop somewhere but can't just stop
      // when index == start, which is why we need the 'wrapped' flag.
      do {
        ++index;
        if (index == max) {
          index = 0;
          wrapped = true;
        }
        var name = this.getChildAt(index).getCaption();
        if (name && name.match(re)) {
          return index;
        }
      } while (!wrapped || index != start);
      return this.getHighlightedIndex();
    }, this.getHighlightedIndex());
  }

  /** @override */
  canHighlightItem(item) {
    return (this.allowHighlightDisabled_ || item.isEnabled()) && item.isVisible() && item.isSupportedState(goog.ui.Component.State.HOVER);
  }

  /** @override */
  decorateInternal(element) {
    this.decorateContent(element);
    super.decorateInternal(element);
  }

  /** @override */
  handleKeyEventInternal(e) {
    var handled = super.handleKeyEventInternal( e);
    if (!handled) {
      // Loop through all child components, and for each menu item call its
      // key event handler so that keyboard mnemonics can be handled.
      this.forEachChild(function(menuItem) {
        if (!handled && menuItem.getMnemonic && menuItem.getMnemonic() == e.keyCode) {
          if (this.isEnabled()) {
            this.setHighlighted(menuItem);
          }
          // We still delegate to handleKeyEvent, so that it can handle
          // enabled/disabled state.
          handled = menuItem.handleKeyEvent(e);
        }
      }, this);
    }
    return handled;
  }

  /** @override */
  setHighlightedIndex(index) {
    super.setHighlightedIndex( index);

    // Bring the highlighted item into view. This has no effect if the menu is not
    // scrollable.
    var child = this.getChildAt(index);
    if (child) {
      goog.style.scrollIntoContainerView(child.getElement(), this.getElement());
    }
  }

  /**
   * Decorate menu items located in any descendant node which as been explicitly
   * marked as a 'content' node.
   * @param {Element} element Element to decorate.
   * @protected
   */
  decorateContent(element) {
    var renderer = this.getRenderer();
    var contentElements = this.getDomHelper().getElementsByTagNameAndClass(
        goog.dom.TagName.DIV, goog.getCssName(renderer.getCssClass(), 'content'), element);

    // Some versions of IE do not like it when you access this nodeList
    // with invalid indices. See
    // http://code.google.com/p/closure-library/issues/detail?id=373
    var length = contentElements.length;
    for (var i = 0; i < length; i++) {
      renderer.decorateChildren(this, contentElements[i]);
    }
  }
};

goog.tagUnsealableClass(goog.ui.Menu);


// TODO(robbyw): Remove this and all references to it.
/**
 * CSS class for menus.
 * @type {string}
 * @deprecated Use goog.ui.MenuRenderer.CSS_CLASS.
 */
goog.ui.Menu.CSS_CLASS = goog.ui.MenuRenderer.CSS_CLASS;



