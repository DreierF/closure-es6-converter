# Closure Library to ES6 Converter
This project is a converter that allows to convert ES5 style Javascript classes, 
which use Closure goog.require/goog.provide/goog.module style, to ES6 code which 
uses ES6 Modules.

The main porpuse is to convert the [Google Closure Library](https://developers.google.com/closure/library). However with a bit of 
customization you may also convert your own Closure style 
code. The result of running  the converter on the closure Library can be found 
on [GitHub](https://github.com/DreierF/ts-closure-library) and 
[npm](https://www.npmjs.com/package/ts-closure-library).

Note that we will only accept pull requests for this converter NOT for 
ts-closure-library itself as it is completely generated from this converter.

# How the Converter works

This repository contains a submodule, which is a [fork of the Closure Library](https://github.com/cqse/closure-library/tree/minimal_fixes_on_20191111) with some manual fixes needed to successfully convert it.
- Extract and index the dependency graphy of the Closure Library. (ReaderPass)
- A subgraph that is actually used in our application (will differ for yours) is selected with all its transitive dependencies. The used namespaces are stored in required-namespaces.txt (SelectionPass)
- A bunch a of regex replace operations is applied to the js files to make the input easier to convert (SpecificFixesApplier)
- Cyclic dependencies are removed by merging some files into one (CyclicDependencyRemovalPass)
- ES5 prototype based "classes" are converted to real ES6 classes (Es6ClassConversionPass)
- Refresh the dependency graph based on the cyclic dependency removal pass (ReaderPass)
- goog.require/goog.module are converted to ES6 imports (ConvertingPass)
- Create a copy of the converted files and apply some fixes so that the Typescript compiler will acept them as input (SpecificFixesApplierForDeclaration)
- Run the Typescript compiler to generate .d.ts files for the js files
- Apply some regex replace fixes to the d.ts files (DeclarationFixer)
- Run the Closure compiler on the generated output to ensure the js code is valid

# How to run the Converter

- Run `yarn install` to download Typescript