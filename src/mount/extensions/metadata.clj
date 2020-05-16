(ns mount.extensions.metadata
  "Extension to consider global states based on their vars metadata. For
  example:

  (defstate nrepl-server
    {:qualifier :dev}
    :start (nrepl/start-server ...)
    :stop  (nrepl/stop-server @nrepl-server))

  (with-metadata #(contains? #{:dev nil} (:qualifier %))
    (start))"
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions :as extensions]))

;;; Public API.

(defmacro with-metadata
  "Inside the body, only consider the global states for which the
  metadata passes the given predicate."
  [predicate & body]
  `(extensions/with-predicate #(~predicate (meta (resolve (symbol (namespace %) (name %)))))
     ~@body))
