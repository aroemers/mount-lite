(ns mount.validations
  "Validation helper functions."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.implementation.statevar :as impl]
            [mount.protocols :as protocols]))

;;; Helper functions

(defn state? [obj]
  (satisfies? protocols/IState obj))

(defn defstate? [obj]
  (and (state? obj)
       (contains? (set (impl/states)) obj)))
