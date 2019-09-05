// Copyright 2011 The Closure Library Authors. All Rights Reserved.
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
 * @fileoverview Error classes for the IndexedDB wrapper.
 *
 */



// TODO(b/130421259): We're trying to migrate all ES5 subclasses of Closure
// Library to ES6. In ES6 this cannot be referenced before super is called. This
// file has at least one this before a super call (in ES5) and cannot be
// automatically upgraded to ES6 as a result. Please fix this if you have a
// chance. Note: This can sometimes be caused by not calling the super
// constructor at all. You can run the conversion tool yourself to see what it
// does on this file: blaze run //javascript/refactoring/es6_classes:convert.

goog.provide('goog.db.DomErrorLike');
goog.provide('goog.db.Error');
goog.provide('goog.db.Error.ErrorCode');
goog.provide('goog.db.Error.ErrorName');
goog.provide('goog.db.Error.VersionChangeBlockedError');

goog.require('goog.asserts');
goog.require('goog.debug.Error');


/** @record */
goog.db.DOMErrorLike = function() {};

/** @type {string|undefined} */
goog.db.DOMErrorLike.prototype.name;

/**
 * A database error. Since the stack trace can be unhelpful in an asynchronous
 * context, the error provides a message about where it was produced.
 * @final
 */
goog.db.Error = class extends goog.debug.Error {

  /**
   * @param {number|!DOMError|!goog.db.DOMErrorLike} error The DOMError instance
   *     returned by the browser for Chrome22+, or an error code for previous
   *     versions.
   * @param {string} context A description of where the error occurred.
   * @param {string=} opt_message Additional message.
   */
  constructor(error, context, opt_message) {
    var errorCode = null;
    var internalError = null;
	
	var name = '';
	if (goog.isNumber(error)) {
      errorCode = error;
      internalError = {name: goog.db.Error.getName(errorCode)};
    } else {
      internalError = error;
      errorCode = goog.db.Error.getCode(error.name);
	  name = error.name;
    }
	
	var msg = 'Error ' + context + ': ' + name;
    if (opt_message) {
      msg += ', ' + opt_message;
    }
    super(msg);
	

    /**
     * The code for this error.
     *
     * @type {number}
     */
    this.code = errorCode;

    /**
     * The DOMException as returned by the browser.
     *
     * @type {!goog.db.DOMErrorLike}
     * @private
     */
    this.error_ = internalError;
  }

  /**
   * @return {string} The name of the error.
   */
  getName() {
    return this.error_.name || '';
  }

  /**
   * Translates an error code into a more useful message.
   *
   * @param {number} code Error code.
   * @return {string} A debug message.
   */
  static getMessage(code) {
    switch (code) {
      case goog.db.Error.ErrorCode.UNKNOWN_ERR:
        return 'Unknown error';
      case goog.db.Error.ErrorCode.NON_TRANSIENT_ERR:
        return 'Invalid operation';
      case goog.db.Error.ErrorCode.NOT_FOUND_ERR:
        return 'Required database object not found';
      case goog.db.Error.ErrorCode.CONSTRAINT_ERR:
        return 'Constraint unsatisfied';
      case goog.db.Error.ErrorCode.DATA_ERR:
        return 'Invalid data';
      case goog.db.Error.ErrorCode.NOT_ALLOWED_ERR:
        return 'Operation disallowed';
      case goog.db.Error.ErrorCode.TRANSACTION_INACTIVE_ERR:
        return 'Transaction not active';
      case goog.db.Error.ErrorCode.ABORT_ERR:
        return 'Request aborted';
      case goog.db.Error.ErrorCode.READ_ONLY_ERR:
        return 'Modifying operation not allowed in a read-only transaction';
      case goog.db.Error.ErrorCode.TIMEOUT_ERR:
        return 'Transaction timed out';
      case goog.db.Error.ErrorCode.QUOTA_ERR:
        return 'Database storage space quota exceeded';
      case goog.db.Error.ErrorCode.INVALID_ACCESS_ERR:
        return 'Invalid operation';
      case goog.db.Error.ErrorCode.INVALID_STATE_ERR:
        return 'Invalid state';
      default:
        return 'Unrecognized exception with code ' + code;
    }
  }

  /**
   * Translates an error name to an error code. This is purely kept for backwards
   * compatibility with Chrome21.
   *
   * @param {string|undefined} name The name of the erorr.
   * @return {number} The error code corresponding to the error.
   */
  static getCode(name) {
    switch (name) {
      case goog.db.Error.ErrorName.UNKNOWN_ERR:
        return goog.db.Error.ErrorCode.UNKNOWN_ERR;
      case goog.db.Error.ErrorName.NOT_FOUND_ERR:
        return goog.db.Error.ErrorCode.NOT_FOUND_ERR;
      case goog.db.Error.ErrorName.CONSTRAINT_ERR:
        return goog.db.Error.ErrorCode.CONSTRAINT_ERR;
      case goog.db.Error.ErrorName.DATA_ERR:
        return goog.db.Error.ErrorCode.DATA_ERR;
      case goog.db.Error.ErrorName.TRANSACTION_INACTIVE_ERR:
        return goog.db.Error.ErrorCode.TRANSACTION_INACTIVE_ERR;
      case goog.db.Error.ErrorName.ABORT_ERR:
        return goog.db.Error.ErrorCode.ABORT_ERR;
      case goog.db.Error.ErrorName.READ_ONLY_ERR:
        return goog.db.Error.ErrorCode.READ_ONLY_ERR;
      case goog.db.Error.ErrorName.TIMEOUT_ERR:
        return goog.db.Error.ErrorCode.TIMEOUT_ERR;
      case goog.db.Error.ErrorName.QUOTA_EXCEEDED_ERR:
        return goog.db.Error.ErrorCode.QUOTA_ERR;
      case goog.db.Error.ErrorName.INVALID_ACCESS_ERR:
        return goog.db.Error.ErrorCode.INVALID_ACCESS_ERR;
      case goog.db.Error.ErrorName.INVALID_STATE_ERR:
        return goog.db.Error.ErrorCode.INVALID_STATE_ERR;
      default:
        return goog.db.Error.ErrorCode.UNKNOWN_ERR;
    }
  }

  /**
   * Converts an error code used by the old spec, to an error name used by the
   * latest spec.
   * @see http://www.w3.org/TR/IndexedDB/#exceptions
   *
   * @param {!goog.db.Error.ErrorCode|number} code The error code to convert.
   * @return {!goog.db.Error.ErrorName} The corresponding name of the error.
   */
  static getName(code) {
    switch (code) {
      case goog.db.Error.ErrorCode.UNKNOWN_ERR:
        return goog.db.Error.ErrorName.UNKNOWN_ERR;
      case goog.db.Error.ErrorCode.NOT_FOUND_ERR:
        return goog.db.Error.ErrorName.NOT_FOUND_ERR;
      case goog.db.Error.ErrorCode.CONSTRAINT_ERR:
        return goog.db.Error.ErrorName.CONSTRAINT_ERR;
      case goog.db.Error.ErrorCode.DATA_ERR:
        return goog.db.Error.ErrorName.DATA_ERR;
      case goog.db.Error.ErrorCode.TRANSACTION_INACTIVE_ERR:
        return goog.db.Error.ErrorName.TRANSACTION_INACTIVE_ERR;
      case goog.db.Error.ErrorCode.ABORT_ERR:
        return goog.db.Error.ErrorName.ABORT_ERR;
      case goog.db.Error.ErrorCode.READ_ONLY_ERR:
        return goog.db.Error.ErrorName.READ_ONLY_ERR;
      case goog.db.Error.ErrorCode.TIMEOUT_ERR:
        return goog.db.Error.ErrorName.TIMEOUT_ERR;
      case goog.db.Error.ErrorCode.QUOTA_ERR:
        return goog.db.Error.ErrorName.QUOTA_EXCEEDED_ERR;
      case goog.db.Error.ErrorCode.INVALID_ACCESS_ERR:
        return goog.db.Error.ErrorName.INVALID_ACCESS_ERR;
      case goog.db.Error.ErrorCode.INVALID_STATE_ERR:
        return goog.db.Error.ErrorName.INVALID_STATE_ERR;
      default:
        return goog.db.Error.ErrorName.UNKNOWN_ERR;
    }
  }

  /**
   * Constructs an goog.db.Error instance from an IDBRequest. This abstraction is
   * necessary to provide backwards compatibility with Chrome21.
   *
   * @param {!IDBRequest} request The request that failed.
   * @param {string} message The error message to add to err if it's wrapped.
   * @return {!goog.db.Error} The error that caused the failure.
   */
  static fromRequest(request, message) {
    if ('error' in request) {
      // Chrome 22+
      return new goog.db.Error(goog.asserts.assert(request.error), message);
    } else {
      return new goog.db.Error({name: goog.db.Error.ErrorName.UNKNOWN_ERR}, message);
    }
  }

  /**
   * Constructs an goog.db.Error instance from an DOMException. This abstraction
   * is necessary to provide backwards compatibility with Chrome21.
   *
   * @param {!DOMError|!DOMException} ex The exception that was thrown.
   * @param {string} message The error message to add to err if it's wrapped.
   * @return {!goog.db.Error} The error that caused the failure.
   */
  static fromException(ex, message) {
    if ('name' in ex) {
      // Chrome 22+.
      var errorMessage = message + ': ' + ex.message;
      return new goog.db.Error(ex, errorMessage);
    } else if ('code' in ex) {
      // Chrome 21 and before.
      var errorName = goog.db.Error.getName(ex.code);
      var errorMessage = message + ': ' + ex.message;
      return new goog.db.Error({name: errorName}, errorMessage);
    } else {
      return new goog.db.Error({name: goog.db.Error.ErrorName.UNKNOWN_ERR}, message);
    }
  }
};

/**
 * A specific kind of database error. If a Version Change is unable to proceed
 * due to other open database connections, it will block and this error will be
 * thrown.
 *
 * @final
 */
goog.db.Error.VersionChangeBlockedError = class extends goog.debug.Error {
  constructor() {
    super('Version change blocked');
  }
};

/**
 * Synthetic error codes for database errors, for use when IndexedDB
 * support is not available. This numbering differs in practice
 * from the browser implementations, but it is not meant to be reliable:
 * this object merely ensures that goog.db.Error is loadable on platforms
 * that do not support IndexedDB.
 *
 * @enum {number}
 * @private
 */
goog.db.Error.DatabaseErrorCode_ = {
  UNKNOWN_ERR: 1,
  NON_TRANSIENT_ERR: 2,
  NOT_FOUND_ERR: 3,
  CONSTRAINT_ERR: 4,
  DATA_ERR: 5,
  NOT_ALLOWED_ERR: 6,
  TRANSACTION_INACTIVE_ERR: 7,
  ABORT_ERR: 8,
  READ_ONLY_ERR: 9,
  TRANSIENT_ERR: 10,
  TIMEOUT_ERR: 11,
  QUOTA_ERR: 12,
  INVALID_ACCESS_ERR: 13,
  INVALID_STATE_ERR: 14
};


/**
 * Error codes for database errors.
 * @see http://www.w3.org/TR/IndexedDB/#idl-def-IDBDatabaseException
 *
 * @enum {number}
 * @suppress {missingProperties} Obsolete IndexDb exception objects
 */
goog.db.Error.ErrorCode = {
  UNKNOWN_ERR: (goog.global.IDBDatabaseException ||
                goog.global.webkitIDBDatabaseException ||
                goog.db.Error.DatabaseErrorCode_)
                   .UNKNOWN_ERR,
  NON_TRANSIENT_ERR: (goog.global.IDBDatabaseException ||
                      goog.global.webkitIDBDatabaseException ||
                      goog.db.Error.DatabaseErrorCode_)
                         .NON_TRANSIENT_ERR,
  NOT_FOUND_ERR: (goog.global.IDBDatabaseException ||
                  goog.global.webkitIDBDatabaseException ||
                  goog.db.Error.DatabaseErrorCode_)
                     .NOT_FOUND_ERR,
  CONSTRAINT_ERR: (goog.global.IDBDatabaseException ||
                   goog.global.webkitIDBDatabaseException ||
                   goog.db.Error.DatabaseErrorCode_)
                      .CONSTRAINT_ERR,
  DATA_ERR: (goog.global.IDBDatabaseException ||
             goog.global.webkitIDBDatabaseException ||
             goog.db.Error.DatabaseErrorCode_)
                .DATA_ERR,
  NOT_ALLOWED_ERR: (goog.global.IDBDatabaseException ||
                    goog.global.webkitIDBDatabaseException ||
                    goog.db.Error.DatabaseErrorCode_)
                       .NOT_ALLOWED_ERR,
  TRANSACTION_INACTIVE_ERR: (goog.global.IDBDatabaseException ||
                             goog.global.webkitIDBDatabaseException ||
                             goog.db.Error.DatabaseErrorCode_)
                                .TRANSACTION_INACTIVE_ERR,
  ABORT_ERR: (goog.global.IDBDatabaseException ||
              goog.global.webkitIDBDatabaseException ||
              goog.db.Error.DatabaseErrorCode_)
                 .ABORT_ERR,
  READ_ONLY_ERR: (goog.global.IDBDatabaseException ||
                  goog.global.webkitIDBDatabaseException ||
                  goog.db.Error.DatabaseErrorCode_)
                     .READ_ONLY_ERR,
  TIMEOUT_ERR: (goog.global.IDBDatabaseException ||
                goog.global.webkitIDBDatabaseException ||
                goog.db.Error.DatabaseErrorCode_)
                   .TIMEOUT_ERR,
  QUOTA_ERR: (goog.global.IDBDatabaseException ||
              goog.global.webkitIDBDatabaseException ||
              goog.db.Error.DatabaseErrorCode_)
                 .QUOTA_ERR,
  INVALID_ACCESS_ERR:
      (goog.global.DOMException || goog.db.Error.DatabaseErrorCode_)
          .INVALID_ACCESS_ERR,
  INVALID_STATE_ERR:
      (goog.global.DOMException || goog.db.Error.DatabaseErrorCode_)
          .INVALID_STATE_ERR
};

/**
 * Names of all possible errors as returned from the browser.
 * @see http://www.w3.org/TR/IndexedDB/#exceptions
 * @enum {string}
 */
goog.db.Error.ErrorName = {
  ABORT_ERR: 'AbortError',
  CONSTRAINT_ERR: 'ConstraintError',
  DATA_CLONE_ERR: 'DataCloneError',
  DATA_ERR: 'DataError',
  INVALID_ACCESS_ERR: 'InvalidAccessError',
  INVALID_STATE_ERR: 'InvalidStateError',
  NOT_FOUND_ERR: 'NotFoundError',
  QUOTA_EXCEEDED_ERR: 'QuotaExceededError',
  READ_ONLY_ERR: 'ReadOnlyError',
  SYNTAX_ERROR: 'SyntaxError',
  TIMEOUT_ERR: 'TimeoutError',
  TRANSACTION_INACTIVE_ERR: 'TransactionInactiveError',
  UNKNOWN_ERR: 'UnknownError',
  VERSION_ERR: 'VersionError'
};

