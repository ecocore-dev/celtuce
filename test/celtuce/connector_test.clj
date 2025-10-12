(ns celtuce.connector-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [celtuce.connector :as conn])
  (:import
    (clojure.lang ExceptionInfo)
    (io.lettuce.core TimeoutOptions TimeoutOptions$DefaultTimeoutSource TimeoutOptions$FixedTimeoutSource)
    (io.lettuce.core.cluster
      ClusterTopologyRefreshOptions
      ClusterTopologyRefreshOptions$Builder
      ClusterTopologyRefreshOptions$RefreshTrigger)))


(def ^:private redis-server-url  "redis://localhost:6379")
(def ^:private redis-cluster-url "redis://localhost:30001")

(deftest ^:integration redis-connector-options-test

  (testing "default options for redis-server"
    (conn/redis-server
     redis-server-url
     :conn-options
     {:auto-flush true}
     :client-options
     {:ping-before-activate-connection       false
      :suspend-reconnect-on-protocol-failure false
      :cancel-commands-on-reconnect-failure  false
      :auto-reconnect                        true
      :request-queue-size                    Integer/MAX_VALUE
      :disconnected-behavior                 :default
      :timeout-options
      {:fixed-timeout {:timeout 2 :unit :seconds} :timeout-commands true}
      :socket-options
      {:timeout 10, :unit :seconds, :keep-alive false, :tcp-no-delay false}
      :ssl-options
      {:provider :jdk}}))

  (testing "default options for redis-cluster"
    (conn/redis-cluster
     redis-cluster-url
     :conn-options
     {:auto-flush true}
     :client-options
     {;; regular client-options
      :ping-before-activate-connection       false
      :suspend-reconnect-on-protocol-failure false
      :cancel-commands-on-reconnect-failure  false
      :auto-reconnect                        true
      :request-queue-size                    Integer/MAX_VALUE
      :disconnected-behavior                 :default
      :timeout-options
      {:fixed-timeout {:timeout 2 :unit :seconds} :timeout-commands true}
      :socket-options
      {:timeout 10, :unit :seconds, :keep-alive false, :tcp-no-delay false}
      :ssl-options
      {:provider :jdk}
      ;; specific cluster options
      :validate-cluster-node-membership       false
      :node-filter                            (fn [{node-flags :flags :as _cluster-node}]
                                                (not-any? node-flags [:fail :eventual-fail :handshake :noaddr]))
      :max-redirects                          5
      :topology-refresh-options
      {:enable-periodic-refresh               false
       :refresh-period                        {:period 60 :unit :seconds}
       :close-stale-connections               true
       :dynamic-refresh-sources               true
       :enable-all-adaptive-refresh-triggers  true
       :adaptive-refresh-triggers-timeout     {:timeout 30 :unit :seconds}
       :refresh-triggers-reconnect-attempts   5}})))


(deftest ^:unit test-enable-adaptive-refresh-triggers
  (let [all-refresh-triggers (set (ClusterTopologyRefreshOptions$RefreshTrigger/values))]

    (testing "legacy enabling all adaptive refresh triggers"
      (let [topology-options (.build
                               ^ClusterTopologyRefreshOptions$Builder
                               (#'conn/enable-adaptive-refresh-triggers
                                 (ClusterTopologyRefreshOptions/builder)
                                 {:enable-adaptive-refresh-trigger :all}))]
        (is (= all-refresh-triggers
               (set (.getAdaptiveRefreshTriggers topology-options))))))

    (testing "enabling all adaptive refresh triggers"
      (let [topology-options (.build
                               ^ClusterTopologyRefreshOptions$Builder
                               (#'conn/enable-adaptive-refresh-triggers
                                 (ClusterTopologyRefreshOptions/builder)
                                 {:enable-all-adaptive-refresh-triggers true}))]
        (is (= all-refresh-triggers
               (set (.getAdaptiveRefreshTriggers topology-options))))))

    (testing "enabling exactly provided adaptive triggers"
      (let [refresh-triggers #{:moved-redirect :ask-redirect}
            topology-options (.build
                               ^ClusterTopologyRefreshOptions$Builder
                               (#'conn/enable-adaptive-refresh-triggers
                                 (ClusterTopologyRefreshOptions/builder)
                                 {:enable-adaptive-refresh-triggers refresh-triggers}))]
        (is (= (->> refresh-triggers (map #'conn/kw->rtrigger) (set))
               (set (.getAdaptiveRefreshTriggers topology-options))))))

    (testing "legacy enabling exactly provided adaptive triggers"
      (let [refresh-triggers #{:moved-redirect :ask-redirect}
            topology-options (.build
                               ^ClusterTopologyRefreshOptions$Builder
                               (#'conn/enable-adaptive-refresh-triggers
                                 (ClusterTopologyRefreshOptions/builder)
                                 {:enable-adaptive-refresh-trigger refresh-triggers}))]
        (is (= (->> refresh-triggers (map #'conn/kw->rtrigger) (set))
               (set (.getAdaptiveRefreshTriggers topology-options))))))

    (testing "with not valid option"
      (is (thrown? ExceptionInfo
                   (#'conn/enable-adaptive-refresh-triggers
              (ClusterTopologyRefreshOptions/builder)
              {:enable-adaptive-refresh-trigger :broken}))))))

(deftest ^:unit test-timeout-options
  (testing "empty options creates valid TimeoutOptions with defaults"
    (is (instance? TimeoutOptions (#'conn/timeout-options {})))
    (is (not (.isTimeoutCommands (#'conn/timeout-options {})))))

  (testing "timeout-commands flag is correctly set"
    (is (.isTimeoutCommands
          (#'conn/timeout-options {:fixed-timeout {:timeout 5 :unit :seconds}
                                   :timeout-commands true})))
    (is (not (.isTimeoutCommands
               (#'conn/timeout-options {:fixed-timeout {:timeout 5 :unit :seconds}
                                        :timeout-commands false})))))

  (testing "fixed timeout duration is correctly set"
    (let [timeout-source (-> {:fixed-timeout    {:timeout 100 :unit :nanoseconds}
                              :timeout-commands true}
                             (#'conn/timeout-options)
                             (.getSource))]
      (is (instance? TimeoutOptions$FixedTimeoutSource timeout-source))
      (is (= 100 (.getTimeout timeout-source nil))))))
