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
 * @fileoverview A utility class for representing a numeric box.
 */


goog.provide('goog.math.Box');

goog.require('goog.asserts');
goog.require('goog.math.Coordinate');

/**
   * Class for representing a box. A box is specified as a top, right, bottom,
   * and left. A box is useful for representing margins and padding.
   *
   * This class assumes 'screen coordinates': larger Y coordinates are further
   * from the top of the screen.
   *
   *
   *
   *
   *
   * @struct
   * 
   */
goog.math.Box = class {
  /**
  * Class for representing a box. A box is specified as a top, right, bottom,
  * and left. A box is useful for representing margins and padding.
  *
  * This class assumes 'screen coordinates': larger Y coordinates are further
  * from the top of the screen.
  *
  * @param {number} top Top.
  * @param {number} right Right.
  * @param {number} bottom Bottom.
  * @param {number} left Left.
  *
   */
  constructor(top, right, bottom, left) {
    /**
     * Top
     * @type {number}
     */
    this.top = top;

    /**
     * Right
     * @type {number}
     */
    this.right = right;

    /**
     * Bottom
     * @type {number}
     */
    this.bottom = bottom;

    /**
     * Left
     * @type {number}
     */
    this.left = left;
  }

  /**
   * @return {number} width The width of this Box.
   */
  getWidth() {
    return this.right - this.left;
  }

  /**
   * @return {number} height The height of this Box.
   */
  getHeight() {
    return this.bottom - this.top;
  }

  /**
   * Creates a copy of the box with the same dimensions.
   * @return {!goog.math.Box} A clone of this Box.
   */
  clone() {
    return new goog.math.Box(this.top, this.right, this.bottom, this.left);
  }

  /**
   * Returns a nice string representing the box.
   * @return {string} In the form (50t, 73r, 24b, 13l).
   * @override
   */
  toString() {
    return '(' + this.top + 't, ' + this.right + 'r, ' + this.bottom + 'b, ' +
        this.left + 'l)';
  }

  /**
   * Returns whether the box contains a coordinate or another box.
   *
   * @param {goog.math.Coordinate|goog.math.Box} other A Coordinate or a Box.
   * @return {boolean} Whether the box contains the coordinate or other box.
   */
  contains(other) {
    return goog.math.Box.contains(this, other);
  }

  /**
   * Expands box with the given margins.
   *
   * @param {number|goog.math.Box} top Top margin or box with all margins.
   * @param {number=} opt_right Right margin.
   * @param {number=} opt_bottom Bottom margin.
   * @param {number=} opt_left Left margin.
   * @return {!goog.math.Box} A reference to this Box.
   */
  expand(top, opt_right, opt_bottom, opt_left) {
    if (goog.isObject(top)) {
      this.top -= top.top;
      this.right += top.right;
      this.bottom += top.bottom;
      this.left -= top.left;
    } else {
      this.top -= /** @type {number} */ (top);
      this.right += Number(opt_right);
      this.bottom += Number(opt_bottom);
      this.left -= Number(opt_left);
    }

    return this;
  }

  /**
   * Expand this box to include another box.
   * NOTE(user): This is used in code that needs to be very fast, please don't
   * add functionality to this function at the expense of speed (variable
   * arguments, accepting multiple argument types, etc).
   * @param {goog.math.Box} box The box to include in this one.
   */
  expandToInclude(box) {
    this.left = Math.min(this.left, box.left);
    this.top = Math.min(this.top, box.top);
    this.right = Math.max(this.right, box.right);
    this.bottom = Math.max(this.bottom, box.bottom);
  }

  /**
   * Expand this box to include the coordinate.
   * @param {!goog.math.Coordinate} coord The coordinate to be included
   *     inside the box.
   */
  expandToIncludeCoordinate(coord) {
    this.top = Math.min(this.top, coord.y);
    this.right = Math.max(this.right, coord.x);
    this.bottom = Math.max(this.bottom, coord.y);
    this.left = Math.min(this.left, coord.x);
  }

  /**
   * Rounds the fields to the next larger integer values.
   *
   * @return {!goog.math.Box} This box with ceil'd fields.
   */
  ceil() {
    this.top = Math.ceil(this.top);
    this.right = Math.ceil(this.right);
    this.bottom = Math.ceil(this.bottom);
    this.left = Math.ceil(this.left);
    return this;
  }

  /**
   * Rounds the fields to the next smaller integer values.
   *
   * @return {!goog.math.Box} This box with floored fields.
   */
  floor() {
    this.top = Math.floor(this.top);
    this.right = Math.floor(this.right);
    this.bottom = Math.floor(this.bottom);
    this.left = Math.floor(this.left);
    return this;
  }

  /**
   * Rounds the fields to nearest integer values.
   *
   * @return {!goog.math.Box} This box with rounded fields.
   */
  round() {
    this.top = Math.round(this.top);
    this.right = Math.round(this.right);
    this.bottom = Math.round(this.bottom);
    this.left = Math.round(this.left);
    return this;
  }

  /**
   * Translates this box by the given offsets. If a `goog.math.Coordinate`
   * is given, then the left and right values are translated by the coordinate's
   * x value and the top and bottom values are translated by the coordinate's y
   * value.  Otherwise, `tx` and `opt_ty` are used to translate the x
   * and y dimension values.
   *
   * @param {number|goog.math.Coordinate} tx The value to translate the x
   *     dimension values by or the the coordinate to translate this box by.
   * @param {number=} opt_ty The value to translate y dimension values by.
   * @return {!goog.math.Box} This box after translating.
   */
  translate(tx, opt_ty) {
    if (tx instanceof goog.math.Coordinate) {
      this.left += tx.x;
      this.right += tx.x;
      this.top += tx.y;
      this.bottom += tx.y;
    } else {
      goog.asserts.assertNumber(tx);
      this.left += tx;
      this.right += tx;
      if (goog.isNumber(opt_ty)) {
        this.top += opt_ty;
        this.bottom += opt_ty;
      }
    }
    return this;
  }

  /**
   * Scales this coordinate by the given scale factors. The x and y dimension
   * values are scaled by `sx` and `opt_sy` respectively.
   * If `opt_sy` is not given, then `sx` is used for both x and y.
   *
   * @param {number} sx The scale factor to use for the x dimension.
   * @param {number=} opt_sy The scale factor to use for the y dimension.
   * @return {!goog.math.Box} This box after scaling.
   */
  scale(sx, opt_sy) {
    var sy = goog.isNumber(opt_sy) ? opt_sy : sx;
    this.left *= sx;
    this.right *= sx;
    this.top *= sy;
    this.bottom *= sy;
    return this;
  }

  /**
   * Creates a Box by bounding a collection of goog.math.Coordinate objects
   * @param {...goog.math.Coordinate} var_args Coordinates to be included inside
   *     the box.
   * @return {!goog.math.Box} A Box containing all the specified Coordinates.
   */
  static boundingBox(var_args) {
    var box = new goog.math.Box(arguments[0].y, arguments[0].x, arguments[0].y, arguments[0].x);
    for (var i = 1; i < arguments.length; i++) {
      box.expandToIncludeCoordinate(arguments[i]);
    }
    return box;
  }

  /**
   * Compares boxes for equality.
   * @param {goog.math.Box} a A Box.
   * @param {goog.math.Box} b A Box.
   * @return {boolean} True iff the boxes are equal, or if both are null.
   */
  static equals(a, b) {
    if (a == b) {
      return true;
    }
    if (!a || !b) {
      return false;
    }
    return a.top == b.top && a.right == b.right && a.bottom == b.bottom && a.left == b.left;
  }

  /**
   * Returns whether a box contains a coordinate or another box.
   *
   * @param {goog.math.Box} box A Box.
   * @param {goog.math.Coordinate|goog.math.Box} other A Coordinate or a Box.
   * @return {boolean} Whether the box contains the coordinate or other box.
   */
  static contains(box, other) {
    if (!box || !other) {
      return false;
    }

    if (other instanceof goog.math.Box) {
      return other.left >= box.left && other.right <= box.right && other.top >= box.top && other.bottom <= box.bottom;
    }

    // other is a Coordinate.
    return other.x >= box.left && other.x <= box.right && other.y >= box.top && other.y <= box.bottom;
  }

  /**
   * Returns the relative x position of a coordinate compared to a box.  Returns
   * zero if the coordinate is inside the box.
   *
   * @param {goog.math.Box} box A Box.
   * @param {goog.math.Coordinate} coord A Coordinate.
   * @return {number} The x position of `coord` relative to the nearest
   *     side of `box`, or zero if `coord` is inside `box`.
   */
  static relativePositionX(box, coord) {
    if (coord.x < box.left) {
      return coord.x - box.left;
    } else if (coord.x > box.right) {
      return coord.x - box.right;
    }
    return 0;
  }

  /**
   * Returns the relative y position of a coordinate compared to a box.  Returns
   * zero if the coordinate is inside the box.
   *
   * @param {goog.math.Box} box A Box.
   * @param {goog.math.Coordinate} coord A Coordinate.
   * @return {number} The y position of `coord` relative to the nearest
   *     side of `box`, or zero if `coord` is inside `box`.
   */
  static relativePositionY(box, coord) {
    if (coord.y < box.top) {
      return coord.y - box.top;
    } else if (coord.y > box.bottom) {
      return coord.y - box.bottom;
    }
    return 0;
  }

  /**
   * Returns the distance between a coordinate and the nearest corner/side of a
   * box. Returns zero if the coordinate is inside the box.
   *
   * @param {goog.math.Box} box A Box.
   * @param {goog.math.Coordinate} coord A Coordinate.
   * @return {number} The distance between `coord` and the nearest
   *     corner/side of `box`, or zero if `coord` is inside
   *     `box`.
   */
  static distance(box, coord) {
    var x = goog.math.Box.relativePositionX(box, coord);
    var y = goog.math.Box.relativePositionY(box, coord);
    return Math.sqrt(x * x + y * y);
  }

  /**
   * Returns whether two boxes intersect.
   *
   * @param {goog.math.Box} a A Box.
   * @param {goog.math.Box} b A second Box.
   * @return {boolean} Whether the boxes intersect.
   */
  static intersects(a, b) {
    return (a.left <= b.right && b.left <= a.right && a.top <= b.bottom && b.top <= a.bottom);
  }

  /**
   * Returns whether two boxes would intersect with additional padding.
   *
   * @param {goog.math.Box} a A Box.
   * @param {goog.math.Box} b A second Box.
   * @param {number} padding The additional padding.
   * @return {boolean} Whether the boxes intersect.
   */
  static intersectsWithPadding(a, b, padding) {
    return (a.left <= b.right + padding && b.left <= a.right + padding && a.top <= b.bottom + padding && b.top <= a.bottom + padding);
  }
};

if (goog.DEBUG) {
}


