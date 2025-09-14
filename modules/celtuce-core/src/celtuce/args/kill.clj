(ns celtuce.args.kill
  (:import 
   (io.lettuce.core KillArgs KillArgs$Builder)))


;; TODO: remove if not used in future
(defn kill-args
  "Creates KillArgs for CLIENT KILL command to terminate client connections.
   Options: :skipme (skip current connection), :addr (target address),
   :id (client ID), :type (connection type: :pubsub, :normal, or :slave)."
  ^KillArgs
  [& {skipme :skipme addr :addr id :id type :type}]
  {:pre [(or (nil? type)
             (#{:pubsub :normal :slave} type))]}
  (-> (if type
        (cond
          (= :pubsub type) (KillArgs$Builder/typePubsub)
          (= :normal type) (KillArgs$Builder/typeNormal)
          (= :slave  type) (KillArgs$Builder/typeSlave))
        (KillArgs.))
      (cond-> 
          skipme (.skipme skipme)
          addr   (.addr addr)
          id     (.id id))))
