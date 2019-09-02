# Closure Library with ES6 Classes only

This is a fork of the [Closure Library](https://github.com/google/closure-library), a general-purpose library written by Google using type annotations for the [Closure compiler](https://github.com/google/closure-compiler).

The original library has been converted to use ES6 classes only (so no Closure-speficic declarations like `goog.inherits` or `goog.declareClass`).

Note that this still uses the `goog.require` / `goog.provide` / `goog.module` module mechanism and is thus not (yet) independent from the Closure compiler.

# Building

You can run build the library the Closure compiler (current version: `20190820.182245-576`) using this bash command: 

``` bash
./scripts/ci/compile_closure.sh
```

Potential compilation errors will be printed on the console. Otherwise, the process  will exit without any output and exit code 0.