(ns mount.lite
  "The core namespace providing the public API."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions :as extensions]
            [mount.internals :as internals]
            [mount.validations :as validations]))

;;; Public API.

(defmacro state
  "Create an anonymous state, useful for substituting. Takes a :start
  and a :stop expression."
  [& {:keys [start stop] :as exprs}]
  (validations/validate-state exprs)
  `(internals/->State (fn [] ~start) (fn [] ~stop)))

(defmacro defstate
  "Define a global state. Takes a :start and a :stop expression.
  Redefining a defstate does not affect the stop logic of an already
  started defstate."
  [name & exprs]
  (validations/validate-defstate name)
  `(let [statevar# (internals/->StateVar (symbol ~(str *ns*) ~(str name)))
         var#      (or (defonce ~name statevar#) (resolve '~name))
         state#    (state ~@exprs)]
     (alter-meta! var# assoc :state state#)
     (.add internals/states statevar#)
     var#))

(defn start
  "Starts all the unstarted global defstates, in the context of the
  current system key. Takes an optional state, starting the system
  only up to that particular state."
  ([]
   (start nil))
  ([up-to]
   (validations/validate-start up-to)
   (let [state-filter (extensions/state-filter (seq internals/states) true up-to)]
     (doall (filter (every-pred state-filter internals/start*) internals/states)))))

(defn stop
  "Stops all the started global defstates, in the context of the current
  system key. Takes an optional state, stopping the system
  only up to that particular state."
  ([]
   (stop nil))
  ([up-to]
   (validations/validate-stop up-to)
   (let [state-filter (extensions/state-filter (seq internals/states) false up-to)]
     (doall (filter (every-pred state-filter internals/stop*) (reverse internals/states))))))

(defn status
  "Returns a status map of all the states."
  []
  (reduce #(assoc %1 %2 (internals/status* %2)) {} internals/states))


;;; Advanced public API

(defmacro with-substitutes
  "Executes the given body while the given defstates' start/stop logic
  have been substituted. These can be nested."
  [substitutes & body]
  `(let [conformed# (validations/validate-with-substitutes ~substitutes)]
     (binding [internals/*substitutes* (merge internals/*substitutes* conformed#)]
       ~@body)))

(defmacro with-system-key
  "Executes the given body in the context of the given system key, like
  starting/stopping a system or dereferencing a defstate in that
  system. This allows multiple parallel state systems."
  [key & body]
  `(binding [internals/*system-key* ~key]
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
     (binding [internals/*system-map* (merge internals/*system-map* conformed#)]
       ~@body)))


;;; Default extensions

(require 'mount.extensions.up-to)
