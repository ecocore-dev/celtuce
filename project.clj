(load-file ".deps-versions.clj")
(defproject dev.ecocore/celtuce celtuce-version
  :description "An idiomatic Clojure Redis client wrapping the Java client Lettuce"
  :url "https://github.com/ecocore-dev/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [dev.ecocore/celtuce-core ~celtuce-version]
                 [dev.ecocore/celtuce-pool ~celtuce-version]
                 [dev.ecocore/celtuce-manifold ~celtuce-version]]
  :global-vars {*warn-on-reflection* true})
