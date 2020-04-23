(ns mount.extensions.basic
  "Basic extensions, with direct influence which states are considered
  for starting or stopping."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.set :as set]
            [mount.extensions :as extensions]
            [mount.lite :as mount]))

;;; Internals.

(def ^:dynamic *only*   nil)
(def ^:dynamic *except* nil)

(defn- predicate-factory
  [{:keys [states]}]
  (cond-> (set states)
    *only*   (set/intersection *only*)
    *except* (set/difference *except*)))

(extensions/register-predicate-factory predicate-factory)


;;; Public API.

(defmacro with-only
  "When starting or stopping within the given body, only the given
  collection of states are considered. Can be nested and composes with
  `with-except`."
  [states & body]
  `(binding [*only* (cond-> (set ~states) *only* (set/intersection *only*))]
     ~@body))

(defmacro with-except
  "When starting or stopping within the given body, the given collectino
  of states are excluded from consideration. Can be nested and
  composes with `with-only`."
  [states & body]
  `(binding [*except* (set/union *except* (set ~states))]
     ~@body))

(defn ns-states
  "Returns a collection of states within the given namespaces. The
  namespaces can be actual Namespace objects, strings or symbols."
  [& nss]
  (let [ns-strs (set (map str nss))]
    (filter (comp ns-strs namespace)
            (keys (mount/status)))))
