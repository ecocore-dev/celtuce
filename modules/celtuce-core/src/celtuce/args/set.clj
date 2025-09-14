(ns celtuce.args.set
  (:import 
   (io.lettuce.core SetArgs)))

(defn set-args 
  "Creates SET command arguments with optional expiration and existence conditions.
   Options: :ex (expire seconds), :px (expire milliseconds), :nx (set if not exists), :xx (set if exists)."
  [& {ex :ex px :px nx :nx xx :xx}]
  (cond-> (SetArgs.)
    ex (.ex ^long ex)
    px (.px ^long px)
    nx (.nx)
    xx (.xx)))
