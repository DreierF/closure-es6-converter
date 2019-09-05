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
 * @fileoverview An abstract base class for transitions. This is a simple
 * interface that allows for playing, pausing and stopping an animation. It adds
 * a simple event model, and animation status.
 */
goog.provide('goog.fx.TransitionBase');
goog.provide('goog.fx.TransitionBase.State');

goog.require('goog.events.EventTarget');
goog.require('goog.fx.Transition');  // Unreferenced: interface

/**
   * Constructor for a transition object.
   *
   * @abstract
   * @struct
   * @implements {goog.fx.Transition}
   * 
   */
goog.fx.TransitionBase = class extends goog.events.EventTarget {
  /**
  * Constructor for a transition object.
  *
  *
   */
  constructor() {
    super();

    /**
     * The internal state of the animation.
     * @type {goog.fx.TransitionBase.State}
     * @private
     */
    this.state_ = goog.fx.TransitionBase.State.STOPPED;

    /**
     * Timestamp for when the animation was started.
     * @type {?number}
     * @protected
     */
    this.startTime = null;

    /**
     * Timestamp for when the animation finished or was stopped.
     * @type {?number}
     * @protected
     */
    this.endTime = null;
  }

  /**
   * Plays the animation.
   *
   * @abstract
   * @param {boolean=} opt_restart Optional parameter to restart the animation.
   * @return {boolean} True iff the animation was started.
   * @override
   * @suppress {checkTypes} (DV)
   */
  play(opt_restart) {}

  /**
   * @abstract
   * Stops the animation.
   *
   * @param {boolean=} opt_gotoEnd Optional boolean parameter to go the the end of
   *     the animation.
   * @override
   */
  stop(opt_gotoEnd) {}

  /**
   * @abstract
   * Pauses the animation.
   */
  pause() {}

  /**
   * Returns the current state of the animation.
   * @return {goog.fx.TransitionBase.State} State of the animation.
   */
  getStateInternal() {
    return this.state_;
  }

  /**
   * Sets the current state of the animation to playing.
   * @protected
   */
  setStatePlaying() {
    this.state_ = goog.fx.TransitionBase.State.PLAYING;
  }

  /**
   * Sets the current state of the animation to paused.
   * @protected
   */
  setStatePaused() {
    this.state_ = goog.fx.TransitionBase.State.PAUSED;
  }

  /**
   * Sets the current state of the animation to stopped.
   * @protected
   */
  setStateStopped() {
    this.state_ = goog.fx.TransitionBase.State.STOPPED;
  }

  /**
   * @return {boolean} True iff the current state of the animation is playing.
   */
  isPlaying() {
    return this.state_ == goog.fx.TransitionBase.State.PLAYING;
  }

  /**
   * @return {boolean} True iff the current state of the animation is paused.
   */
  isPaused() {
    return this.state_ == goog.fx.TransitionBase.State.PAUSED;
  }

  /**
   * @return {boolean} True iff the current state of the animation is stopped.
   */
  isStopped() {
    return this.state_ == goog.fx.TransitionBase.State.STOPPED;
  }

  /**
   * Dispatches the BEGIN event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onBegin() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.BEGIN);
  }

  /**
   * Dispatches the END event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onEnd() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.END);
  }

  /**
   * Dispatches the FINISH event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onFinish() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.FINISH);
  }

  /**
   * Dispatches the PAUSE event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onPause() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.PAUSE);
  }

  /**
   * Dispatches the PLAY event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onPlay() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.PLAY);
  }

  /**
   * Dispatches the RESUME event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onResume() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.RESUME);
  }

  /**
   * Dispatches the STOP event. Sub classes should override this instead
   * of listening to the event, and call this instead of dispatching the event.
   * @protected
   */
  onStop() {
    this.dispatchAnimationEvent(goog.fx.Transition.EventType.STOP);
  }

  /**
   * Dispatches an event object for the current animation.
   * @param {string} type Event type that will be dispatched.
   * @protected
   */
  dispatchAnimationEvent(type) {
    this.dispatchEvent(type);
  }
};



/**
 * Enum for the possible states of an animation.
 * @enum {number}
 */
goog.fx.TransitionBase.State = {
  STOPPED: 0,
  PAUSED: -1,
  PLAYING: 1
};


