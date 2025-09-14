(ns celtuce.args.geo
  (:import 
   (io.lettuce.core
    GeoArgs GeoArgs$Unit GeoArgs$Sort GeoRadiusStoreArgs)))

(defn ->unit
  "Converts distance unit keyword to GeoArgs$Unit enum.
   Accepts :m (meters), :km (kilometers), :ft (feet), or :mi (miles)."
  ^GeoArgs$Unit
  [u]
  (case u
    :m  GeoArgs$Unit/m
    :km GeoArgs$Unit/km
    :ft GeoArgs$Unit/ft
    :mi GeoArgs$Unit/mi
    (throw (ex-info "Invalid Unit" {:unit u :valids #{:m :km :ft :mi}}))))

(defn ->sort
  "Converts sort order keyword to GeoArgs$Sort enum.
   Accepts :asc (ascending), :desc (descending), or :none (unsorted)."
  ^GeoArgs$Sort
  [s]
  (case s
    :asc   GeoArgs$Sort/asc
    :desc  GeoArgs$Sort/desc
    :none  GeoArgs$Sort/none
    (throw (ex-info "Invalid Sort" {:sort s :valids #{:asc :desc :none}}))))

(defn geo-args
  "Creates GeoArgs for geo radius queries with optional result modifiers.
   Options: :with-dist (include distance), :with-coord (include coordinates),
   :with-hash (include hash), :count (limit results), :sort (sort order)."
  [& {with-dist :with-dist with-coord :with-coord with-hash :with-hash
                   count :count sort :sort}]
  (cond-> (GeoArgs.)
    with-dist  (.withDistance)
    with-coord (.withCoordinates)
    with-hash  (.withHash)
    count      (.withCount count)
    sort       (.sort (->sort sort))))

(defn georadius-store-args
  "Creates GeoRadiusStoreArgs for storing geo radius query results.
   Options: :store-key (key for results), :store-dist-key (key for distances),
   :count (limit results), :sort (sort order)."
  [& {store-key :store-key store-dist-key :store-dist-key
                               count :count sort :sort}]
  (cond-> (GeoRadiusStoreArgs.)
    store-key      (.withStore store-key)
    store-dist-key (.withStoreDist store-dist-key)
    count          (.withCount count)
    sort           (.sort (->sort sort))))

