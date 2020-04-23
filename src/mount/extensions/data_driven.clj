(ns mount.extensions.data-driven
  "Extension to declare which states are started (or stopped) and how,
  using plain data."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [clojure.set :as set]
            [mount.extensions.basic :as basic]
            [mount.lite :as mount]))

;;; Internals.

(defn- resolve* [symbol]
  (-> symbol resolve deref))

(defn- wrap-with-only [f states]
  (fn []
    (basic/with-only (map resolve* states)
      (f))))

(defn- wrap-with-except [f states]
  (fn []
    (basic/with-except (map resolve* states)
      (f))))

(defn- wrap-with-substitutes [f substitutes]
  (let [resolved (reduce-kv (fn [a k v] (assoc a (resolve* k) (resolve* v))) {} substitutes)]
    (fn []
      (mount/with-substitutes resolved
        (f)))))

(defn- wrap-with-system-map [f system-map]
  (let [resolved (reduce-kv (fn [a k v] (assoc a (resolve* k) v)) {} system-map)]
    (fn []
      (mount/with-system-map resolved
        (f)))))

(defn- wrap-with-system-key [f system-key]
  (fn []
    (mount/with-system-key system-key
      (f))))

(def ^:private predefined-keys
  {:only        ::wrap-with-only
   :except      ::wrap-with-except
   :substitutes ::wrap-with-substitutes
   :system-map  ::wrap-with-system-map
   :system-key  ::wrap-with-system-key})


;;; Public API.

(defn with-config*
  "Same as the `with-config` macro, but now the body is in a 0-arity
  function."
  [config f]
  ((reduce-kv (fn [f k v]
                (require (symbol (namespace k)))
                (let [wrapper (resolve (symbol (namespace k) (name k)))]
                  (wrapper f v)))
              f
              (set/rename-keys config predefined-keys))))

(defmacro with-config
  "Use a plain data map to declare various options regarding how
  mount-lite should behave inside the given body. To refer to
  defstates or other globally defined values, use qualified symbols.
  The data map supports the following keys:

   :only - wrap the body with the basic/with-only extension.

   :except - wrap the body with the basic/with-except extension.

   :substitutes - wrap the body with a map of substitutions.

   :system-map - wrap the body with a system map.

   :system-key - wrap the body with a system key.

  Next to the predefined keys above, one can supply your own namespace
  qualified keys. These keys must point to wrapper functions that
  receive a 0-arity function and the data value, returning a 0-arity
  function calling the given function."
  [config & body]
  `(with-config* ~config
     (fn [] ~@body)))
