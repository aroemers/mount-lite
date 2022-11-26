(ns hooks.lite
  (:require
    [clj-kondo.hooks-api
     :as api
     :refer [token-node list-node sexpr map-node keyword-node]]))

(defn- map-node->map [nodes]
  (zipmap (map sexpr (take-nth 2 nodes))
          (take-nth 2 (rest nodes))))

(defn defstate-hook [{:keys [node]}]
  (let [[_ def-name & nodes] (:children node)
        {:keys [start stop]} (map-node->map nodes)]
    (when-not start
      (throw (ex-info "missing :start expression" {})))
    {:node
     (list-node 
       (list (token-node 'defonce)
             def-name
             (list-node
               (list (token-node 'atom)
                     (map-node
                       (list (keyword-node :start)
                             start
                             (keyword-node :stop)
                             (or stop (token-node nil))))))))}))

(comment
  (def node
    (api/parse-string "(defstate db
                        :start (db/start (get-in @config/config [:db :url]))
                        :stop  (db/stop @db))"))
  (sexpr node)
  (sexpr (:node (defstate-hook {:node node}))))
