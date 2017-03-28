(ns mount.extensions.refresh
  "An extension that offers helpers for working with the
  tools.namespace contrib library. Make sure you add the
  tools.namespace to the dependencies of your project yourself. This
  extension has been tested with version 0.2.11 of the tools.namespace
  library."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.lite :as mount]
            [mount.utils :as utils]))

;;; Vars from tools.namespace, which may not be loaded as a dependency.

(def ^:private refresh-tracker
  (resolve 'clojure.tools.namespace.repl/refresh-tracker))

(def ^:private scan
  (resolve 'clojure.tools.namespace.dir/scan))

(def ^:private remove-disabled
  (resolve 'clojure.tools.namespace.repl/remove-disabled))

(def ^:private refresh*
  (resolve 'clojure.tools.namespace.repl/refresh))


;;; Helper functions.

(defn affected-vars
  "Returns a set of defstate vars that will be reloaded by
  clojure.tools.namespace.repl/refresh."
  []
  (if refresh-tracker
    (let [nss (-> refresh-tracker deref scan remove-disabled :clojure.tools.namespace.track/unload set)]
      (->> @mount/*states*
           (keep (fn [kw]
                   (when (nss (symbol (namespace kw)))
                     (resolve (utils/keyword->symbol kw)))))
           (set)))
    (throw (UnsupportedOperationException. "Could not find tools.namespace dependency"))))


;;; Refresh wrapper

(def ^:private restart)

(defn- restarter
  [_ stopped-keywords start-fn]
  (fn []
    (->> (for [kw    stopped-keywords
               :let  [state (utils/resolve-keyword kw)]
               :when state]
           (start-fn state))
         (apply concat)
         (println :started))))

(defn refresh
  "Wrapper around clojure.tools.namespace.repl/refresh, which stops
  the affected defstate vars before reloading, and restarts the stopped
  states afterwards."
  []
  (let [affected    (affected-vars)
        stopped     (->> (for [state affected]
                           (mount/stop state))
                         (apply concat)
                         (doall))
        stopped-kws (mapv utils/var->keyword stopped)]
    (println :stopped stopped)
    (alter-var-root #'restart restarter stopped-kws mount/start)
    (refresh* :after 'mount.extensions.refresh/restart)))
