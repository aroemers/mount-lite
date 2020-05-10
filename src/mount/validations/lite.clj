(ns mount.validations.lite
  "Validation functions for mount.lite namespace."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true}
  (:require [mount.validations :as validations]))

(defn validate-state [exprs]
  (let [other-keys (keys (dissoc exprs :start :stop))]
    (assert (empty? other-keys)
            (apply str "unknown expression key(s) supplied to state: "
                   (interpose ", " other-keys)))))

(defn validate-defstate [name [arg1 arg2 & argx :as args]]
  (assert (symbol? name)
          "name of defstate must be a symbol.")
  (assert (not (namespace name))
          (str "name of defstate must not be namespaced."))
  (let [[attrs args]
        (cond (and (string? arg1) (map? arg2)) [(assoc arg2 :doc arg1) argx]
              (string? arg1)                   [{:doc arg1} (cons arg2 argx)]
              (map? arg1)                      [arg1 (cons arg2 argx)]
              :otherwise                       [{} args])]
    [(with-meta name (merge (meta name) attrs)) args]))

(defn validate-start-stop [up-to]
  (let [conformed (validations/maybe-deref up-to)]
    (assert (validations/defstate? conformed)
            "up-to parameter must be a defstate.")
    conformed))

(defn validate-with-substitutes [substitutes]
  (assert (or (map? substitutes) (vector? substitutes))
          "substitutes must be a map or vector.")
  (let [conformed (->> (validations/maybe-hash-map substitutes)
                       (validations/map-keys validations/maybe-deref))]
    (assert (every? validations/defstate? (keys conformed))
            "all keys of the substitutes map must be a known defstate.")
    (assert (every? validations/state? (vals conformed))
            "all values of the substitutes map must be an (anonymous) state.")
    conformed))

(defn validate-with-system-map [system]
  (assert (or (map? system) (vector? system))
          "system must be a map or vector.")
  (let [conformed (->> (validations/maybe-hash-map system)
                       (validations/map-keys validations/maybe-deref))]
    (assert (every? validations/defstate? (keys conformed))
            "all keys of the system map must be a known defstate.")
    conformed))
