(ns celtuce.args.cluster-node-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [celtuce.args.cluster-node :as sut])
  (:import
    (clojure.lang ExceptionInfo)
    (io.lettuce.core.cluster.models.partitions RedisClusterNode RedisClusterNode$NodeFlag)
    (java.util Collection EnumSet)
    (java.util.concurrent TimeUnit)))


(set! *warn-on-reflection* true)


(defn- redis-cluster-node
  ^RedisClusterNode
  [{:keys [id connected? flags]}]
  (doto (RedisClusterNode.)
    (.setNodeId id)
    (.setConnected (boolean connected?))
    (.setFlags
      (if (seq flags)
        (EnumSet/copyOf ^Collection flags)
        (EnumSet/noneOf RedisClusterNode$NodeFlag)))))


(deftest ^:unit test-enum->kw
  (let [time-unit->kw {TimeUnit/MILLISECONDS :milliseconds}]
    (testing "valid enum mapping returns correct keyword"
      (is (= :milliseconds
             (sut/enum->kw time-unit->kw TimeUnit/MILLISECONDS))))
    (testing "missing enum throws exception with descriptive message"
      (is (thrown-with-msg? ExceptionInfo #"Unknown enum member: MINUTES"
                            (sut/enum->kw time-unit->kw TimeUnit/MINUTES))))))


(deftest test-node->map
  (testing "returns stable shape & types"
    (let [generic-node (-> {:id "generic" :connected? true}
                           (redis-cluster-node)
                           (sut/node->map))]
      (is (= #{:id :is-connected :flags :role :has-no-slots}
             (set (keys generic-node))))
      (is (string? (:id generic-node)))
      (is (boolean? (:is-connected generic-node)))
      (is (set? (:flags generic-node)))
      (is (keyword? (:role generic-node)))
      (is (boolean? (:has-no-slots generic-node)))))

  (testing "converts `NodeFlag` enums to a deduped keyword set"
    (let [flagged-node (-> {:id    "flagged"
                            :flags [RedisClusterNode$NodeFlag/MYSELF
                                    RedisClusterNode$NodeFlag/FAIL]}
                           (redis-cluster-node)
                           (sut/node->map))]
      (is (= #{:myself :fail} (:flags flagged-node)))))

  (testing "converts `Role` enum to keyword (independent of flags)"
    (let [master-node (-> {:id         "master-node"
                           :flags      [RedisClusterNode$NodeFlag/UPSTREAM]}
                          (redis-cluster-node)
                          (sut/node->map))
          replica-node (-> {:id    "replica-node"
                            :flags [RedisClusterNode$NodeFlag/REPLICA]}
                           (redis-cluster-node)
                           (sut/node->map))]
      (is (= :upstream (:role master-node)))
      (is (= :replica (:role replica-node))))))
