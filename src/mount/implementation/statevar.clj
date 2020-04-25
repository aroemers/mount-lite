(ns mount.implementation.statevar
  "Core implementation."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true}
  (:require [clojure.pprint :refer [simple-dispatch]]
            [mount.extensions :as extensions]
            [mount.protocols :as protocols :refer [start stop status]]))

;;; Global state.

(defonce ^:dynamic *system-key*  :default)
(defonce ^:dynamic *system-map*  {})
(defonce ^:dynamic *substitutes* {})
(defonce ^:dynamic *lazy-mode*   false)

(defonce statevars (java.util.LinkedHashMap.))
(defonce systems   (atom {}))
(defonce started   (atom {}))


;;; StateVar implementation.

(defn throw-error [state & msg]
  (throw (ex-info (apply str (interpose " " msg)) 
                  {:state state :system-key *system-key*})))

(defrecord StateVar [name]
  protocols/IState
  (start [this]
    (when (and (= :stopped (status this))
               (extensions/*predicate* this))
      (let [state  (or (get *substitutes* this)
                       (get statevars this))
            result (start state)]
        (swap! systems update *system-key* assoc this result)
        (swap! started update *system-key* assoc this state)
        :started)))

  (stop [this]
    (when (and (= :started (status this))
               (extensions/*predicate* this))
      (when-let [state (get-in @started [*system-key* this])]
        (stop state))
      (swap! systems update *system-key* dissoc this)
      (swap! started update *system-key* dissoc this)
      :stopped))

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
      (if *lazy-mode*
        (if (extensions/*predicate* this)
          (do (println "Lazily starting" this "...")
              (start this)
              (deref this))
          (throw-error this "Not allowed to lazy start state" this))
        (throw-error this "Cannot deref state" this " when not started"))))

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
