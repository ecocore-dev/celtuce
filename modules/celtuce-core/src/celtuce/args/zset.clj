(ns celtuce.args.zset
  (:import
   (io.lettuce.core ZAddArgs ZAddArgs$Builder ZStoreArgs)))

(defn zadd-args 
  "Creates ZADD command arguments with conditional behavior.
   Options: :nx (add if not exists), :xx (update if exists), :ch (return count of changed elements)."
  ^ZAddArgs [opt]
  (case opt
    :nx (ZAddArgs$Builder/nx)
    :xx (ZAddArgs$Builder/xx)
    :ch (ZAddArgs$Builder/ch)
    (throw (ex-info "invalid zadd opt" {:opt opt}))))

(defn zstore-args 
  "Creates arguments for ZSTORE operations with aggregation function and optional weights.
   Aggregation options: :sum, :min, :max. Weights are applied to corresponding sets."
  [agg & weights]
  (cond-> (ZStoreArgs.)
    (seq weights) (.weights ^doubles (into-array Double/TYPE weights))
    (= :sum agg) (.sum)
    (= :min agg) (.min)
    (= :max agg) (.max)))
