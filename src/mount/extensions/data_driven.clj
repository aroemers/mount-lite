(ns mount.extensions.data-driven
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false}
  (:require [mount.extensions.basic :as basic]
            [mount.lite :as mount]))

(defn- wrap-with-only [f states]
  (fn []
    (basic/with-only states
      (f))))

(defn- wrap-with-except [f states]
  (fn []
    (basic/with-except states
      (f))))

(defn- wrap-with-substitutes [f substitutes]
  (fn []
    (mount/with-substitutes substitutes
      (f))))

(defn- resolve* [symbol]
  (-> symbol resolve deref))

(defn with-config*
  [{:keys [only except substitutes]} f]
  ((cond-> f
     only        (wrap-with-only (map resolve* only))
     except      (wrap-with-except (map resolve* except))
     substitutes (wrap-with-substitutes (reduce-kv (fn [a k v] (assoc a (resolve* k) (resolve* v))) {} substitutes)))))

(defmacro with-config
  [config & body]
  `(with-config* ~config
     (fn [] ~@body)))
