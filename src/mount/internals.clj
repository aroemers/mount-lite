(ns mount.internals
  "The core namespace providing the public API."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true}
  (:require [clojure.pprint :refer [simple-dispatch]]))

;;; Internals

(defonce ^:dynamic *system-key*  :default)
(defonce ^:dynamic *system-map*  {})
(defonce ^:dynamic *substitutes* {})

(defonce states  (java.util.LinkedHashSet.))
(defonce systems (atom {}))
(defonce started (atom {}))

(defprotocol IState
  :extend-via-metadata true
  (start* [this])
  (stop* [this])
  (status* [this]))

(defrecord StateVar [name]
  IState
  (start* [this]
    (when (= :stopped (status* this))
      (let [state (or (get *substitutes* this)
                      (:state (meta (resolve name))))
            result (start* state)]
        (swap! systems update *system-key* assoc this result)
        (swap! started update *system-key* assoc this state))))

  (stop* [this]
    (when (= :started (status* this))
      (when-let [state (get-in @started [*system-key* this])]
        (stop* state))
      (swap! systems update *system-key* dissoc this)
      (swap! started update *system-key* dissoc this)))

  (status* [this]
    (if (or (contains? *system-map* this)
            (contains? (get @systems *system-key*) this))
      :started
      :stopped))

  clojure.lang.IDeref
  (deref [this]
    (if (= :started (status* this))
      (if (contains? *system-map* this)
        (get *system-map* this)
        (get-in @systems [*system-key* this]))
      (throw (ex-info (str "Cannot deref state " name " when not started (system " *system-key* ")")
                      {:state this :system *system-key*}))))

  clojure.lang.Named
  (getNamespace [_]
    (namespace name))
  (getName [_]
    (clojure.core/name name))

  Object
  (toString [_]
    (str name)))

(defrecord State [start-fn stop-fn]
  IState
  (start* [_] (start-fn))
  (stop*  [_] (stop-fn)))


;;; Printing

(defmethod print-method StateVar [sv ^java.io.Writer writer]
  (.write writer (str sv)))

(defmethod simple-dispatch StateVar [sv]
  (.write *out* (str sv)))
