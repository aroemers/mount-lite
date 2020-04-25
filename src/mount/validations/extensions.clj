(ns mount.validations.extensions
  "Validation functions for mount.extensions namespace."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true})

(defn validate-with-predicate [f]
  (assert (instance? clojure.lang.IFn f)
          "predicate must be a function, i.e. implement clojure.lang.IFn."))
