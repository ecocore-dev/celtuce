(ns hooks
  (:require [clj-kondo.hooks-api :as api]))


(defn with-pubsub-cmds
  "Hook for with-pubsub-cmds macro to bind pub and sub variables."
  [{:keys [node]}]
  (let [[listener & body] (rest (:children node))]
    {:node (api/list-node
            (list*
             (api/token-node 'let)
             (api/vector-node [(api/token-node 'pub) listener
                               (api/token-node 'sub) listener])
             body))}))
