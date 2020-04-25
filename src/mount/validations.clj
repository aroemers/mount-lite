(ns mount.validations
  "Validation helper functions."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.protocols :as protocols]))

;;; Internals

(def ^:private status
  (delay @(resolve 'mount.lite/status)))


;;; Helper functions

(defn state? [obj]
  (satisfies? protocols/IState obj))

(defn defstate? [obj]
  (and (state? obj)
       (contains? (set (keys (@status))) obj)))
