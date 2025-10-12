(ns celtuce.args.command
  (:require
    [clojure.string :as str])
  (:import
    (io.lettuce.core.protocol CommandType)))


(set! *warn-on-reflection* true)


(defn command-type->kw
  "Converts CommandType to a kebab-case keyword."
  [^CommandType type]
  (some-> type
          (str)
          (str/replace #"[\._\s]+" "-")
          (str/lower-case)
          (keyword)))
