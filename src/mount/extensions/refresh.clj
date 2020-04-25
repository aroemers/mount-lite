(ns mount.extensions.refresh
  "An extension that offers helpers for working with the tools.namespace
  contrib library. Make sure you add the tools.namespace to the
  dependencies of your project yourself.

  This extension has been tested with version 0.2.11 of the
  tools.namespace library."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.tools.namespace.dir :as dir]
            [clojure.tools.namespace.repl :as repl]
            [clojure.tools.namespace.track :as track]
            [mount.extensions.basic :as basic]
            [mount.lite :as mount]))

(defn affected-states
  "Returns a collection of states that would be reloaded by
  clojure.tools.namespace.repl/refresh."
  []
  (let [scanned (dir/scan @#'repl/refresh-tracker)
        pruned  (#'repl/remove-disabled scanned)
        nss     (::track/unload pruned)]
    (apply basic/ns-states nss)))

(defn refresh
  "Wrapper around clojure.tools.namespace.repl/refresh, which stops the
  the affected states before reloading, and restarts the the stopped
  states afterwards."
  []
  (let [affected (affected-states)
        stopped  (basic/with-only affected (mount/stop))
        _        (prn :stopped stopped)
        result   (repl/refresh)]
    (when (= result :ok)
      (prn :started (basic/with-only stopped (mount/start)))
      result)))
