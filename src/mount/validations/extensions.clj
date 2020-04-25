(ns mount.validations.extensions
  "Validation functions for mount.extensions namespace."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false})

(defn validate-register-predicate-factory [f]
  (assert (fn? f) "predicate factory must be a function."))
