(ns celtuce.args.migrate
  (:import 
   (io.lettuce.core MigrateArgs)))

(defn migrate-args 
  "Create MigrateArgs for Redis MIGRATE command with optional parameters.
  
  The MIGRATE command atomically transfers a key from one Redis instance to another,
  optionally with COPY and REPLACE semantics.
  
  Options:
  - :copy    - Do not remove the key from the local instance (boolean)
  - :replace - Replace existing key at destination (boolean) 
  - :key     - Single key to migrate (string/keyword)
  - :keys    - Multiple keys to migrate (collection)
  
  Note: :key and :keys are mutually exclusive - specify one or the other, not both.
  
  Examples:
    (migrate-args :key \"mykey\" :copy true)
    (migrate-args :keys [\"key1\" \"key2\"] :replace true)
    (migrate-args :key \"session:123\" :copy true :replace true)"
  [& {copy :copy replace :replace k :key ks :keys}]
  {:pre [(or (and (not= nil k)  (nil? ks))
             (and (not= nil ks) (nil? k)))]}
  (cond-> (MigrateArgs.)
    copy    (.copy)
    replace (.replace)
    k       (.key k)
    ks      (.keys ^objects (into-array Object ks))))
