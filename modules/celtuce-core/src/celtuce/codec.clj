(ns celtuce.codec
  (:require 
   [carbonite.api :refer [default-registry]]
   [taoensso.nippy :as nippy])
  (:import 
   (io.lettuce.core.codec 
    RedisCodec Utf8StringCodec ByteArrayCodec 
    CompressionCodec CompressionCodec$CompressionType)
   (java.io ByteArrayOutputStream ByteArrayInputStream)
   (java.nio ByteBuffer)
   (com.esotericsoftware.kryo Kryo)
   (com.esotericsoftware.kryo.io Output Input)
   (com.esotericsoftware.kryo.pool KryoFactory KryoPool KryoPool$Builder)))

(defn ^"[B" bb->bytes 
  "Converts a ByteBuffer to a byte array."
  [^ByteBuffer bb]
  (let [bytes (byte-array (.remaining bb))]
    (.get bb bytes)
    bytes))

(defn bytes->bb 
  "Converts a byte array to a ByteBuffer."
  ^ByteBuffer [^bytes b]
  (ByteBuffer/wrap b))

;; Lettuce codecs

(defn utf8-string-codec 
  "Creates a UTF-8 string codec for Redis commands."
  []
  (Utf8StringCodec.))

(defn byte-array-codec 
  "Creates a byte array codec for Redis commands."
  []
  (ByteArrayCodec/INSTANCE))

(defn compression-codec 
  "Creates a compression codec that wraps a delegate codec with gzip or deflate compression."
  [^RedisCodec delegate-codec compression-type]
  {:pre [(#{:gzip :deflate} compression-type)]}
  (let [type (case compression-type
               :gzip    CompressionCodec$CompressionType/GZIP
               :deflate CompressionCodec$CompressionType/DEFLATE)]
    (CompressionCodec/valueCompressor delegate-codec type)))

;; Carbonite based codec

(defn kryo-read 
  "Deserialize obj from ByteBuffer bb"
  [^Kryo kryo bb]
  (with-open [in (Input. (ByteArrayInputStream. (bb->bytes bb)))]
    (.readClassAndObject kryo in)))

(defn kryo-write 
  "Serialize obj to ByteBuffer"
  [^Kryo kryo obj]
  (let [bos (ByteArrayOutputStream.)]
    (with-open [out (Output. bos)]
      (.writeClassAndObject kryo out obj))
    (bytes->bb (.toByteArray bos))))

(defn kryos-pool
  "Kryo objects pool with soft references to allow for GC when running out of memory"
  [kryo-factory]
  (-> (proxy [KryoFactory] []
        (create []
          (kryo-factory)))
      (KryoPool$Builder.)
      .softReferences
      .build))

(defmacro with-kryos-pool 
  "Inject a Kryo object from kryo-pool as the first parameter of form and run it"
  [kryo-pool form]
  (let [kryo-pool (vary-meta kryo-pool assoc :tag `KryoPool)]
    `(let [kryo# (.borrow ~kryo-pool)
           res# (-> kryo# ~form)]
       (.release ~kryo-pool kryo#)
       res#)))

(defn carbonite-codec 
  "Creates a Redis codec using Carbonite for Kryo-based serialization."
  ([]
   (carbonite-codec (fn [] (default-registry))))
  ([kryo-factory]
   (let [kryos (kryos-pool kryo-factory)]
     (proxy [RedisCodec] []
       (decodeKey [bb]
         (with-kryos-pool kryos (kryo-read bb)))
       (decodeValue [bb]
         (with-kryos-pool kryos (kryo-read bb)))
       (encodeKey [k]
         (with-kryos-pool kryos (kryo-write k)))
       (encodeValue [v]
         (with-kryos-pool kryos (kryo-write v)))))))

;; Nippy based codec

(defn nippy-codec
  "Creates a Redis codec using Nippy for serialization with optional freeze and thaw options."
  ([]
   (nippy-codec nil nil))
  ([freeze-opts thaw-opts]
   (proxy [RedisCodec] []
     (decodeKey [bb]
       (nippy/thaw (bb->bytes bb) thaw-opts))
     (decodeValue [bb]
       (nippy/thaw (bb->bytes bb) thaw-opts))
     (encodeKey [k]
       (bytes->bb (nippy/freeze k freeze-opts)))
     (encodeValue [v]
       (bytes->bb (nippy/freeze v freeze-opts))))))

