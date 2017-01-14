(ns mount.extensions.data-driven
  "Start a system based on a data configuration."
  (:require [mount.lite :as mount]))

(defn with-config*
  [{:keys [only except substitutes]} f]
  (let [states      (cond->> @mount/*states*
                      (seq only)   (filter (set (map keyword only)))
                      (seq except) (remove (set (map keyword except))))
        substitutes (->> substitutes
                         (partition 2)
                         (reduce (fn [m [state substitute]]
                                   (assoc m (resolve state) (deref (resolve substitute))))
                                 {}))]
    (binding [mount/*states*      (atom states)
              mount/*substitutes* substitutes]
      (f))))

(defmacro with-config
  "Set the available states and substitutes for starting (or stopping)
  by using a configuration map. This map supports the :only (a
  collection), :except (a collection) and :substitutes (a sequence)
  keys. All references to defstates or substitute states should be
  symbols, i.e. they will be resolved for you. For example:

  (with-config '{:only        [user/foo user/bar]
                 :substitutes [user/bar user/baz]}
    (start))"
  [config & body]
  `(with-config* ~config
     (fn []
       ~@body)))
