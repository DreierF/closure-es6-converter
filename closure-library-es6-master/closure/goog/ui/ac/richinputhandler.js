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
 * @fileoverview Class for managing the interactions between a rich autocomplete
 * object and a text-input or textarea.
 *
 */

goog.provide('goog.ui.ac.RichInputHandler');

goog.require('goog.ui.ac.InputHandler');

/**
   * Class for managing the interaction between an autocomplete object and a
   * text-input or textarea.
   *
   *
   *
   *     (Default: true).
   *
   (Default: 150).
   * 
   * 
   */
goog.ui.ac.RichInputHandler = class extends goog.ui.ac.InputHandler {
  /**
  * Class for managing the interaction between an autocomplete object and a
  * text-input or textarea.
  * @param {?string=} opt_separators Seperators to split multiple entries.
  * @param {?string=} opt_literals Characters used to delimit text literals.
  * @param {?boolean=} opt_multi Whether to allow multiple entries
  *     (Default: true).
  * @param {?number=} opt_throttleTime Number of milliseconds to throttle
  *     keyevents with (Default: 150).
  *
   */
  constructor(opt_separators, opt_literals, opt_multi, opt_throttleTime) {
    super( opt_separators, opt_literals, opt_multi, opt_throttleTime);
  }

  /**
   * Selects the given rich row.  The row's select(target) method is called.
   * @param {Object} row The row to select.
   * @return {boolean} Whether to suppress the update event.
   * @override
   */
  selectRow(row) {
    var suppressUpdate = super.selectRow(row);
    row.select(this.ac_.getTarget());
    return suppressUpdate;
  }
};



