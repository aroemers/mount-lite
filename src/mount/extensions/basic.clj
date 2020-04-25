(ns mount.extensions.basic
  "Basic extensions, with direct influence which states are considered
  for starting or stopping."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions :as extensions]
            [mount.lite :as mount]
            [mount.validations :refer [defstate? maybe-deref]]))

;;; Validations.

(defn ^:no-doc validate-with-only [states]
  (assert (or (nil? states) (coll? states))
          "the first parameter must be a collection")
  (let [conformed (map maybe-deref states)]
    (assert (every? defstate? conformed)
            "the items in the collection must all be existing states")
    conformed))

(defn ^:no-doc validate-with-except [states]
  (validate-with-only states))


;;; Public API.

(defmacro with-only
  "When starting or stopping within the given body, only the given
  collection of states are considered. Can be nested and composes with
  `with-except`."
  [states & body]
  `(let [conformed# (validate-with-only ~states)]
     (extensions/with-predicate (set conformed#)
       ~@body)))

(defmacro with-except
  "When starting or stopping within the given body, the given collectino
  of states are excluded from consideration. Can be nested and
  composes with `with-only`."
  [states & body]
  `(let [conformed# (validate-with-except ~states)]
     (extensions/with-predicate (complement (set conformed#))
       ~@body)))

(defn ns-states
  "Returns a collection of states within the given namespaces. The
  namespaces can be actual Namespace objects, strings or symbols."
  [& nss]
  (let [ns-strs (set (map str nss))]
    (filter (comp ns-strs namespace)
            (keys (mount/status)))))
