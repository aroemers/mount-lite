(ns mount.implementation.statevar
  "Core implementation."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true}
  (:require [clojure.pprint :refer [simple-dispatch]]
            [mount.protocols :as protocols :refer [start stop status]]))

;;; Internals

(defonce ^:dynamic *system-key*  :default)
(defonce ^:dynamic *system-map*  {})
(defonce ^:dynamic *substitutes* {})

(defonce statevars (java.util.LinkedHashMap.))
(defonce systems   (atom {}))
(defonce started   (atom {}))

(defrecord StateVar [name]
  protocols/IState
  (start [this]
    (when (= :stopped (status this))
      (let [state (or (get *substitutes* this)
                      (get statevars this))
            result (start state)]
        (swap! systems update *system-key* assoc this result)
        (swap! started update *system-key* assoc this state))))

  (stop [this]
    (when (= :started (status this))
      (when-let [state (get-in @started [*system-key* this])]
        (stop state))
      (swap! systems update *system-key* dissoc this)
      (swap! started update *system-key* dissoc this)))

  protocols/IStatus
  (status [this]
    (if (or (contains? *system-map* this)
            (contains? (get @systems *system-key*) this))
      :started
      :stopped))

  clojure.lang.IDeref
  (deref [this]
    (if (= :started (status this))
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


;;; Printing

(defmethod print-method StateVar [sv ^java.io.Writer writer]
  (.write writer (str sv)))

(defmethod simple-dispatch StateVar [sv]
  (.write *out* (str sv)))


;;; API

(defn states []
  (keys statevars))

(defn upsert [symbol state]
  (let [statevar (->StateVar symbol)]
    (.put statevars statevar state)
    statevar))
