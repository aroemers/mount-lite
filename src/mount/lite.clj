(ns mount.lite
  "The core namespace providing the public API"
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.pprint :refer [simple-dispatch]]
            [mount.extensions :as extensions]
            ;; Always load the following extensions:
            mount.extensions.up-to))

;;; Internals

(defonce ^:dynamic *system-key*  :default)
(defonce ^:dynamic *system-map*  {})
(defonce ^:dynamic *substitutes* {})

(defonce ^:private states  (java.util.LinkedHashSet.))
(defonce ^:private systems (atom {}))
(defonce ^:private started (atom {}))

(defprotocol IState
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


;;; Core public API

(defmacro state
  "Create an anonymous state, useful for substituting. Takes a :start
  and a :stop expression."
  [& {:keys [start stop]}]
  `(State. (fn [] ~start) (fn [] ~stop)))

(defmacro defstate
  "Define a global state. Takes a :start and a :stop expression.
  Redefining a defstate does not affect the stop logic of an already
  started defstate."
  [name & {:keys [start stop]}]
  `(let [statevar# (StateVar. (symbol ~(str *ns*) ~(str name)))
         var#      (or (defonce ~name statevar#) (resolve '~name))
         state#    (state :start ~start :stop ~stop)]
     (alter-meta! var# assoc :state state#)
     (.add @#'states statevar#)
     var#))

(defn start
  "Starts all the unstarted global defstates, in the context of the
  current system key. Takes an optional state, starting the system
  only up to that particular state."
  ([]
   (start nil))
  ([up-to]
   (let [state-filter (extensions/state-filter (seq states) true up-to)]
     (doall (filter (every-pred state-filter start*) states)))))

(defn stop
  "Stops all the started global defstates, in the context of the current
  system key. Takes an optional state, stopping the system
  only up to that particular state."
  ([]
   (stop nil))
  ([up-to]
   (let [state-filter (extensions/state-filter (seq states) false up-to)]
     (doall (filter (every-pred state-filter stop*) (reverse states))))))

(defn status
  "Returns a status map of all the states."
  []
  (reduce #(assoc %1 %2 (status* %2)) {} states))


;;; Advanced public API

(defmacro with-substitutes
  "Executes the given body while the given defstates' start/stop logic
  have been substituted. These can be nested."
  [substitutes & body]
  `(binding [*substitutes* (merge *substitutes* ~substitutes)]
     ~@body))

(defmacro with-system-key
  "Executes the given body in the context of the given system key, like
  starting/stopping a system or dereferencing a defstate in that
  system. This allows multiple parallel state systems."
  [key & body]
  `(binding [*system-key* ~key]
     ~@body))

(defmacro with-system-map
  "Executes the given body while the given system map has been merged in
  the (possibly empty) existing system. These can be nested.

  Note that calling `start` within the scope of `with-system-map`
  deliberately does not \"move\" the state values from the system map
  to the global system. This means that one can end up with a
  partially running system when leaving the `with-system-map` scope."
  [system & body]
  `(binding [*system-map* (merge *system-map* ~system)]
     ~@body))
