(ns mount.validations
  "Validations of core.lite's function arguments."
  {:clojure.tools.namespace.repl/load   false
   :clojure.tools.namespace.repl/unload false
   :no-doc                              true}
  (:require [mount.internals :as internals]))

(defn- state? [obj]
  (satisfies? internals/IState obj))

(defn- defstate? [obj]
  (contains? (set internals/states) obj))

(defn validate-state [exprs]
  (let [other-keys (keys (dissoc exprs :start :stop))]
    (assert (empty? other-keys)
            (apply str "unknown expression key(s) supplied to state: "
                   (interpose ", " other-keys)))))

(defn validate-defstate [name]
  (assert (symbol? name)
          "name of defstate must be a symbol.")
  (assert (not (namespace name))
          (str "name of defstate must not be namespaced.")))

(defn validate-start [up-to]
  (assert (or (nil? up-to)
              (contains? internals/states up-to))
          "up-to parameter of start must be nil or an actual defstate."))

(defn validate-stop [up-to]
  (assert (or (nil? up-to)
              (contains? internals/states up-to))
          "up-to parameter of stop must be nil or an actual defstate."))

(defn validate-with-substitutes [substitutes]
  (assert (or (map? substitutes)
              (and (vector? substitutes)
                   (even? (count substitutes))))
          "substitutes must be a map or vector.")
  (let [conformed (cond->> substitutes (vector? substitutes) (apply hash-map))]
    (assert (every? defstate? (keys conformed))
            "all keys of the substitutes map must be a known defstate.")
    (assert (every? state? (vals conformed))
            "all values of the substitutes map must be an (anonymous) state.")
    conformed))

(defn validate-with-system-map [system]
  (assert (or (map? system)
              (and (vector? system)
                   (even? (count system))))
          "system must be a map or vector.")
  (let [conformed (cond->> system (vector? system) (apply hash-map))]
    (assert (every? defstate? (keys conformed))
            "all keys of the system map must be a known defstate.")
    conformed))
