(ns celtuce.args.sort
  (:import
   (io.lettuce.core SortArgs)))

(defn sort-args 
  "Creates a SortArgs object for Redis SORT command with optional sorting criteria."
  [& {by :by [offset count :as limit] :limit get :get 
      asc :asc desc :desc alpha :alpha}]
  {:pre []}
  (cond-> (SortArgs.)
    by    (.by by)
    limit (.limit offset count)
    get   (.get get)
    asc   (.asc)
    desc  (.desc)
    alpha (.alpha)))

