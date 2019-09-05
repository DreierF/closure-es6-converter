// Copyright 2013 The Closure Library Authors. All Rights Reserved.
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

goog.provide('goog.net.XhrLike');

/**
 * Interface for the common parts of XMLHttpRequest.
 *
 * Mostly copied from externs/w3c_xml.js.
 *
 * @interface
 * @see http://www.w3.org/TR/XMLHttpRequest/
 */
goog.net.XhrLike = class {

	/**
	 * @param {string} method
	 * @param {string} url
	 * @param {?boolean=} opt_async
	 * @param {?string=} opt_user
	 * @param {?string=} opt_password
	 * @see http://www.w3.org/TR/XMLHttpRequest/#the-open()-method
	 */
	open(method, url, opt_async, opt_user, opt_password) {}

	/**
	 * @param {ArrayBuffer|ArrayBufferView|Blob|Document|FormData|string=} opt_data
	 * @see http://www.w3.org/TR/XMLHttpRequest/#the-send()-method
	 */
	send(opt_data) {}

	/**
	 * See link
	 * @see http://www.w3.org/TR/XMLHttpRequest/#the-abort()-method
	 */
	abort() {}

	/**
	 * @param {string} header
	 * @param {string} value
	 * @see http://www.w3.org/TR/XMLHttpRequest/#the-setrequestheader()-method
	 */
	setRequestHeader(header, value) {}

	/**
	 * @param {string} header
	 * @return {string}
	 * @see http://www.w3.org/TR/XMLHttpRequest/#the-getresponseheader()-method
	 */
	getResponseHeader(header) {}

	/**
	 * @return {string}
	 * @see http://www.w3.org/TR/XMLHttpRequest/#the-getallresponseheaders()-method
	 */
	getAllResponseHeaders() {}
};


/**
 * Typedef that refers to either native or custom-implemented XHR objects.
 * @typedef {!goog.net.XhrLike|!XMLHttpRequest}
 */
goog.net.XhrLike.OrNative;


/**
 * @type {function()|null|undefined}
 * @see http://www.w3.org/TR/XMLHttpRequest/#handler-xhr-onreadystatechange
 */
goog.net.XhrLike.prototype.onreadystatechange;


/**
 * @type {?ArrayBuffer|?Blob|?Document|?Object|?string}
 * @see https://xhr.spec.whatwg.org/#response-object
 */
goog.net.XhrLike.prototype.response;


/**
 * @type {string}
 * @see http://www.w3.org/TR/XMLHttpRequest/#the-responsetext-attribute
 */
goog.net.XhrLike.prototype.responseText;


/**
 * @type {string}
 * @see https://xhr.spec.whatwg.org/#the-responsetype-attribute
 */
goog.net.XhrLike.prototype.responseType;


/**
 * @type {Document}
 * @see http://www.w3.org/TR/XMLHttpRequest/#the-responsexml-attribute
 */
goog.net.XhrLike.prototype.responseXML;


/**
 * @type {number}
 * @see http://www.w3.org/TR/XMLHttpRequest/#readystate
 */
goog.net.XhrLike.prototype.readyState;


/**
 * @type {number}
 * @see http://www.w3.org/TR/XMLHttpRequest/#status
 */
goog.net.XhrLike.prototype.status;


/**
 * @type {string}
 * @see http://www.w3.org/TR/XMLHttpRequest/#statustext
 */
goog.net.XhrLike.prototype.statusText;


