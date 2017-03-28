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
  "Returns a list of defstate vars that will be reloaded by
  clojure.tools.namespace.repl/refresh."
  []
  (if refresh-tracker
    (let [nss (-> refresh-tracker deref scan remove-disabled :clojure.tools.namespace.track/unload set)]
      (keep (fn [kw]
              (when (nss (symbol (namespace kw)))
                (utils/resolve-keyword kw)))
            @mount/*states*))
    (throw (UnsupportedOperationException. "Could not find tools.namespace dependency"))))


;;; Refresh wrapper

(def ^:private restart)

(defn- do-lifecycle
  [lifecycle-fn vars]
  (loop [vars   vars
         result ()]
    (if-let [var (first vars)]
      (let [affected (lifecycle-fn var)]
        (recur (remove (set affected) vars)
               (concat result affected)))
      result)))

(defn- restarter
  [_ stopped-keywords start-fn]
  (fn []
    (let [vars    (keep utils/resolve-keyword stopped-keywords)
          started (do-lifecycle start-fn vars)]
      (println :started started))))

(defn refresh
  "Wrapper around clojure.tools.namespace.repl/refresh, which stops
  the affected defstate vars before reloading, and restarts the stopped
  states afterwards.

  One can optionally supply your own start-fn and/or stop-fn, for
  instance when using the explicit-deps extension."
  [& {:keys [start-fn stop-fn]
      :or   {start-fn mount/start
             stop-fn  mount/stop}}]
  (let [affected    (affected-vars)
        stopped     (do-lifecycle stop-fn affected)
        stopped-kws (mapv utils/var->keyword stopped)]
    (println :stopped stopped)
    (alter-var-root #'restart restarter stopped-kws start-fn)
    (refresh* :after 'mount.extensions.refresh/restart)))
