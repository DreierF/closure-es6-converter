# Closure Library with ES6 Classes only

This is a fork of the [Closure Library](https://github.com/google/closure-library), a general-purpose library written by Google using type annotations for the [Closure compiler](https://github.com/google/closure-compiler).

The original library has been converted to use ES6 classes only (so no Closure-speficic declarations like `goog.inherits` or `goog.declareClass`).

Note that this still uses the `goog.require` / `goog.provide` / `goog.module` module mechanism and is thus not (yet) independent from the Closure compiler.