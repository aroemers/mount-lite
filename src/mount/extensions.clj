(ns mount.extensions
  "The extension point for mount-lite.

  Extending mount-lite means influencing which states are considered
  when calling start or stop. This is done by registering predicate
  factory functions. These functions are called on start and stop, to
  retrieve a set of predicates. All these predicates need to agree
  that a state should be considered for being started or stopped.

  The predicate factory functions receive a map in the following
  structure:

  {:states (state-1 ...)  ;; sequence of all states, in start order
   :start? true/false     ;; whether start or stop is called
   :up-to  state/nil}     ;; the supplied up-to parameter, if any"
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.validations.extensions :as validations]))

;;; Internals.

(defonce ^:no-doc predicate-factories (atom #{}))

(defn ^:no-doc state-filter
  "Implementation detail; called by mount-lite's core."
  [states start? up-to]
  (let [info {:states states :start? start? :up-to up-to}]
    (apply every-pred (map #(% info) @predicate-factories))))


;;; Public API.

(defn register-predicate-factory
  "Register a predicate factory function to extend mount-lite
  start/stop logic."
  [f]
  (validations/validate-register-predicate-factory f)
  (swap! predicate-factories conj f))
