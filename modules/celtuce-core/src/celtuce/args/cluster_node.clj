(ns celtuce.args.cluster-node
  (:import
    (io.lettuce.core.cluster.models.partitions RedisClusterNode RedisClusterNode$NodeFlag)
    (io.lettuce.core.models.role RedisInstance$Role)))


(set! *warn-on-reflection* true)


(def ^:private flag->kw
  {RedisClusterNode$NodeFlag/EVENTUAL_FAIL :eventual-fail
   RedisClusterNode$NodeFlag/FAIL          :fail
   RedisClusterNode$NodeFlag/HANDSHAKE     :handshake
   RedisClusterNode$NodeFlag/LOADING       :loading
   RedisClusterNode$NodeFlag/MYSELF        :myself
   RedisClusterNode$NodeFlag/NOADDR        :noaddr
   RedisClusterNode$NodeFlag/NOFLAGS       :noflags
   RedisClusterNode$NodeFlag/ONLINE        :online
   RedisClusterNode$NodeFlag/REPLICA       :replica
   RedisClusterNode$NodeFlag/UPSTREAM      :upstream
   RedisClusterNode$NodeFlag/MASTER        :master
   RedisClusterNode$NodeFlag/SLAVE         :slave})

(def ^:private role->kw
  {RedisInstance$Role/MASTER   :master
   RedisInstance$Role/SLAVE    :slave
   RedisInstance$Role/REPLICA  :replica
   RedisInstance$Role/SENTINEL :sentinel
   RedisInstance$Role/UPSTREAM :upstream})

(defn enum->kw!
  "Converts Java enum to keyword using provided mapping.
   Throws an exception if the enum value is not found in the mapping."
  [mapping ^Enum e]
  (or (get mapping e)
      (throw (ex-info (str "Unknown enum member: " (.name e))
                      {:enum e :mapping mapping}))))

(defn node->map
  "Converts a RedisClusterNode instance to a Clojure map with keyword keys.
   Returns a map containing node flags, ID, role, slot status, and connection state."
  [^RedisClusterNode cluster-node]
  {:flags        (->> (.getFlags cluster-node)
                      (map #(enum->kw! flag->kw %))
                      (set))
   :id           (.getNodeId cluster-node)
   :role         (some->> cluster-node
                          (.getRole)
                          (enum->kw! role->kw))
   :has-no-slots (.hasNoSlots cluster-node)
   :is-connected (.isConnected cluster-node)})
