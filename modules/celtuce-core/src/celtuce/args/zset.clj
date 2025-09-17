(ns celtuce.args.zset
  (:import
   (io.lettuce.core ZAddArgs ZAddArgs$Builder ZStoreArgs)))

(defn zadd-args ^ZAddArgs [opt]
  (case opt
    :nx (ZAddArgs$Builder/nx)
    :xx (ZAddArgs$Builder/xx)
    :ch (ZAddArgs$Builder/ch)
    (throw (ex-info "invalid zadd opt" {:opt opt}))))

(defn zstore-args [agg & weights]
  (cond-> (ZStoreArgs.)
    (not (empty? weights)) (.weights ^doubles (into-array Double/TYPE weights))
    (= :sum agg) (.sum)
    (= :min agg) (.min)
    (= :max agg) (.max)))
