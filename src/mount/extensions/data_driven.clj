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

(defn- wrap-with-system-map [f system-map]
  (fn []
    (mount/with-system-map system-map
      (f))))

(defn- wrap-with-system-key [f system-key]
  (fn []
    (mount/with-system-key system-key
      (f))))

(defn- resolve* [symbol]
  (-> symbol resolve deref))

(defn with-config*
  [{:keys [only except substitutes system-map system-key]} f]
  ((cond-> f
     only        (wrap-with-only        (map resolve* only))
     except      (wrap-with-except      (map resolve* except))
     substitutes (wrap-with-substitutes (reduce-kv (fn [a k v] (assoc a (resolve* k) (resolve* v))) {} substitutes))
     system-map  (wrap-with-system-map  (reduce-kv (fn [a k v] (assoc a (resolve* k) v))            {} system-map))
     system-key  (wrap-with-system-key  system-key))))

(defmacro with-config
  [config & body]
  `(with-config* ~config
     (fn [] ~@body)))
