(ns mount.validations
  "Validation helper functions."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.protocols :as protocols]))

;;; Internals

(def ^:private status
  (delay @(resolve 'mount.lite/status)))


;;; Validation functions

(defn state?
  "Test whether an object is a state."
  [obj]
  (satisfies? protocols/IState obj))

(defn defstate? [obj]
  "Test whether an object is a known statevar."
  (contains? (set (keys (@status))) obj))


;;; Conforming functions.

(defn maybe-deref
  "Calls deref on the object, if it is a Var."
  [obj]
  (cond-> obj (var? obj) deref))

(defn maybe-hash-map
  "Creates a hash-map, if the supplied object is a vector."
  [obj]
  (cond->> obj (vector? obj) (apply hash-map)))

(defn map-keys
  "Apply f on all the keys of the map."
  [f map]
  (reduce-kv #(assoc %1 (f %2) %3) {} map))
