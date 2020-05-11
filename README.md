This project is a converter that allows to convert ES5 style Javascript classes, 
which use Closure goog.require/goog.provide/goog.module style, to ES6 Code which 
uses ES6 Modules.

The main porpuse is to convert the Google closure library. However with a bit of 
customization you may also make it work to convert you custom  Closure style 
code. The result of running  the converter on the closure Library can be found 
on [GitHub](https://github.com/DreierF/ts-closure-library) and 
[npm](https://www.npmjs.com/package/ts-closure-library).

Note that we will only accept pull requests for this converter not for 
ts-closure-library itself as it is completely generated from this converter.