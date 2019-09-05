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

goog.provide('goog.testing.assertsTest');
goog.setTestOnly('goog.testing.assertsTest');

goog.require('goog.Promise');
goog.require('goog.array');
goog.require('goog.async.Deferred');
goog.require('goog.dom');
goog.require('goog.iter.Iterator');
goog.require('goog.iter.StopIteration');
goog.require('goog.structs.Map');
goog.require('goog.structs.Set');
goog.require('goog.testing.TestCase');
goog.require('goog.testing.asserts');
goog.require('goog.testing.jsunit');
goog.require('goog.userAgent');
goog.require('goog.userAgent.product');


function testAssertTrue() {
  assertTrue(true);
  assertTrue('Good assertion', true);
  assertThrowsJsUnitException(function() {
    assertTrue(false);
  }, 'Call to assertTrue(boolean) with false');
  assertThrowsJsUnitException(function() {
    assertTrue('Should be true', false);
  }, 'Should be true\nCall to assertTrue(boolean) with false');
  assertThrowsJsUnitException(function() {
    assertTrue(null);
  }, 'Bad argument to assertTrue(boolean)');
  assertThrowsJsUnitException(function() {
    assertTrue(undefined);
  }, 'Bad argument to assertTrue(boolean)');
}

function testAssertFalse() {
  assertFalse(false);
  assertFalse('Good assertion', false);
  assertThrowsJsUnitException(function() {
    assertFalse(true);
  }, 'Call to assertFalse(boolean) with true');
  assertThrowsJsUnitException(function() {
    assertFalse('Should be false', true);
  }, 'Should be false\nCall to assertFalse(boolean) with true');
  assertThrowsJsUnitException(function() {
    assertFalse(null);
  }, 'Bad argument to assertFalse(boolean)');
  assertThrowsJsUnitException(function() {
    assertFalse(undefined);
  }, 'Bad argument to assertFalse(boolean)');
}

function testAssertEqualsWithString() {
  assertEquals('a', 'a');
  assertEquals('Good assertion', 'a', 'a');
  assertThrowsJsUnitException(function() {
    assertEquals('a', 'b');
  }, 'Expected <a> (String) but was <b> (String)');
  assertThrowsJsUnitException(function() {
    assertEquals('Bad assertion', 'a', 'b');
  }, 'Bad assertion\nExpected <a> (String) but was <b> (String)');
}

function testAssertEqualsWithInteger() {
  assertEquals(1, 1);
  assertEquals('Good assertion', 1, 1);
  assertThrowsJsUnitException(function() {
    assertEquals(1, 2);
  }, 'Expected <1> (Number) but was <2> (Number)');
  assertThrowsJsUnitException(function() {
    assertEquals('Bad assertion', 1, 2);
  }, 'Bad assertion\nExpected <1> (Number) but was <2> (Number)');
}

function testAssertNotEquals() {
  assertNotEquals('a', 'b');
  assertNotEquals('a', 'a', 'b');
  assertThrowsJsUnitException(function() {
    assertNotEquals('a', 'a');
  }, 'Expected not to be <a> (String)');
  assertThrowsJsUnitException(function() {
    assertNotEquals('a', 'a', 'a');
  }, 'a\nExpected not to be <a> (String)');
}

function testAssertNull() {
  assertNull(null);
  assertNull('Good assertion', null);
  assertThrowsJsUnitException(function() {
    assertNull(true);
  }, 'Expected <null> but was <true> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNull('Should be null', false);
  }, 'Should be null\nExpected <null> but was <false> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNull(undefined);
  }, 'Expected <null> but was <undefined>');
  assertThrowsJsUnitException(function() {
    assertNull(1);
  }, 'Expected <null> but was <1> (Number)');
}

function testAssertNullOrUndefined() {
  assertNullOrUndefined(null);
  assertNullOrUndefined(undefined);
  assertNullOrUndefined('Good assertion', null);
  assertNullOrUndefined('Good assertion', undefined);
  assertThrowsJsUnitException(function() {
    assertNullOrUndefined(true);
  }, 'Expected <null> or <undefined> but was <true> (Boolean)');
  assertThrowsJsUnitException(
      function() {
        assertNullOrUndefined('Should be null', false);
      },
      'Should be null\n' +
          'Expected <null> or <undefined> but was <false> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNullOrUndefined(0);
  }, 'Expected <null> or <undefined> but was <0> (Number)');
}

function testAssertNotNull() {
  assertNotNull(true);
  assertNotNull('Good assertion', true);
  assertNotNull(false);
  assertNotNull(undefined);
  assertNotNull(1);
  assertNotNull('a');
  assertThrowsJsUnitException(function() {
    assertNotNull(null);
  }, 'Expected not to be <null>');
  assertThrowsJsUnitException(function() {
    assertNotNull('Should not be null', null);
  }, 'Should not be null\nExpected not to be <null>');
}

function testAssertUndefined() {
  assertUndefined(undefined);
  assertUndefined('Good assertion', undefined);
  assertThrowsJsUnitException(function() {
    assertUndefined(true);
  }, 'Expected <undefined> but was <true> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertUndefined('Should be undefined', false);
  }, 'Should be undefined\nExpected <undefined> but was <false> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertUndefined(null);
  }, 'Expected <undefined> but was <null>');
  assertThrowsJsUnitException(function() {
    assertUndefined(1);
  }, 'Expected <undefined> but was <1> (Number)');
}

function testAssertNotUndefined() {
  assertNotUndefined(true);
  assertNotUndefined('Good assertion', true);
  assertNotUndefined(false);
  assertNotUndefined(null);
  assertNotUndefined(1);
  assertNotUndefined('a');
  assertThrowsJsUnitException(function() {
    assertNotUndefined(undefined);
  }, 'Expected not to be <undefined>');
  assertThrowsJsUnitException(function() {
    assertNotUndefined('Should not be undefined', undefined);
  }, 'Should not be undefined\nExpected not to be <undefined>');
}

function testAssertNotNullNorUndefined() {
  assertNotNullNorUndefined(true);
  assertNotNullNorUndefined('Good assertion', true);
  assertNotNullNorUndefined(false);
  assertNotNullNorUndefined(1);
  assertNotNullNorUndefined(0);
  assertNotNullNorUndefined('a');
  assertThrowsJsUnitException(function() {
    assertNotNullNorUndefined(undefined);
  }, 'Expected not to be <undefined>');
  assertThrowsJsUnitException(function() {
    assertNotNullNorUndefined('Should not be undefined', undefined);
  }, 'Should not be undefined\nExpected not to be <undefined>');
  assertThrowsJsUnitException(function() {
    assertNotNullNorUndefined(null);
  }, 'Expected not to be <null>');
  assertThrowsJsUnitException(function() {
    assertNotNullNorUndefined('Should not be null', null);
  }, 'Should not be null\nExpected not to be <null>');
}

function testAssertNonEmptyString() {
  assertNonEmptyString('hello');
  assertNonEmptyString('Good assertion', 'hello');
  assertNonEmptyString('true');
  assertNonEmptyString('false');
  assertNonEmptyString('1');
  assertNonEmptyString('null');
  assertNonEmptyString('undefined');
  assertNonEmptyString('\n');
  assertNonEmptyString(' ');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString('');
  }, 'Expected non-empty string but was <> (String)');
  assertThrowsJsUnitException(
      function() { assertNonEmptyString('Should be non-empty string', ''); },
      'Should be non-empty string\n' +
          'Expected non-empty string but was <> (String)');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(true);
  }, 'Expected non-empty string but was <true> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(false);
  }, 'Expected non-empty string but was <false> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(1);
  }, 'Expected non-empty string but was <1> (Number)');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(null);
  }, 'Expected non-empty string but was <null>');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(undefined);
  }, 'Expected non-empty string but was <undefined>');
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(['hello']);
  }, 'Expected non-empty string but was <hello> (Array)');
  // Different browsers return different values/types in the failure message
  // so don't bother checking if the message is exactly as expected.
  assertThrowsJsUnitException(function() {
    assertNonEmptyString(goog.dom.createTextNode('hello'));
  });
}

function testAssertNaN() {
  assertNaN(NaN);
  assertNaN('Good assertion', NaN);
  assertThrowsJsUnitException(function() {
    assertNaN(1);
  }, 'Expected NaN but was <1> (Number)');
  assertThrowsJsUnitException(function() {
    assertNaN('Should be NaN', 1);
  }, 'Should be NaN\nExpected NaN but was <1> (Number)');
  assertThrowsJsUnitException(function() {
    assertNaN(true);
  }, 'Expected NaN but was <true> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNaN(false);
  }, 'Expected NaN but was <false> (Boolean)');
  assertThrowsJsUnitException(function() {
    assertNaN(null);
  }, 'Expected NaN but was <null>');
  assertThrowsJsUnitException(function() {
    assertNaN('');
  }, 'Expected NaN but was <> (String)');

  // TODO(user): These assertions fail. We should decide on the
  // semantics of assertNaN
  // assertThrowsJsUnitException(function() { assertNaN(undefined); },
  //    'Expected NaN');
  // assertThrowsJsUnitException(function() { assertNaN('a'); },
  //    'Expected NaN');
}

function testAssertNotNaN() {
  assertNotNaN(1);
  assertNotNaN('Good assertion', 1);
  assertNotNaN(true);
  assertNotNaN(false);
  assertNotNaN('');
  assertNotNaN(null);

  // TODO(user): These assertions fail. We should decide on the
  // semantics of assertNotNaN
  // assertNotNaN(undefined);
  // assertNotNaN('a');

  assertThrowsJsUnitException(function() {
    assertNotNaN(Number.NaN);
  }, 'Expected not NaN');
  assertThrowsJsUnitException(function() {
    assertNotNaN('Should not be NaN', Number.NaN);
  }, 'Should not be NaN\nExpected not NaN');
}

function testAssertObjectEquals() {
  const obj1 = [{'a': 'hello', 'b': 'world'}];
  const obj2 = [{'a': 'hello', 'c': 'dear', 'b': 'world'}];

  // Check with obj1 and obj2 as first and second arguments respectively.
  assertThrowsJsUnitException(function() {
    assertObjectEquals(obj1, obj2);
  });

  // Check with obj1 and obj2 as second and first arguments respectively.
  assertThrowsJsUnitException(function() {
    assertObjectEquals(obj2, obj1);
  });

  // Test if equal objects are considered equal.
  const obj3 = [{'b': 'world', 'a': 'hello'}];
  assertObjectEquals(obj1, obj3);
  assertObjectEquals(obj3, obj1);

  // Test with a case where one of the members has an undefined value.
  const obj4 = [{'a': 'hello', 'b': undefined}];
  const obj5 = [{'a': 'hello'}];

  // Check with obj4 and obj5 as first and second arguments respectively.
  assertThrowsJsUnitException(function() {
    assertObjectEquals(obj4, obj5);
  });

  // Check with obj5 and obj4 as first and second arguments respectively.
  assertThrowsJsUnitException(function() {
    assertObjectEquals(obj5, obj4);
  });
}

function testAssertObjectNotEquals() {
  const obj1 = [{'a': 'hello', 'b': 'world'}];
  const obj2 = [{'a': 'hello', 'c': 'dear', 'b': 'world'}];

  // Check with obj1 and obj2 as first and second arguments respectively.
  assertObjectNotEquals(obj1, obj2);

  // Check with obj1 and obj2 as second and first arguments respectively.
  assertObjectNotEquals(obj2, obj1);

  // Test if equal objects are considered equal.
  const obj3 = [{'b': 'world', 'a': 'hello'}];
  let error = assertThrowsJsUnitException(function() {
    assertObjectNotEquals(obj1, obj3);
  });
  assertContains('Objects should not be equal', error.message);
  error = assertThrowsJsUnitException(function() {
    assertObjectNotEquals(obj3, obj1);
  });
  assertContains('Objects should not be equal', error.message);

  // Test with a case where one of the members has an undefined value.
  const obj4 = [{'a': 'hello', 'b': undefined}];
  const obj5 = [{'a': 'hello'}];

  // Check with obj4 and obj5 as first and second arguments respectively.
  assertObjectNotEquals(obj4, obj5);

  // Check with obj5 and obj4 as first and second arguments respectively.
  assertObjectNotEquals(obj5, obj4);

  assertObjectNotEquals(new Map([['a', '1']]), new Map([['b', '1']]));
  assertObjectNotEquals(new Set(['a', 'b']), new Set(['a']));

  if (SUPPORTS_TYPED_ARRAY) {
    assertObjectNotEquals(
        new Uint32Array([1, 2, 3]), new Uint32Array([1, 4, 3]));
  }
}

function testAssertObjectEquals2() {
  // NOTE: (0 in [undefined]) is true on FF but false on IE.
  // (0 in {'0': undefined}) is true on both.
  // grrr.
  assertObjectEquals('arrays should be equal', [undefined], [undefined]);
  assertThrowsJsUnitException(function() {
    assertObjectEquals([undefined, undefined], [undefined]);
  });
  assertThrowsJsUnitException(function() {
    assertObjectEquals([undefined], [undefined, undefined]);
  });
}

function testAssertObjectEquals3() {
  // Check that objects that contain identical Map objects compare
  // as equals. We can't do a negative test because on browsers that
  // implement __iterator__ we can't check the values of the iterated
  // properties.
  const obj1 = [
    {'a': 'hi', 'b': new goog.structs.Map('hola', 'amigo', 'como', 'estas?')},
    14, 'yes', true
  ];
  const obj2 = [
    {'a': 'hi', 'b': new goog.structs.Map('hola', 'amigo', 'como', 'estas?')},
    14, 'yes', true
  ];
  assertObjectEquals('Objects should be equal', obj1, obj2);

  const obj3 = {'a': [1, 2]};
  const obj4 = {'a': [1, 2, 3]};
  // inner arrays should not be equal
  assertThrowsJsUnitException(function() {
    assertObjectEquals(obj3, obj4);
  });
  // inner arrays should not be equal
  assertThrowsJsUnitException(function() {
    assertObjectEquals(obj4, obj3);
  });
}

function testAssertObjectEqualsSet() {
  // verify that Sets compare equal, when run in an environment that
  // supports iterators
  const set1 = new goog.structs.Set();
  const set2 = new goog.structs.Set();

  set1.add('a');
  set1.add('b');
  set1.add(13);

  set2.add('a');
  set2.add('b');
  set2.add(13);

  assertObjectEquals('sets should be equal', set1, set2);

  set2.add('hey');
  assertThrowsJsUnitException(function() {
    assertObjectEquals(set1, set2);
  });
}

function testAssertObjectEqualsMap() {
  class FooClass {}

  const map1 = new Map([
    ['foo', 'bar'],
    [1, 2],
    [FooClass, 'bar'],
  ]);
  const map2 = new Map([
    ['foo', 'bar'],
    [1, 2],
    [FooClass, 'bar'],
  ]);

  assertObjectEquals('maps should be equal', map1, map2);

  map1.set('hi', 'hey');
  assertThrowsJsUnitException(function() {
    assertObjectEquals(map1, map2);
  });
}

const SUPPORTS_TYPED_ARRAY =
    typeof Uint8Array === 'function' && typeof Uint8Array.of === 'function';

function testAssertObjectEqualsTypedArrays() {
  if (!SUPPORTS_TYPED_ARRAY) return;  // not supported in IE<11

  assertObjectEquals(
      'Float32Arrays should be equal', Float32Array.of(1, 2, 3),
      Float32Array.of(1, 2, 3));
  assertObjectEquals(
      'Float64Arrays should be equal', Float64Array.of(1, 2, 3),
      Float64Array.of(1, 2, 3));
  assertObjectEquals(
      'Int8Arrays should be equal', Int8Array.of(1, 2, 3),
      Int8Array.of(1, 2, 3));
  assertObjectEquals(
      'Int16Arrays should be equal', Int16Array.of(1, 2, 3),
      Int16Array.of(1, 2, 3));
  assertObjectEquals(
      'Int32Arrays should be equal', Int32Array.of(1, 2, 3),
      Int32Array.of(1, 2, 3));
  assertObjectEquals(
      'Uint8Arrays should be equal', Uint8Array.of(1, 2, 3),
      Uint8Array.of(1, 2, 3));
  assertObjectEquals(
      'Uint8ClampedArrays should be equal', Uint8ClampedArray.of(1, 2, 3),
      Uint8ClampedArray.of(1, 2, 3));
  assertObjectEquals(
      'Uint16Arrays should be equal', Uint16Array.of(1, 2, 3),
      Uint16Array.of(1, 2, 3));
  assertObjectEquals(
      'Uint32Arrays should be equal', Uint32Array.of(1, 2, 3),
      Uint32Array.of(1, 2, 3));

  assertThrowsJsUnitException(() => {
    assertObjectNotEquals(Uint8Array.of(1, 2), Uint8Array.of(1, 2));
  });
}

function testAssertObjectEqualsTypedArrayDifferentBacking() {
  if (!SUPPORTS_TYPED_ARRAY) return;  // not supported in IE<11

  const buf1 = new ArrayBuffer(3 * Uint16Array.BYTES_PER_ELEMENT);
  const buf2 = new ArrayBuffer(4 * Uint16Array.BYTES_PER_ELEMENT);
  const arr1 = new Uint16Array(buf1, 0, 3);
  const arr2 = new Uint16Array(buf2, Uint16Array.BYTES_PER_ELEMENT, 3);
  for (let i = 0; i < arr1.length; ++i) {
    arr1[i] = arr2[i] = i * 2 + 1;
  }
  assertObjectEquals(
      'TypedArrays with different backing buffer lengths should be equal',
      Uint16Array.of(1, 3, 5), Uint16Array.of(0, 1, 3, 5, 7).subarray(1, 4));
}

function testAssertObjectNotEqualsMutatedTypedArray() {
  if (!SUPPORTS_TYPED_ARRAY) return;  // not supported in IE<11

  const arr1 = Int8Array.of(2, -5, 7);
  const arr2 = Int8Array.from(arr1);
  assertObjectEquals('TypedArrays should be equal', arr1, arr2);
  ++arr1[1];
  assertObjectNotEquals('Mutated TypedArray should not be equal', arr1, arr2);
}

function testAssertObjectNotEqualsDifferentTypedArrays() {
  if (!SUPPORTS_TYPED_ARRAY) return;  // not supported in IE<11

  assertObjectNotEquals(
      'Float32Array and Float64Array should not be equal',
      Float32Array.of(1, 2, 3), Float64Array.of(1, 2, 3));
  assertObjectNotEquals(
      'Float32Array and Int32Array should not be equal',
      Float32Array.of(1, 2, 3), Int32Array.of(1, 2, 3));
  assertObjectNotEquals(
      'Int8Array and Int16Array should not be equal', Int8Array.of(1, 2, 3),
      Int16Array.of(1, 2, 3));
  assertObjectNotEquals(
      'Int16Array and Uint16Array should not be equal', Int16Array.of(1, 2, 3),
      Uint16Array.of(1, 2, 3));
  assertObjectNotEquals(
      'Int32Array and Uint8Array should not be equal', Int8Array.of(1, 2, 3),
      Uint8Array.of(1, 2, 3));
  assertObjectNotEquals(
      'Uint8Array and Uint8ClampedArray should not be equal',
      Uint8Array.of(1, 2, 3), Uint8ClampedArray.of(1, 2, 3));

  assertThrowsJsUnitException(() => {
    assertObjectEquals(Uint8Array.of(1, 2), Uint16Array.of(1, 2));
  });
}

function testAssertObjectBigIntTypedArrays() {
  if (typeof BigInt64Array !== 'function') return;  // not supported pre-ES2020

  // Check equality.
  assertObjectEquals(
      'BigInt64Arrays should be equal',
      BigInt64Array.of(BigInt(1), BigInt(2), BigInt(3)),
      BigInt64Array.of(BigInt(1), BigInt(2), BigInt(3)));
  assertObjectEquals(
      'BigUint64Arrays should be equal',
      BigUint64Array.of(BigInt(1), BigInt(2), BigInt(3)),
      BigUint64Array.of(BigInt(1), BigInt(2), BigInt(3)));

  // Check mutation.
  const arr1 = BigInt64Array.of(BigInt(2), BigInt(-5), BigInt(7));
  const arr2 = BigInt64Array.from(arr1);
  assertObjectEquals('BigInt64Arrays should be equal', arr1, arr2);
  ++arr1[1];
  assertObjectNotEquals(
      'Mutated BigInt64Array should not be equal', arr1, arr2);

  // Check different types are not equal.
  assertObjectNotEquals(
      'BigInt64Array and BigUint64Array should not equal',
      BigInt64Array.of(BigInt(1), BigInt(2), BigInt(3)),
      BigUint64Array.of(BigInt(1), BigInt(2), BigInt(3)));
}

function testAssertObjectNotEqualsTypedArrayContents() {
  if (!SUPPORTS_TYPED_ARRAY) return;  // not supported in IE<11
  assertObjectNotEquals(
      'Different Uint16Array contents should not equal',
      Uint16Array.of(1, 2, 3), Uint16Array.of(1, 3, 2));
  assertObjectNotEquals(
      'Different Float32Array contents should not equal',
      Float32Array.of(1.2, 2.4, 3.8), Float32Array.of(1.2, 2.3, 3.8));

  assertThrowsJsUnitException(() => {
    assertObjectEquals(Uint8Array.of(1, 2), Uint8Array.of(3, 2));
  });
}

function testAssertObjectNotEqualsTypedArrayOneExtra() {
  if (!SUPPORTS_TYPED_ARRAY) return;  // not supported in IE<11
  assertObjectNotEquals(
      'Uint8ClampedArray with extra element should not equal',
      Uint8ClampedArray.of(1, 2, 3), Uint8ClampedArray.of(1, 2, 3, 4));
  assertObjectNotEquals(
      'Float32Array with extra element should not equal',
      Float32Array.of(1, 2, 3), Float32Array.of(1, 2, 3, 4));
}

function testAssertObjectEqualsIterNoEquals() {
  // an object with an iterator but no equals() and no map_ cannot
  // be compared
  /** @constructor */
  function Thing() { this.what = []; }
  Thing.prototype.add = function(n, v) { this.what.push(n + '@' + v); };
  Thing.prototype.get = function(n) {
    const m = new RegExp('^' + n + '@(.*)$', '');
    for (let i = 0; i < this.what.length; ++i) {
      const match = this.what[i].match(m);
      if (match) {
        return match[1];
      }
    }
    return null;
  };
  Thing.prototype.__iterator__ = function() {
    const iter = new goog.iter.Iterator;
    iter.index = 0;
    iter.thing = this;
    iter.next = function() {
      if (this.index < this.thing.what.length) {
        return this.thing.what[this.index++].split('@')[0];
      } else {
        throw goog.iter.StopIteration;
      }
    };
    return iter;
  };

  const thing1 = new Thing();
  thing1.name = 'thing1';
  const thing2 = new Thing();
  thing2.name = 'thing2';
  thing1.add('red', 'fish');
  thing1.add('blue', 'fish');

  thing2.add('red', 'fish');
  thing2.add('blue', 'fish');

  assertThrowsJsUnitException(function() {
    assertObjectEquals(thing1, thing2);
  });
}

function testAssertObjectEqualsWithDates() {
  const date = new Date(2010, 0, 1);
  const dateWithMilliseconds = new Date(2010, 0, 1, 0, 0, 0, 1);
  assertObjectEquals(new Date(2010, 0, 1), date);
  assertThrowsJsUnitException(
      goog.partial(assertObjectEquals, date, dateWithMilliseconds));
}

function testAssertObjectEqualsSparseArrays() {
  const arr1 = [, 2, , 4];
  const arr2 = [1, 2, 3, 4, 5];

  // Sparse arrays should not be equal
  assertThrowsJsUnitException(function() {
    assertObjectEquals(arr1, arr2);
  });

  // Sparse arrays should not be equal
  assertThrowsJsUnitException(function() {
    assertObjectEquals(arr2, arr1);
  });

  let a1 = [];
  let a2 = [];
  a2[1.8] = undefined;
  // Empty slots only equal `undefined` for natural-number array keys`
  assertThrowsJsUnitException(function() {
    assertObjectEquals(a1, a2);
  });

  a1 = [];
  a2 = [];
  a2[-999] = undefined;
  // Empty slots only equal `undefined` for natural-number array keys`
  assertThrowsJsUnitException(function() {
    assertObjectEquals(a1, a2);
  });
}

function testAssertObjectEqualsSparseArrays2() {
  // On IE6-8, the expression "1 in [4,undefined]" evaluates to false,
  // but true on other browsers. FML. This test verifies a regression
  // where IE reported that arr4 was not equal to arr1 or arr2.
  const arr1 = [1, , 3];
  const arr2 = [1, undefined, 3];
  const arr3 = goog.array.clone(arr1);
  const arr4 = [];
  arr4.push(1, undefined, 3);

  // Assert that all those arrays are equivalent pairwise.
  const arrays = [arr1, arr2, arr3, arr4];
  for (let i = 0; i < arrays.length; i++) {
    for (let j = 0; j < arrays.length; j++) {
      assertArrayEquals(arrays[i], arrays[j]);
    }
  }
}

function testAssertObjectEqualsNestedPropertyMessage() {
  assertThrowsJsUnitException(function() {
    assertObjectEquals(
        {a: 'abc', b: 4, array: [1, 2, 3, {nested: [2, 3, 4]}]},
        {a: 'bcd', b: '4', array: [1, 5, 3, {nested: [2, 3, 4, 5]}]});
  }, `Expected <[object Object]> (Object) but was <[object Object]> (Object)
   a: Expected <abc> (String) but was <bcd> (String)
   b: Expected <4> (Number) but was <4> (String)
   array[1]: Expected <2> (Number) but was <5> (Number)
   array[3].nested: Expected 3-element array but got a 4-element array`);
}

function testAssertObjectEqualsRootDifference() {
  assertThrowsJsUnitException(function() {
    assertObjectEquals([1], [1, 2]);
  }, `Expected <1> (Array) but was <1,2> (Array)
   Expected 1-element array but got a 2-element array`);

  assertThrowsJsUnitException(function() {
    assertObjectEquals('a', 'b');
  }, 'Expected <a> (String) but was <b> (String)');

  assertThrowsJsUnitException(function() {
    assertObjectEquals([], {});
  }, 'Expected <> (Array) but was <[object Object]> (Object)');
}

function testAssertObjectEqualsArraysWithExtraProps() {
  const arr1 = [1];
  const arr2 = [1];
  arr2.foo = 3;

  assertThrowsJsUnitException(function() {
    assertObjectEquals(arr1, arr2);
  });

  assertThrowsJsUnitException(function() {
    assertObjectEquals(arr2, arr1);
  });
}

function testAssertSameElementsOnArray() {
  assertSameElements([1, 2], [2, 1]);
  assertSameElements('Good assertion', [1, 2], [2, 1]);
  assertSameElements('Good assertion with duplicates', [1, 1, 2], [2, 1, 1]);
  assertThrowsJsUnitException(function() {
    assertSameElements([1, 2], [1]);
  }, 'Expected 2 elements: [1,2], got 1 elements: [1]');
  assertThrowsJsUnitException(function() {
    assertSameElements('Should match', [1, 2], [1]);
  }, 'Should match\nExpected 2 elements: [1,2], got 1 elements: [1]');
  assertThrowsJsUnitException(function() {
    assertSameElements([1, 2], [1, 3]);
  }, 'Expected [1,2], got [1,3]');
  assertThrowsJsUnitException(function() {
    assertSameElements('Should match', [1, 2], [1, 3]);
  }, 'Should match\nExpected [1,2], got [1,3]');
  assertThrowsJsUnitException(function() {
    assertSameElements([1, 1, 2], [2, 2, 1]);
  }, 'Expected [1,1,2], got [2,2,1]');
}

function testAssertSameElementsOnArrayLike() {
  assertSameElements({0: 0, 1: 1, length: 2}, {length: 2, 1: 1, 0: 0});
  assertThrowsJsUnitException(function() {
    assertSameElements({0: 0, 1: 1, length: 2}, {0: 0, length: 1});
  }, 'Expected 2 elements: [0,1], got 1 elements: [0]');
}

function testAssertSameElementsWithBadArguments() {
  const ex = assertThrowsJsUnitException(
      /** @suppress {checkTypes} */
      function() {
        assertSameElements([], new goog.structs.Set());
      });
  assertContains('actual', ex.toString());
  assertContains('array-like or iterable', ex.toString());
}

function testAssertSameElementsWithIterables() {
  const s = new Set([1, 2, 3]);
  assertSameElements({0: 3, 1: 2, 2: 1, length: 3}, s);
  assertSameElements(s, {0: 3, 1: 2, 2: 1, length: 3});
  assertSameElements([], new Set());
  assertSameElements(new Set(), []);

  assertThrowsJsUnitException(
      () => assertSameElements([1, 1], new Set([1, 1])));
  assertThrowsJsUnitException(
      () => assertSameElements(new Set([1, 1]), [1, 1]));

  assertThrowsJsUnitException(
      () => assertSameElements([1, 3], new Set([1, 2])));
  assertThrowsJsUnitException(
      () => assertSameElements(new Set([1, 2]), [1, 3]));
}

const implicitlyTrue = [true, 1, -1, ' ', 'string', Infinity, new Object()];

const implicitlyFalse = [false, 0, '', null, undefined, NaN];

function testAssertEvaluatesToTrue() {
  assertEvaluatesToTrue(true);
  assertEvaluatesToTrue('', true);
  assertEvaluatesToTrue('Good assertion', true);
  assertThrowsJsUnitException(function() {
    assertEvaluatesToTrue(false);
  }, 'Expected to evaluate to true');
  assertThrowsJsUnitException(function() {
    assertEvaluatesToTrue('Should be true', false);
  }, 'Should be true\nExpected to evaluate to true');
  for (let i = 0; i < implicitlyTrue.length; i++) {
    assertEvaluatesToTrue(
        String('Test ' + implicitlyTrue[i] + ' [' + i + ']'),
        implicitlyTrue[i]);
  }
  for (let i = 0; i < implicitlyFalse.length; i++) {
    assertThrowsJsUnitException(function() {
      assertEvaluatesToTrue(implicitlyFalse[i]);
    }, 'Expected to evaluate to true');
  }
}

function testAssertEvaluatesToFalse() {
  assertEvaluatesToFalse(false);
  assertEvaluatesToFalse('Good assertion', false);
  assertThrowsJsUnitException(function() {
    assertEvaluatesToFalse(true);
  }, 'Expected to evaluate to false');
  assertThrowsJsUnitException(function() {
    assertEvaluatesToFalse('Should be false', true);
  }, 'Should be false\nExpected to evaluate to false');
  for (let i = 0; i < implicitlyFalse.length; i++) {
    assertEvaluatesToFalse(
        String('Test ' + implicitlyFalse[i] + ' [' + i + ']'),
        implicitlyFalse[i]);
  }
  for (let i = 0; i < implicitlyTrue.length; i++) {
    assertThrowsJsUnitException(function() {
      assertEvaluatesToFalse(implicitlyTrue[i]);
    }, 'Expected to evaluate to false');
  }
}

function testAssertHTMLEquals() {
  // TODO
}

function testAssertHashEquals() {
  assertHashEquals({a: 1, b: 2}, {b: 2, a: 1});
  assertHashEquals('Good assertion', {a: 1, b: 2}, {b: 2, a: 1});
  assertHashEquals({a: undefined}, {a: undefined});
  // Missing key.
  assertThrowsJsUnitException(function() {
    assertHashEquals({a: 1, b: 2}, {a: 1});
  }, 'Expected hash had key b that was not found');
  assertThrowsJsUnitException(function() {
    assertHashEquals('Should match', {a: 1, b: 2}, {a: 1});
  }, 'Should match\nExpected hash had key b that was not found');
  assertThrowsJsUnitException(function() {
    assertHashEquals({a: undefined}, {});
  }, 'Expected hash had key a that was not found');
  // Not equal key.
  assertThrowsJsUnitException(function() {
    assertHashEquals({a: 1}, {a: 5});
  }, 'Value for key a mismatch - expected = 1, actual = 5');
  assertThrowsJsUnitException(function() {
    assertHashEquals('Should match', {a: 1}, {a: 5});
  }, 'Should match\nValue for key a mismatch - expected = 1, actual = 5');
  assertThrowsJsUnitException(function() {
    assertHashEquals({a: undefined}, {a: 1});
  }, 'Value for key a mismatch - expected = undefined, actual = 1');
  // Extra key.
  assertThrowsJsUnitException(function() {
    assertHashEquals({a: 1}, {a: 1, b: 1});
  }, 'Actual hash had key b that was not expected');
  assertThrowsJsUnitException(function() {
    assertHashEquals('Should match', {a: 1}, {a: 1, b: 1});
  }, 'Should match\nActual hash had key b that was not expected');
}

function testAssertRoughlyEquals() {
  assertRoughlyEquals(1, 1, 0);
  assertRoughlyEquals('Good assertion', 1, 1, 0);
  assertRoughlyEquals(1, 1.1, 0.11);
  assertRoughlyEquals(1.1, 1, 0.11);
  assertThrowsJsUnitException(function() {
    assertRoughlyEquals(1, 1.1, 0.05);
  }, 'Expected 1, but got 1.1 which was more than 0.05 away');
  assertThrowsJsUnitException(function() {
    assertRoughlyEquals('Close enough', 1, 1.1, 0.05);
  }, 'Close enough\nExpected 1, but got 1.1 which was more than 0.05 away');
}

function testAssertContainsForArrays() {
  assertContains(1, [1, 2, 3]);
  assertContains('Should contain', 1, [1, 2, 3]);
  assertThrowsJsUnitException(function() {
    assertContains(4, [1, 2, 3]);
  }, 'Expected \'1,2,3\' to contain \'4\'');
  assertThrowsJsUnitException(function() {
    assertContains('Should contain', 4, [1, 2, 3]);
  }, 'Should contain\nExpected \'1,2,3\' to contain \'4\'');
  // assertContains uses ===.
  const o = new Object();
  assertContains(o, [o, 2, 3]);
  assertThrowsJsUnitException(function() {
    assertContains(o, [1, 2, 3]);
  }, 'Expected \'1,2,3\' to contain \'[object Object]\'');
}

function testAssertNotContainsForArrays() {
  assertNotContains(4, [1, 2, 3]);
  assertNotContains('Should not contain', 4, [1, 2, 3]);
  assertThrowsJsUnitException(function() {
    assertNotContains(1, [1, 2, 3]);
  }, 'Expected \'1,2,3\' not to contain \'1\'');
  assertThrowsJsUnitException(function() {
    assertNotContains('Should not contain', 1, [1, 2, 3]);
  }, "Should not contain\nExpected '1,2,3' not to contain '1'");
  // assertNotContains uses ===.
  const o = new Object();
  assertNotContains({}, [o, 2, 3]);
  assertThrowsJsUnitException(function() {
    assertNotContains(o, [o, 2, 3]);
  }, 'Expected \'[object Object],2,3\' not to contain \'[object Object]\'');
}

function testAssertContainsForStrings() {
  assertContains('ignored msg', 'abc', 'zabcd');
  assertContains('abc', 'abc');
  assertContains('', 'abc');
  assertContains('', '');
  assertThrowsJsUnitException(function() {
    assertContains('msg', 'abc', 'bcd');
  }, 'msg\nExpected \'bcd\' to contain \'abc\'');
  assertThrowsJsUnitException(function() {
    assertContains('a', '');
  }, 'Expected \'\' to contain \'a\'');
}

function testAssertNotContainsForStrings() {
  assertNotContains('ignored msg', 'abc', 'bcd');
  assertNotContains('a', '');
  assertThrowsJsUnitException(function() {
    assertNotContains('msg', 'abc', 'zabcd');
  }, 'msg\nExpected \'zabcd\' not to contain \'abc\'');
  assertThrowsJsUnitException(function() {
    assertNotContains('abc', 'abc');
  }, 'Expected \'abc\' not to contain \'abc\'');
  assertThrowsJsUnitException(function() {
    assertNotContains('', 'abc');
  }, 'Expected \'abc\' not to contain \'\'');
}

/**
 * Tests `assertContains` and 'assertNotContains` with an arbitrary type that
 * has a custom `indexOf`.
 */
function testAssertContainsAndAssertNotContainsOnCustomObjectWithIndexof() {
  const valueContained = {toString: () => 'I am in'};
  const valueNotContained = {toString: () => 'I am out'};
  const container = {
    indexOf: (value) => value === valueContained ? 1234 : -1,
    toString: () => 'I am a container',
  };
  assertContains('ignored message', valueContained, container);
  assertNotContains('ignored message', valueNotContained, container);
  assertThrowsJsUnitException(function() {
    assertContains('msg', valueNotContained, container);
  }, 'msg\nExpected \'I am a container\' to contain \'I am out\'');
  assertThrowsJsUnitException(function() {
    assertNotContains('msg', valueContained, container);
  }, 'msg\nExpected \'I am a container\' not to contain \'I am in\'');
}

function testAssertRegExp() {
  const a = 'I like turtles';
  assertRegExp(/turtles$/, a);
  assertRegExp('turtles$', a);
  assertRegExp('Expected subject to be about turtles', /turtles$/, a);
  assertRegExp('Expected subject to be about turtles', 'turtles$', a);

  const b = 'Hello';
  assertThrowsJsUnitException(function() {
    assertRegExp(/turtles$/, b);
  }, 'Expected \'Hello\' to match RegExp /turtles$/');
  assertThrowsJsUnitException(function() {
    assertRegExp('turtles$', b);
  }, 'Expected \'Hello\' to match RegExp /turtles$/');
}

function testAssertThrows() {
  assertThrowsJsUnitException(() => {
    assertThrows(
        'assertThrows should not pass with null param',
        /** @type {?} */ (null));
  });

  assertThrowsJsUnitException(() => {
    assertThrows(
        'assertThrows should not pass with undefined param',
        /** @type {?} */ (undefined));
  });

  assertThrowsJsUnitException(() => {
    assertThrows(
        'assertThrows should not pass with number param', /** @type {?} */ (1));
  });

  assertThrowsJsUnitException(() => {
    assertThrows(
        'assertThrows should not pass with string param',
        /** @type {?} */ ('string'));
  });

  assertThrowsJsUnitException(() => {
    assertThrows(
        'assertThrows should not pass with object param',
        /** @type {?} */ ({}));
  });

  let error;
  try {
    error = assertThrows('valid function throws Error', function() {
      throw new Error('test');
    });
  } catch (e) {
    fail('assertThrows incorrectly doesn\'t detect a thrown exception');
  }
  assertEquals('error message', 'test', error.message);

  let stringError;
  try {
    stringError =
        assertThrows('valid function throws string error', function() {
          throw 'string error test';
        });
  } catch (e) {
    fail('assertThrows doesn\'t detect a thrown string exception');
  }
  assertEquals('string error', 'string error test', stringError);
}

function testAssertThrowsThrowsIfJsUnitException() {
  // Asserts that assertThrows will throw a JsUnitException if the method
  // passed to assertThrows throws a JsUnitException of its own. assertThrows
  // should not be used for catching JsUnitExceptions with
  // "failOnUnreportedAsserts" enabled.
  const e = assertThrowsJsUnitException(function() {
    assertThrows(function() {
      // We need to invalidate this exception so it's not flagged as a
      // legitimate failure by the test framework. The only way to get at the
      // exception thrown by assertTrue is to catch it so we can invalidate it.
      // We then need to rethrow it so the surrounding assertThrows behaves as
      // expected.
      try {
        assertTrue(false);
      } catch (ex) {
        goog.testing.TestCase.getActiveTestCase().invalidateAssertionException(
            ex);
        throw ex;
      }
    });
  });
  assertContains(
      'Function passed to assertThrows caught a JsUnitException', e.message);

}

function testAssertThrowsJsUnitException() {
  let error = assertThrowsJsUnitException(function() {
    assertTrue(false);
  });
  assertEquals('Call to assertTrue(boolean) with false', error.message);

  error = assertThrowsJsUnitException(function() {
    assertThrowsJsUnitException(function() { throw new Error('fail'); });
  });
  assertEquals('Call to fail()\nExpected a JsUnitException', error.message);

  error = assertThrowsJsUnitException(function() {
    assertThrowsJsUnitException(goog.nullFunction);
  });
  assertEquals('Expected a failure', error.message);
}

function testAssertNotThrows() {
  if (goog.userAgent.product.SAFARI) {
    // TODO(b/20733468): Disabled so we can get the rest of the Closure test
    // suite running in a continuous build. Will investigate later.
    return;
  }

  assertThrowsJsUnitException(() => {
    assertNotThrows(
        'assertNotThrows should not pass with null param',
        /** @type {?} */ (null));
  });


  assertThrowsJsUnitException(() => {
    assertNotThrows(
        'assertNotThrows should not pass with undefined param',
        /** @type {?} */ (undefined));
  });

  assertThrowsJsUnitException(() => {
    assertNotThrows(
        'assertNotThrows should not pass with number param',
        /** @type {?} */ (1));
  });

  assertThrowsJsUnitException(() => {
    assertNotThrows(
        'assertNotThrows should not pass with string param',
        /** @type {?} */ ('string'));
  });


  assertThrowsJsUnitException(() => {
    assertNotThrows(
        'assertNotThrows should not pass with object param',
        /** @type {?} */ ({}));
  });


  let result;
  try {
    result =
        assertNotThrows('valid function', function() { return 'some value'; });
  } catch (e) {
    // Shouldn't be here: throw exception.
    fail('assertNotThrows returned failure on a valid function');
  }
  assertEquals(
      'assertNotThrows should return the result of the function.', 'some value',
      result);

  assertThrowsJsUnitException(() => {
    assertNotThrows('non valid error throwing function', function() {
      throw new Error('a test error exception');
    });
  });
}

async function testAssertRejects_nonThenables() {
  assertThrowsJsUnitException(() => {
    assertRejects(
        'assertRejects should not pass with null param',
        /** @type {?} */ (null));
  });

  assertThrowsJsUnitException(() => {
    assertRejects(
        'assertRejects should not pass with undefined param',
        /** @type {?} */ (undefined));
  });

  assertThrowsJsUnitException(() => {
    assertRejects(
        'assertRejects should not pass with number param',
        /** @type {?} */ (1));
  });

  assertThrowsJsUnitException(() => {
    assertRejects(
        'assertRejects should not pass with string param',
        /** @type {?} */ ('string'));
  });

  assertThrowsJsUnitException(() => {
    assertRejects(
        'assertRejects should not pass with object param with no then property',
        /** @type {?} */ ({}));
  });
}

function testAssertRejects_deferred() {
  return internalTestAssertRejects(true, (fn) => {
    const d = new goog.async.Deferred();
    try {
      fn((val) => d.callback(), (err) => d.errback(err));
    } catch (e) {
      d.errback(e);
    }
    return d;
  });
}

function testAssertRejects_googPromise() {
  return internalTestAssertRejects(true, (fn) => new goog.Promise(fn));
}

function testAssertRejects_promise() {
  return internalTestAssertRejects(false, (fn) => new Promise(fn));
}

function testAssertRejects_asyncFunction_awaitingGoogPromise() {
  return internalTestAssertRejects(true, async (fn) => {
    await new goog.Promise(fn);
  });
}

function testAssertRejects_asyncFunction_awaitingPromise() {
  return internalTestAssertRejects(false, async (fn) => {
    await new Promise(fn);
  });
}

function testAssertRejects_asyncFunction_thatThrows() {
  return internalTestAssertRejects(false, async (fn) => {
    fn(() => {}, (err) => {
      throw err;
    });
  });
}

/**
 * Runs test suite (function) for a `Thenable` implementation covering
 * rejection.
 *
 * @param {boolean} swallowUnhandledRejections
 * @param {function(function(function(?), function(?))): !IThenable<?>} factory
 */
async function internalTestAssertRejects(swallowUnhandledRejections, factory) {
  try {
    // TODO(b/136116638): Stop the unhandled rejection handler from firing
    // rather than swallowing the errors.
    if (swallowUnhandledRejections) {
      goog.Promise.setUnhandledRejectionHandler(goog.nullFunction);
    }

    let e;
    e = await assertRejects(
        'valid IThenable constructor throws Error', factory(() => {
          throw new Error('test0');
        }));
    assertEquals('test0', e.message);

    e = await assertRejects(
        'valid IThenable constructor throws string error', factory(() => {
          throw 'test1';
        }));
    assertEquals('test1', e);

    e = await assertRejects(
        'valid IThenable rejects Error', factory((_, reject) => {
          reject(new Error('test2'));
        }));
    assertEquals('test2', e.message);

    e = await assertRejects(
        'valid IThenable rejects string error', factory((_, reject) => {
          reject('test3');
        }));
    assertEquals('test3', e);

    e = await assertRejects(
        'assertRejects should fail with a resolved thenable', (async () => {
          await assertRejects(factory((resolve) => resolve(undefined)));
          fail('should always throw.');
        })());
    assertEquals(
        'IThenable passed into assertRejects did not reject', e.message);
    // Record this as an expected assertion: go/failonunreportedasserts
    goog.testing.TestCase.invalidateAssertionException(/** @type {?} */ (e));
  } finally {
    // restore the default exception handler.
    goog.Promise.setUnhandledRejectionHandler(goog.async.throwException);
  }
}

function testAssertArrayEquals() {
  let a1 = [0, 1, 2];
  let a2 = [0, 1, 2];
  assertArrayEquals('Arrays should be equal', a1, a2);

  // Should have thrown because args are not arrays
  assertThrowsJsUnitException(function() {
    assertArrayEquals(true, true);
  });

  a1 = [0, undefined, 2];
  a2 = [0, , 2];
  // The following test fails unexpectedly. The bug is tracked at
  // http://code.google.com/p/closure-library/issues/detail?id=419
  // assertThrows(
  //     'assertArrayEquals distinguishes undefined items from sparse arrays',
  //     function() {
  //       assertArrayEquals(a1, a2);
  //     });

  // For the record. This behavior will probably change in the future.
  assertArrayEquals(
      'Bug: sparse arrays and undefined items are not distinguished',
      [0, undefined, 2], [0, , 2]);

  // The array elements should be compared with ===
  assertThrowsJsUnitException(function() {
    assertArrayEquals([0], ['0']);
  });

  // Arrays with different length should be different
  assertThrowsJsUnitException(function() {
    assertArrayEquals([0, undefined], [0]);
  });

  a1 = [0];
  a2 = [0];
  a2[-1] = -1;
  assertArrayEquals('Negative indexes are ignored', a1, a2);

  a1 = [0];
  a2 = [0];
  a2[/** @type {?} */ ('extra')] = 1;
  assertArrayEquals(
      'Extra properties are ignored. Use assertObjectEquals to compare them.',
      a1, a2);

  assertArrayEquals(
      'An example where assertObjectEquals would fail in IE.', ['x'],
      'x'.match(/x/g));
}

function testAssertObjectsEqualsDifferentArrays() {
  // Should throw because args are different
  assertThrowsJsUnitException(function() {
    const a1 = ['className1'];
    const a2 = ['className2'];
    assertObjectEquals(a1, a2);
  });
}

function testAssertObjectsEqualsNegativeArrayIndexes() {
  const a2 = [0];
  a2[-1] = -1;
  // The following test fails unexpectedly. The bug is tracked at
  // http://code.google.com/p/closure-library/issues/detail?id=418
  // assertThrows('assertObjectEquals compares negative indexes', function() {
  //   assertObjectEquals(a1, a2);
  // });
}

function testAssertObjectsEqualsDifferentTypeSameToString() {
  assertThrowsJsUnitException(function() {
    const a1 = 'className1';
    const a2 = ['className1'];
    assertObjectEquals(a1, a2);
  });

  assertThrowsJsUnitException(function() {
    const a1 = ['className1'];
    const a2 = {'0': 'className1'};
    assertObjectEquals(a1, a2);
  });

  assertThrowsJsUnitException(function() {
    const a1 = ['className1'];
    const a2 = [['className1']];
    assertObjectEquals(a1, a2);
  });
}

function testAssertObjectsRoughlyEquals() {
  assertObjectRoughlyEquals({'a': 1}, {'a': 1.2}, 0.3);
  assertThrowsJsUnitException(
      function() {
        assertObjectRoughlyEquals({'a': 1}, {'a': 1.2}, 0.1);
      },
      'Expected <[object Object]> (Object) but was <[object Object]> ' +
          '(Object)\n   a: Expected <1> (Number) but was <1.2> (Number) which ' +
          'was more than 0.1 away');
}

function testAssertObjectRoughlyEqualsWithStrings() {
  // Check that objects with string properties are compared properly.
  const obj1 = {'description': [{'colName': 'x1'}]};
  const obj2 = {'description': [{'colName': 'x2'}]};
  assertThrowsJsUnitException(
      function() {
        assertObjectRoughlyEquals(obj1, obj2, 0.00001);
      },
      'Expected <[object Object]> (Object)' +
          ' but was <[object Object]> (Object)' +
          '\n   description[0].colName: Expected <x1> (String) but was <x2> (String)');
  assertThrowsJsUnitException(function() {
    assertObjectRoughlyEquals('x1', 'x2', 0.00001);
  }, 'Expected <x1> (String) but was <x2> (String)');
}

function testFindDifferences_equal() {
  assertNull(goog.testing.asserts.findDifferences(true, true));
  assertNull(goog.testing.asserts.findDifferences(null, null));
  assertNull(goog.testing.asserts.findDifferences(undefined, undefined));
  assertNull(goog.testing.asserts.findDifferences(1, 1));
  assertNull(goog.testing.asserts.findDifferences([1, 'a'], [1, 'a']));
  assertNull(
      goog.testing.asserts.findDifferences([[1, 2], [3, 4]], [[1, 2], [3, 4]]));
  assertNull(
      goog.testing.asserts.findDifferences([{a: 1, b: 2}], [{b: 2, a: 1}]));
  assertNull(goog.testing.asserts.findDifferences(null, null));
  assertNull(goog.testing.asserts.findDifferences(undefined, undefined));
  assertNull(goog.testing.asserts.findDifferences(
      new Map([['a', 1], ['b', 2]]), new Map([['b', 2], ['a', 1]])));
  assertNull(goog.testing.asserts.findDifferences(
      new Set(['a', 'b']), new Set(['b', 'a'])));
}

function testFindDifferences_unequal() {
  assertNotNull(goog.testing.asserts.findDifferences(true, false));
  assertNotNull(
      goog.testing.asserts.findDifferences([{a: 1, b: 2}], [{a: 2, b: 1}]));
  assertNotNull(
      goog.testing.asserts.findDifferences([{a: 1}], [{a: 1, b: [2]}]));
  assertNotNull(
      goog.testing.asserts.findDifferences([{a: 1, b: [2]}], [{a: 1}]));

  assertNotNull(
      'Second map is missing key "a"; first map is missing key "b"',
      goog.testing.asserts.findDifferences(
          new Map([['a', 1]]), new Map([['b', 2]])));
  assertNotNull(
      'Value for key "a" differs by value',
      goog.testing.asserts.findDifferences(
          new Map([['a', '1']]), new Map([['a', '2']])));
  assertNotNull(
      'Value for key "a" differs by type',
      goog.testing.asserts.findDifferences(
          new Map([['a', '1']]), new Map([['a', 1]])));

  assertNotNull(
      'Second set is missing key "a"',
      goog.testing.asserts.findDifferences(
          new Set(['a', 'b']), new Set(['b'])));
  assertNotNull(
      'First set is missing key "b"',
      goog.testing.asserts.findDifferences(
          new Set(['a']), new Set(['a', 'b'])));
  assertNotNull(
      'Values have different types"',
      goog.testing.asserts.findDifferences(new Set(['1']), new Set([1])));
}

function testFindDifferences_arrays_nonNaturalKeys_notConfsuedForSparseness() {
  let actual;
  let expected;

  actual = [];
  actual[1.8] = undefined;
  expected = [];
  assertNotNull(goog.testing.asserts.findDifferences(actual, expected));

  actual = [];
  actual[-1] = undefined;
  expected = [];
  assertNotNull(goog.testing.asserts.findDifferences(actual, expected));
}

function testFindDifferences_objectsAndNull() {
  assertNotNull(goog.testing.asserts.findDifferences({a: 1}, null));
  assertNotNull(goog.testing.asserts.findDifferences(null, {a: 1}));
  assertNotNull(goog.testing.asserts.findDifferences(null, []));
  assertNotNull(goog.testing.asserts.findDifferences([], null));
  assertNotNull(goog.testing.asserts.findDifferences([], undefined));
}

function testFindDifferences_basicCycle() {
  const a = {};
  const b = {};
  a.self = a;
  b.self = b;
  assertNull(goog.testing.asserts.findDifferences(a, b));

  a.unique = 1;
  assertNotNull(goog.testing.asserts.findDifferences(a, b));
}

function testFindDifferences_crossedCycle() {
  const a = {};
  const b = {};
  a.self = b;
  b.self = a;
  assertNull(goog.testing.asserts.findDifferences(a, b));

  a.unique = 1;
  assertNotNull(goog.testing.asserts.findDifferences(a, b));
}

function testFindDifferences_asymmetricCycle() {
  const a = {};
  const b = {};
  const c = {};
  const d = {};
  const e = {};
  a.self = b;
  b.self = a;
  c.self = d;
  d.self = e;
  e.self = c;
  assertNotNull(goog.testing.asserts.findDifferences(a, c));
}

function testFindDifferences_basicCycleArray() {
  const a = [];
  const b = [];
  a[0] = a;
  b[0] = b;
  assertNull(goog.testing.asserts.findDifferences(a, b));

  a[1] = 1;
  assertNotNull(goog.testing.asserts.findDifferences(a, b));
}

function testFindDifferences_crossedCycleArray() {
  const a = [];
  const b = [];
  a[0] = b;
  b[0] = a;
  assertNull(goog.testing.asserts.findDifferences(a, b));

  a[1] = 1;
  assertNotNull(goog.testing.asserts.findDifferences(a, b));
}

function testFindDifferences_asymmetricCycleArray() {
  const a = [];
  const b = [];
  const c = [];
  const d = [];
  const e = [];
  a[0] = b;
  b[0] = a;
  c[0] = d;
  d[0] = e;
  e[0] = c;
  assertNotNull(goog.testing.asserts.findDifferences(a, c));
}

function testFindDifferences_multiCycles() {
  const a = {};
  a.cycle1 = a;
  a.test = {cycle2: a};

  const b = {};
  b.cycle1 = b;
  b.test = {cycle2: b};
  assertNull(goog.testing.asserts.findDifferences(a, b));
}

function testFindDifferences_binaryTree() {
  function createBinTree(depth, root) {
    if (depth == 0) {
      return {root: root};
    } else {
      const node = {};
      node.left = createBinTree(depth - 1, root || node);
      node.right = createBinTree(depth - 1, root || node);
      return node;
    }
  }

  // TODO(gboyer,user): This test does not terminate with the current
  // algorithm. Can be enabled when (if) the algorithm is improved.
  // assertNull(goog.testing.asserts.findDifferences(
  //    createBinTree(5, null), createBinTree(5, null)));
  assertNotNull(goog.testing.asserts.findDifferences(
      createBinTree(4, null), createBinTree(5, null)));
}

function testStringForWindowIE() {
  if (goog.userAgent.IE && !goog.userAgent.isVersionOrHigher('8')) {
    // NOTE(user): This test sees of we are being affected by a JScript bug
    // in try/finally handling. This bug only affects the lowest try/finally
    // block in the stack. Calling this function via VBScript allows
    // us to run the test synchronously in an empty JS stack.
    window.execScript('stringForWindowIEHelper()', 'vbscript');
    assertEquals('<[object]> (Object)', window.stringForWindowIEResult);
  }
}

function testStringSamePrefix() {
  assertThrowsJsUnitException(
      function() {
        assertEquals('abcdefghi', 'abcdefghx');
      },
      'Expected <abcdefghi> (String) but was <abcdefghx> (String)\n' +
          'Difference was at position 8. Expected [...ghi] vs. actual [...ghx]');
}

function testStringSameSuffix() {
  assertThrowsJsUnitException(
      function() {
        assertEquals('xbcdefghi', 'abcdefghi');
      },
      'Expected <xbcdefghi> (String) but was <abcdefghi> (String)\n' +
          'Difference was at position 0. Expected [xbc...] vs. actual [abc...]');
}

function testStringLongComparedValues() {
  assertThrowsJsUnitException(
      function() {
        assertEquals(
            'abcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxyz',
            'abcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxyz');
      },
      'Expected\n' +
          '<abcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxyz> (String)\n' +
          'but was\n' +
          '<abcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxyz> (String)\n' +
          'Difference was at position 40. Expected [...kkklmnopqrstuvwxyz] vs. actual [...kklmnopqrstuvwxyz]');
}

function testStringLongDiff() {
  assertThrowsJsUnitException(
      function() {
        assertEquals(
            'abcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxyz',
            'abc...xyz');
      },
      'Expected\n' +
          '<abcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxyz> (String)\n' +
          'but was\n' +
          '<abc...xyz> (String)\n' +
          'Difference was at position 3. Expected\n' +
          '[...bcdefghijkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkklmnopqrstuvwxy...]\n' +
          'vs. actual\n' +
          '[...bc...xy...]');
}

function testStringDissimilarShort() {
  assertThrowsJsUnitException(function() {
    assertEquals('x', 'y');
  }, 'Expected <x> (String) but was <y> (String)');
}

function testStringDissimilarLong() {
  assertThrowsJsUnitException(function() {
    assertEquals('xxxxxxxxxx', 'yyyyyyyyyy');
  }, 'Expected <xxxxxxxxxx> (String) but was <yyyyyyyyyy> (String)');
}

function testAssertElementsEquals() {
  assertElementsEquals([1, 2], [1, 2]);
  assertElementsEquals([1, 2], {0: 1, 1: 2, length: 2});
  assertElementsEquals('Good assertion', [1, 2], [1, 2]);
  assertThrowsJsUnitException(
      function() {
        assertElementsEquals('Message', [1, 2], [1]);
      },
      'length mismatch: Message\n' +
          'Expected <2> (Number) but was <1> (Number)');
}

function stringForWindowIEHelper() {
  window.stringForWindowIEResult = _displayStringForValue(window);
}

function testDisplayStringForValue() {
  assertEquals('<hello> (String)', _displayStringForValue('hello'));
  assertEquals('<1> (Number)', _displayStringForValue(1));
  assertEquals('<null>', _displayStringForValue(null));
  assertEquals('<undefined>', _displayStringForValue(undefined));
  assertEquals(
      '<hello,,,,1> (Array)',
      _displayStringForValue(['hello', /* array hole */, undefined, null, 1]));
}

function testDisplayStringForValue_exception() {
  assertEquals(
      '<toString failed: foo message> (Object)', _displayStringForValue({
        toString: function() {
          throw new Error('foo message');
        }
      }));
}

function testDisplayStringForValue_cycle() {
  const cycle = ['cycle'];
  cycle.push(cycle);
  assertTrue(
      'Computing string should terminate and result in a reasonable length',
      _displayStringForValue(cycle).length < 1000);
}

function testToArrayForIterable() {
  const s = new Set([3]);
  const arr = goog.testing.asserts.toArray_(s);
  assertEquals(3, arr[0]);
}
