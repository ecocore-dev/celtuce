(ns hooks.celtuce-pool
  (:require [clj-kondo.hooks-api :as api]))


(defn with-conn-pool
  "Hook for with-conn-pool macro to bind command function variable."
  [{:keys [node]}]
  (let [[conn-pool cmds-name & body] (rest (:children node))]
    {:node (api/list-node
             (list*
               (api/token-node 'let)
               (api/vector-node [cmds-name conn-pool])
               body))}))

(defn with-conn-pool*
  "Hook for with-conn-pool* macro to bind both command function and connection variables."
  [{:keys [node]}]
  (let [[conn-pool cmds-name conn-name & body] (rest (:children node))]
    {:node (api/list-node
             (list*
               (api/token-node 'let)
               (api/vector-node [cmds-name conn-pool
                                 conn-name conn-pool])
               body))}))
