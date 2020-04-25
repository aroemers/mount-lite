(ns mount.extensions
  "The extension point for mount-lite.

  Extending mount-lite means influencing which states are actually
  started or stopped whenever the are called to start or stop.

  One can influence this by supplying a predicate that receives a
  state just before it is started or stopped. These predicates compose
  thus every predicate needs to agree that a state should start or
  stop."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.validations.extensions :as validations]))

(defonce ^:dynamic ^:no-doc *predicate* identity)

(defmacro with-predicate
  "Supply an predicate to be active within the given body, composing it
  with possibly other active predicates."
  [predicate & body]
  `(let [predicate# ~predicate]
     (validations/validate-with-predicate predicate#)
     (binding [*predicate* (every-pred predicate# *predicate*)]
       ~@body)))
