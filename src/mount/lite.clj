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

(defrecord State [start-fn stop-fn value]
  protocols/IState
  (start [_]
    (reset! value (start-fn)))
  (stop [_]
    (let [result (stop-fn @value)]
      (reset! value nil)
      result)))


;;; Public API.

(defmacro state
  "Create an anonymous state, useful for substituting. Takes a :start
  and a :stop expression. It also takes a :name symbol, by which the
  stop expression can access the result of the start expression. All
  expressions are optional."
  [& {:keys [start stop name] :as exprs :or {name (gensym)}}]
  (validations/validate-state exprs)
  `(->State (fn [] ~start) (fn [~name] ~stop) (atom nil)))

(def ^{:doc "Low-level function to create a global state, given a
  namespace (symbol or Namespace object), a name (symbol) and state
  implementation. Returns the object that should be bound to a var."
       :arglists '([ns name state])}
  defstate* impl/defstate)

(defmacro defstate
  "Define a global state. Takes a :start and a :stop expression.
  Redefining a defstate does not affect the stop logic of an already
  started defstate."
  [name & exprs]
  (validations/validate-defstate name)
  `(def ~name (defstate* *ns* '~name (state ~@exprs))))

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
  have been substituted. These can be nested."
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
  the (possibly empty) existing system. These can be nested.

  Note that calling `start` within the scope of `with-system-map`
  deliberately does not \"move\" the state values from the system map
  to the started system. This means that one can end up with a
  partially running system when leaving the `with-system-map` scope."
  [system & body]
  `(let [conformed# (validations/validate-with-system-map ~system)]
     (binding [impl/*system-map* (merge impl/*system-map* conformed#)]
       ~@body)))
