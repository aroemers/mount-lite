(ns mount.extensions.basic
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.set :as set]
            [mount.lite :as mount]))

(def ^:dynamic *only*   nil)
(def ^:dynamic *except* nil)

(defn- predicate-factory
  [{:keys [states]}]
  (cond-> (set states)
    *only*   (set/intersection *only*)
    *except* (set/difference *except*)))

(defmacro with-only
  [states & body]
  `(binding [*only* (cond-> (set ~states) *only* (set/intersection *only*))]
     ~@body))

(defmacro with-except
  [states & body]
  `(binding [*except* (set/union *except* (set ~states))]
     ~@body))

(defn ns-states
  [& nss]
  (let [ns-strs (set (map str nss))]
    (filter (comp ns-strs namespace)
            (keys (mount/status)))))

(swap! mount/predicate-factories conj predicate-factory)
