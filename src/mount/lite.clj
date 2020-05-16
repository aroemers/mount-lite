(ns mount.lite
  "The core namespace providing the public API."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions :as extensions]
            [mount.extensions.up-to :as up-to]
            [mount.implementation.statevar :as impl]
            [mount.protocols :as protocols]
            [mount.validations.lite :as validations]))

;;; Internals.

(defrecord State [start-fn stop-fn]
  protocols/IState
  (start [_] (start-fn))
  (stop  [_] (stop-fn)))


;;; Public API.

(defmacro state
  "Create an anonymous state, useful for substituting. Takes a :start
  and a :stop expression."
  [& {:keys [start stop] :as exprs}]
  (validations/validate-state exprs)
  `(->State (fn [] ~start) (fn [] ~stop)))

(defn defstate*
  "Low-level function to create a global state, given a
  named object (e.g. a symbol) and a state implementation. Returns the
  object that provides access to the global state's value."
  [named state]
  (impl/defstate named state))

(defmacro defstate
  "Define a global state. Takes a :start and a :stop expression.
  Supports optional docstring and attribute map like defn."
  [name & exprs]
  (let [[name exprs] (validations/validate-defstate name exprs)]
    `(def ~name (defstate* '~(symbol (str *ns*) (str name)) (state ~@exprs)))))

(defn start
  "Starts all the unstarted global defstates, in the context of the
  current system key. Takes an optional state, starting the system
  only up to that particular state."
  ([]
   (doall (filter protocols/start (impl/states))))
  ([up-to]
   (let [conformed    (validations/validate-start-stop up-to)
         up-to-filter (up-to/predicate-factory (impl/states) true conformed)]
     (extensions/with-predicate up-to-filter
       (start)))))

(defn stop
  "Stops all the started global defstates, in the context of the current
  system key. Takes an optional state, stopping the system
  only up to that particular state."
  ([]
   (doall (filter protocols/stop (reverse (impl/states)))))
  ([up-to]
   (let [conformed    (validations/validate-start-stop up-to)
         up-to-filter (up-to/predicate-factory (impl/states) false conformed)]
     (extensions/with-predicate up-to-filter
       (stop)))))

(defn status
  "Returns a status map of all the states."
  []
  (reduce #(assoc %1 %2 (protocols/status %2)) {} (impl/states)))


;;; Advanced public API

(defmacro with-substitutes
  "Executes the given body while the given defstates' start/stop logic
  is substituted.

  When starting one or more states within the body, the associated
  substituted stop logic will be cached, such that it is also in
  effect outside the scope of the body. This cache is unaffected by
  possible global defstate redefinitions.

  When stopping one or more states within the body, the substitutes
  always override the stop logic of already started states, regardless
  of how those were started.

  Multiple uses of this macro can be nested, merging the maps."
  [substitutes & body]
  `(let [conformed# (validations/validate-with-substitutes ~substitutes)]
     (binding [impl/*substitutes* (merge impl/*substitutes* conformed#)]
       ~@body)))

(defmacro with-system-key
  "Executes the given body in the context of the given system key, like
  starting/stopping a system or dereferencing a defstate in that
  system. This allows multiple parallel state systems."
  [key & body]
  `(binding [impl/*system-key* ~key]
     ~@body))

(defmacro with-system-map
  "Executes the given body while the given system map has been merged in
  the (possibly empty) existing system.

  Note that calling `start` within the scope of `with-system-map`
  deliberately does not \"move\" the state values from the system map
  to the started system. This means that one can end up with a
  partially running system when leaving the `with-system-map` scope.

  Multiple uses of this macro can be nested, merging the maps."
  [system & body]
  `(let [conformed# (validations/validate-with-system-map ~system)]
     (binding [impl/*system-map* (merge impl/*system-map* conformed#)]
       ~@body)))

(defn system-keys
  "Returns a set of active system keys."
  []
  (impl/system-keys))

(defn reload!
  "Have mount-lite forget all known global states, so that mount-lite
  can pick up on new and renamed global states in the correct order
  when they are loaded again. You will either have to do this loading
  yourself - using `(require 'my-app.core :reload-all)` for example -
  or pass a namespace (string, symbol or actual namespace object) to
  reload! to do this loading for you. No system(s) can be running when
  calling reload!"
  ([]
   (assert (empty? (system-keys)) "Cannot reload while system(s) are running.")
   (impl/unload!))
  ([ns]
   (reload!)
   (require (symbol (str ns)) :reload-all)))
