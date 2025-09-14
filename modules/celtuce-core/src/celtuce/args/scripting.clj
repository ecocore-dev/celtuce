(ns celtuce.args.scripting
  (:import 
   (io.lettuce.core ScriptOutputType)))

(defn output-type ^ScriptOutputType [t]
  (case t
    :boolean ScriptOutputType/BOOLEAN
    :integer ScriptOutputType/INTEGER
    :multi   ScriptOutputType/MULTI
    :status  ScriptOutputType/STATUS
    :value   ScriptOutputType/VALUE
    (throw (ex-info "invalid scripting output type" {:type t}))))
