(ns mount.extensions.refresh
  "An extension that offers helpers for working with the
  tools.namespace contrib library. Make sure you add the
  tools.namespace to the dependencies of your project yourself. This
  extension has been tested with versions 0.2.11, 0.3.1 and 1.1.0
  of the tools.namespace library."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions.basic :as basic]
            [mount.lite :as mount]
            [mount.utils :as utils]))

;;; Vars from tools.namespace, which may not be loaded as a dependency.

(def ^:private refresh-tracker
  (resolve 'clojure.tools.namespace.repl/refresh-tracker))

(def ^:private refresh-dirs
  (resolve 'clojure.tools.namespace.repl/refresh-dirs))

(def ^:private scan
  (resolve 'clojure.tools.namespace.dir/scan))

(def ^:private scan-dirs
  (resolve 'clojure.tools.namespace.dir/scan-dirs))

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
    (let [scan-fn (cond
                    ;; `refresh-dirs` is set and `scan-dirs` is available.
                    (and refresh-dirs (seq @refresh-dirs) scan-dirs)
                    #(scan-dirs % @refresh-dirs)

                    ;; `refresh-dirs` is not set, but `scan-dirs` is available.
                    scan-dirs
                    #(scan-dirs %)

                    ;; We are using 0.2.x version of `clojure.tools.namespace`.
                    :else
                    scan)
          nss (-> refresh-tracker deref scan-fn remove-disabled :clojure.tools.namespace.track/unload set)]
      (keep (fn [kw]
              (when (nss (symbol (namespace kw)))
                (utils/resolve-keyword kw)))
            @mount/*states*))
    (throw (UnsupportedOperationException. "Could not find tools.namespace dependency"))))

(defn refresh
  "Wrapper around clojure.tools.namespace.repl/refresh, which stops
  the affected defstate vars before reloading, and restarts the
  stopped states afterwards."
  []
  (let [stopped (basic/with-only (affected-vars) (mount/stop))]
    (println :stopped stopped)
    (let [result (refresh*)]
      (if (= result :ok)
        (let [started (basic/with-only stopped (mount/start))]
          (println :started started))
        result))))
