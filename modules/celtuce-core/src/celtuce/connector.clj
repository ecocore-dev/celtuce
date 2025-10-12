(ns celtuce.connector
  (:require
    [celtuce.args.cluster-node :as node]
    [celtuce.args.command :as command]
    [celtuce.codec :refer [nippy-codec]]
    [celtuce.commands :as cmds]
    [clojure.java.io :as io])
  (:import
    (io.lettuce.core
      ClientOptions
      ClientOptions$Builder ClientOptions$DisconnectedBehavior RedisClient
      SocketOptions SslOptions TimeoutOptions TimeoutOptions$TimeoutSource)
    (io.lettuce.core.api StatefulRedisConnection)
    (io.lettuce.core.cluster RedisClusterClient)
    (io.lettuce.core.cluster.api StatefulRedisClusterConnection)
    (io.lettuce.core.cluster
      ClusterClientOptions ClusterClientOptions$Builder
      ClusterTopologyRefreshOptions ClusterTopologyRefreshOptions$Builder ClusterTopologyRefreshOptions$RefreshTrigger)
    (io.lettuce.core.cluster.models.partitions RedisClusterNode)
    (io.lettuce.core.codec RedisCodec)
    (io.lettuce.core.dynamic RedisCommandFactory)
    (io.lettuce.core.protocol RedisCommand)
    (io.lettuce.core.pubsub RedisPubSubListener StatefulRedisPubSubConnection)
    (io.lettuce.core.resource ClientResources)
    (java.time Duration)
    (java.time.temporal ChronoUnit)
    (java.util.concurrent TimeUnit)))

;; FIXME: Incomplete RedisConnector implementations:
;;        - RedisServer: missing `set-options`
;;        - RedisCluster: missing `set-options`
;;        - RedisPubSub: missing `set-options`, `commands-dynamic`
;;       Leave as-is for now. Revisit to implement or redesign later.
(defprotocol RedisConnector
  "Manipulate Redis client and stateful connection"
  (commands-sync    [this])
  (commands-dynamic [this cmd-class])
  (flush-commands   [this])
  (set-options      [this options])
  (reset            [this])
  (shutdown         [this]))

(def kw->tunit
  "Map of keywords to Java TimeUnit enum values for time unit conversions."
  {:nanoseconds  TimeUnit/NANOSECONDS
   :microseconds TimeUnit/MICROSECONDS
   :milliseconds TimeUnit/MILLISECONDS
   :seconds      TimeUnit/SECONDS
   :minutes      TimeUnit/MINUTES
   :hours        TimeUnit/HOURS
   :days         TimeUnit/DAYS})

(def kw->cunit
  "Map of keywords to Java ChronoUnit enum values for time duration conversions."
  {:nanoseconds  ChronoUnit/NANOS
   :nanos        ChronoUnit/NANOS
   :microseconds ChronoUnit/MICROS
   :micros       ChronoUnit/MICROS
   :milliseconds ChronoUnit/MILLIS
   :millis       ChronoUnit/MILLIS
   :seconds      ChronoUnit/SECONDS
   :minutes      ChronoUnit/MINUTES
   :hours        ChronoUnit/HOURS
   :half-day     ChronoUnit/HALF_DAYS
   :days         ChronoUnit/DAYS
   :weeks        ChronoUnit/WEEKS
   :months       ChronoUnit/MONTHS
   :years        ChronoUnit/YEARS
   :decades      ChronoUnit/DECADES
   :centuries    ChronoUnit/CENTURIES
   :millennia    ChronoUnit/MILLENNIA
   :eras         ChronoUnit/ERAS
   :forever      ChronoUnit/FOREVER})

(def kw->dbehavior
  "Map of keywords to ClientOptions DisconnectedBehavior enum values."
  {:default          ClientOptions$DisconnectedBehavior/DEFAULT
   :accept-commands  ClientOptions$DisconnectedBehavior/ACCEPT_COMMANDS
   :reject-commmands ClientOptions$DisconnectedBehavior/REJECT_COMMANDS})

(def kw->rtrigger
  "Map of keywords to ClusterTopologyRefreshOptions RefreshTrigger enum values."
  {:moved-redirect
   ClusterTopologyRefreshOptions$RefreshTrigger/MOVED_REDIRECT
   :ask-redirect
   ClusterTopologyRefreshOptions$RefreshTrigger/ASK_REDIRECT
   :persistent-reconnects
   ClusterTopologyRefreshOptions$RefreshTrigger/PERSISTENT_RECONNECTS})

(defn- socket-options
  "Internal helper to build SocketOptions, used by b-client-options"
  ^SocketOptions
  [opts]
  (cond-> (SocketOptions/builder)
    (and (contains? opts :timeout)
         (contains? opts :unit))
    (.connectTimeout (:timeout opts) (kw->tunit (:unit opts)))
    (contains? opts :keep-alive)
    (.keepAlive ^boolean (:keep-alive opts))
    (contains? opts :tcp-no-delay)
    (.tcpNoDelay (:tcp-no-delay opts))
    true (.build)))

(defn- ssl-options
  "Internal helper to build SslOptions, used by b-client-options"
  ^SslOptions
  [opts]
  (cond-> (SslOptions/builder)
    ;; provider setup
    (contains? opts :provider)
    (cond->
        (= :open-ssl (:provider opts)) (.openSslProvider)
        (= :jdk      (:provider opts)) (.jdkSslProvider))
    ;; keystore setup
    (contains? opts :keystore)
    (cond->
        (and (contains? (:keystore opts) :file)
             (contains? (:keystore opts) :password))
      (.keystore (io/as-file (-> opts :keystore :file))
                 (chars      (-> opts :keystore :password)))
      (contains? (:keystore opts) :file)
      (.keystore (io/as-file (-> opts :keystore :file)))
      (and (contains? (:keystore opts) :url)
           (contains? (:keystore opts) :password))
      (.keystore (io/as-url (-> opts :keystore :url))
                 (chars     (-> opts :keystore :password)))
      (contains? (:keystore opts) :url)
      (.keystore (io/as-url (-> opts :keystore :url))))
    ;; truststore setup
    (contains? opts :truststore)
    (cond->
        (and (contains? (:truststore opts) :file)
             (contains? (:truststore opts) :password))
      (.truststore (io/as-file (-> opts :truststore :file))
                   (-> opts :truststore :password str))
      (contains? (:truststore opts) :file)
      (.truststore (io/as-file (-> opts :truststore :file)))
      (and (contains? (:truststore opts) :url)
           (contains? (:truststore opts) :password))
      (.truststore (io/as-url (-> opts :truststore :url))
                   ^String (-> opts :truststore :password str))
      (contains? (:truststore opts) :url)
      (.truststore (io/as-url (-> opts :truststore :url))))
    ;; finally, build
    true (.build)))


(defn- timeout-source
  [timeout-fn]
  (proxy [TimeoutOptions$TimeoutSource] []
    (getTimeout [^RedisCommand command]
      (-> command
          (.getType)
          (command/command-type->kw)
          (timeout-fn)))))


(defn- timeout-options
  "Internal helper to build TimeoutOptions, used by b-client-options"
  ^TimeoutOptions
  [{:keys [fixed-timeout] :as opts}]
  (cond-> (TimeoutOptions/builder)
    (and (contains? fixed-timeout :timeout)
         (contains? fixed-timeout :unit))
    (.fixedTimeout
      (Duration/of
        (:timeout fixed-timeout)
        (-> fixed-timeout
            :unit
            (kw->cunit))))

    (contains? opts :timeout-commands)
    (.timeoutCommands (:timeout-commands opts))

    (true? (:connection-timeout opts))
    (.connectionTimeout)

    (contains? opts :get-timeout)
    (.timeoutSource
      (-> opts
          :get-timeout
          (timeout-source)))

    :always (.build)))


(defn- b-client-options
  "Sets up a ClientOptions builder from a map of options"
  (^ClientOptions$Builder [opts]
   (b-client-options (ClientOptions/builder) opts))
  (^ClientOptions$Builder [^ClientOptions$Builder builder opts]
   (cond-> builder
     (contains? opts :ping-before-activate-connection)
     (.pingBeforeActivateConnection (:ping-before-activate-connection opts))
     (contains? opts :auto-reconnect)
     (.autoReconnect (:auto-reconnect opts))
     (contains? opts :suspend-reconnect-on-protocol-failure)
     (.suspendReconnectOnProtocolFailure (:suspend-reconnect-on-protocol-failure opts))
     (contains? opts :cancel-commands-on-reconnect-failure)
     (.cancelCommandsOnReconnectFailure (:cancel-commands-on-reconnect-failure opts))
     (contains? opts :request-queue-size)
     (.requestQueueSize (:request-queue-size opts))
     (contains? opts :disconnected-behavior)
     (.disconnectedBehavior (-> opts :disconnected-behavior kw->dbehavior))
     (contains? opts :socket-options)
     (.socketOptions (socket-options (:socket-options opts)))
     (contains? opts :timeout-options)
     (.timeoutOptions (timeout-options (:timeout-options opts)))
     (contains? opts :ssl-options)
     (.sslOptions (ssl-options (:ssl-options opts))))))


(defn- enable-adaptive-refresh-triggers
  "Enable adaptive refresh triggers on the given ClusterTopologyRefreshOptions builder.

   Expects :enable-adaptive-refresh-trigger in `opts` to be either:
   - `:all` — enables all adaptive refresh triggers (kept for backward compatibility);
   - a collection (vector/list/set) of trigger keywords — enables exactly those triggers.

   Accepts in `opts`:
   - :enable-all-adaptive-refresh-triggers true  → enable all
   - :enable-adaptive-refresh-triggers coll      → enable exactly those triggers
   - :enable-adaptive-refresh-trigger :all|coll  → (back-compat) enable all or listed

   Returns the same builder for chaining."
  ^ClusterTopologyRefreshOptions$Builder
  [^ClusterTopologyRefreshOptions$Builder builder opts]
  (let [refresh-triggers (or (:enable-adaptive-refresh-triggers opts)
                             (:enable-adaptive-refresh-trigger opts))]
    (cond
      (or (:enable-all-adaptive-refresh-triggers opts)
          (= :all refresh-triggers))
      (.enableAllAdaptiveRefreshTriggers builder)

      (coll? refresh-triggers)
      (.enableAdaptiveRefreshTrigger
        builder
        (->> refresh-triggers
             (set)
             (map kw->rtrigger)
             (into-array ClusterTopologyRefreshOptions$RefreshTrigger)))

      :else (throw
              (ex-info "Unsupported value for `:enable-adaptive-refresh-trigger`"
                       {:value refresh-triggers})))))


(defn- cluster-topo-refresh-options
  "Internal helper to build ClusterTopologyRefreshOptions,
   used by b-cluster-client-options."
  ^ClusterTopologyRefreshOptions
  [opts]
  (cond-> (ClusterTopologyRefreshOptions/builder)
    (and (contains? opts :enable-periodic-refresh)
         (true? (:enable-periodic-refresh opts))
         (contains? opts :refresh-period))
    (.enablePeriodicRefresh
     (Duration/of (-> opts :refresh-period :period)
                  (-> opts :refresh-period :unit kw->cunit)))

    (contains? opts :close-stale-connections)
    (.closeStaleConnections (:close-stale-connections opts))

    (contains? opts :dynamic-refresh-sources)
    (.dynamicRefreshSources (:dynamic-refresh-sources opts))

    (or
      (contains? opts :enable-all-adaptive-refresh-triggers)
      (contains? opts :enable-adaptive-refresh-triggers)
      (contains? opts :enable-adaptive-refresh-trigger))
    (enable-adaptive-refresh-triggers opts)

    (contains? opts :adaptive-refresh-triggers-timeout)
    (.adaptiveRefreshTriggersTimeout
     (Duration/of
      (-> opts :adaptive-refresh-triggers-timeout :timeout)
      (-> opts :adaptive-refresh-triggers-timeout :unit kw->cunit)))

    (contains? opts :refresh-triggers-reconnect-attempts)
    (.refreshTriggersReconnectAttempts (:refresh-triggers-reconnect-attempts opts))

    :always (.build)))


(defn- cluster-node-filter
  "Transforms a Clojure predicate function that operates on node maps
   into a Java function compatible with `ClusterClientOptions.nodeFilter`.
   The predicate receives a map representation of cluster node properties."
  [node-filter-fn]
  (fn [^RedisClusterNode node]
    (-> node
        (node/node->map)
        (node-filter-fn)
        (boolean))))


(defn- b-cluster-client-options
  "Sets up a ClusterClientOptions builder from a map of options.
   Supported keys:
   - :validate-cluster-node-membership (boolean)
   - :max-redirects (int)
   - :topology-refresh-options (map)
   - :node-filter (fn of node->map -> boolean)"
  ^ClusterClientOptions$Builder
  [{:keys [validate-cluster-node-membership
           max-redirects
           topology-refresh-options
           node-filter] :as opts}]
  (cond-> (ClusterClientOptions/builder)
    (some? validate-cluster-node-membership)
    (.validateClusterNodeMembership (boolean validate-cluster-node-membership))

    (some? max-redirects)
    (.maxRedirects (int max-redirects))

    (some? topology-refresh-options)
    (.topologyRefreshOptions
      (cluster-topo-refresh-options topology-refresh-options))

    (some? node-filter)
    (.nodeFilter
      (cluster-node-filter node-filter))

    :always (b-client-options opts)))


(defn create-client-resource
  "You can create an instance of client resources in a clojuresque way; check out the
  class io.lettuce.core.resource.ClientResources for details.

  It is useful to configure \"plumbing\" of client side redis connections such as: Netty
  threads, metrics, etc. But also it is good to have it for sharing the same NIO layer
  across multiple connections.

  Currently only the number of threads are implemented. Also, you can call it without
  any param or with an empty map and it will create a default client resource, but that
  can be shared across client connections."
  [options-map]
  (let [builder (ClientResources/builder)]
    (cond-> builder
      (contains? options-map :nb-io-threads)
      (.ioThreadPoolSize (:nb-io-threads options-map))
      (contains? options-map :nb-worker-threads)
      (.computationThreadPoolSize (:nb-worker-threads options-map)))
    (.build builder)))

(defn destroy-client-resource
  "If you create a client resource, you must close/dispose it; otherwise you will not
  shutdown the Netty threads."
  [^ClientResources client-resources]
  (.shutdown client-resources 100 1000 TimeUnit/MILLISECONDS))

;;
;; Redis Server
;;

(defrecord RedisServer
    [^RedisClient redis-client
     client-options
     ^StatefulRedisConnection stateful-conn
     conn-options
     ^RedisCodec codec
     ^RedisCommandFactory dynamic-factory]
  RedisConnector
  (commands-sync [_]
    (locking clojure.lang.RT/REQUIRE_LOCK
      (require '[celtuce.impl.server]))
    (.sync stateful-conn))
  (commands-dynamic [_ cmd-class]
    (.getCommands dynamic-factory cmd-class))
  (flush-commands [_]
    (.flushCommands stateful-conn))
  (reset [_]
    (.reset stateful-conn))
  (shutdown [_]
    (.close stateful-conn)
    (.shutdown redis-client)))

(defn redis-server
  "Creates a Redis server connection with optional configuration."
  [^String redis-uri &
   {codec :codec
    client-options :client-options
    {auto-flush :auto-flush
     conn-timeout :timeout
     conn-unit :unit ^ClientResources
     client-resources :client-resources
     :or
     {auto-flush true}
     } :conn-options
    :or
    {codec (nippy-codec)
     client-options {}}}]
  (let [redis-client (if (nil? client-resources)
                       (RedisClient/create redis-uri)
                       (RedisClient/create client-resources redis-uri))
        _ (.setOptions redis-client (.build (b-client-options client-options)))
        stateful-conn (.connect redis-client ^RedisCodec codec)]
    (when (and conn-timeout conn-unit)
      (.setTimeout stateful-conn (Duration/of  conn-timeout (kw->cunit conn-unit))))
    (.setAutoFlushCommands stateful-conn auto-flush)
    (map->RedisServer
     {:redis-client   redis-client
      :client-options client-options
      :codec          codec
      :stateful-conn  stateful-conn
      :conn-options   {:auto-flush auto-flush
                       :timeout    conn-timeout
                       :unit       conn-unit}
      :dynamic-factory (RedisCommandFactory. stateful-conn)})))

;;
;; Redis Cluster
;;

(defrecord RedisCluster
    [^RedisClusterClient redis-client
     client-options
     ^StatefulRedisClusterConnection stateful-conn
     conn-options
     ^RedisCodec codec
     ^RedisCommandFactory dynamic-factory]
  RedisConnector
  (commands-sync [_]
    (locking clojure.lang.RT/REQUIRE_LOCK
      (require '[celtuce.impl.cluster]))
    (.sync stateful-conn))
  (commands-dynamic [_ cmd-class]
    (.getCommands dynamic-factory cmd-class))
  (flush-commands [_]
    (.flushCommands stateful-conn))
  (reset [_]
    (.reset stateful-conn))
  (shutdown [_]
    (.close stateful-conn)
    (.shutdown redis-client)))

(defn redis-cluster
  "Creates a Redis cluster connection with optional configuration."
  [^String redis-uri &
   {codec :codec
    client-options :client-options
    {auto-flush :auto-flush
     conn-timeout :timeout
     conn-unit :unit ^ClientResources
     client-resources :client-resources
     :or
     {auto-flush true}
     } :conn-options
    :or
    {codec (nippy-codec)
     client-options {}}}]
  (let [redis-client (if (nil? client-resources)
                       (RedisClusterClient/create redis-uri)
                       (RedisClusterClient/create client-resources redis-uri))
        _ (.setOptions redis-client
                       (.build (b-cluster-client-options client-options)))
        stateful-conn (.connect redis-client codec)]
    (when (and conn-timeout conn-unit)
      (.setTimeout stateful-conn (Duration/of  conn-timeout (kw->cunit conn-unit))))
    (.setAutoFlushCommands stateful-conn auto-flush)
    (map->RedisCluster
     {:redis-client   redis-client
      :client-options client-options
      :codec          codec
      :stateful-conn  stateful-conn
      :conn-options   {:auto-flush auto-flush
                       :timeout    conn-timeout
                       :unit       conn-unit}
      :dynamic-factory (RedisCommandFactory. stateful-conn)})))

;;
;; Redis PubSub
;;

(defprotocol Listenable
  "Register a celtuce.commands.PubSubListener on a stateful pubsub connection"
  (add-listener! [this listener]))

(defrecord RedisPubSub
    [redis-client ^StatefulRedisPubSubConnection stateful-conn codec]
  RedisConnector
  (commands-sync [_]
    (locking clojure.lang.RT/REQUIRE_LOCK
      (require '[celtuce.impl.pubsub]))
    (.sync stateful-conn))
  (flush-commands [_]
    (.flushCommands stateful-conn))
  (reset [_]
    (.reset stateful-conn))
  (shutdown [_]
    (.close stateful-conn)
    (condp instance? redis-client
      RedisClusterClient (.shutdown ^RedisClusterClient redis-client)
      RedisClient        (.shutdown ^RedisClient        redis-client)))
  Listenable
  (add-listener! [_ listener]
    (.addListener
     stateful-conn
     (reify
       RedisPubSubListener
       (message [_ ch msg]
         (cmds/message listener ch msg))
       (message [_ p ch msg]
         (cmds/message listener p ch msg))
       (subscribed [_ ch cnt]
         (cmds/subscribed listener ch cnt))
       (unsubscribed [_ ch cnt]
         (cmds/unsubscribed listener ch cnt))
       (psubscribed [_ p cnt]
         (cmds/psubscribed listener p cnt))
       (punsubscribed [_ p cnt]
         (cmds/punsubscribed listener p cnt))))))

(defn as-pubsub
  "Converts a Redis connection to a pub/sub connection."
  [{:keys [redis-client ^RedisCodec codec]}]
  (->RedisPubSub
   redis-client
   (condp instance? redis-client
     RedisClusterClient (.connectPubSub ^RedisClusterClient redis-client codec)
     RedisClient        (.connectPubSub ^RedisClient        redis-client codec))
   codec))
