(ns mount.extensions.basic
  "Basic extensions for influencing which states are started or
  stopped."
  (:require [mount.lite :as mount]
            [mount.utils :as utils]))

(defn- with-states
  [state-kws f]
  (binding [mount/*states* (atom state-kws)]
    (f)))

(defmacro with-only
  "Reduce the available states for starting or stopping, to the given
  state vars, inside the body. The state vars don't need to be in
  dependency order, this is deduced from the root `*states*` binding."
  [state-vars & body]
  `(#'with-states (filter (set (map utils/var->keyword ~state-vars)) @mount/*states*)
     (fn [] ~@body)))

(defmacro with-except
  "Reduce the available states for starting or stopping, to all the
  current `*states*` except for the given state vars, inside the body."
  [state-vars & body]
  `(#'with-states (remove (set (map utils/var->keyword ~state-vars)) @mount/*states*)
     (fn [] ~@body)))
