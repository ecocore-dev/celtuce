(load-file "../../.deps-versions.clj")
(defproject dev.ecocore/celtuce-manifold celtuce-version
  :url "https://github.com/ecocore-dev/celtuce"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [dev.ecocore/celtuce-core ~celtuce-version]
                 [manifold "0.1.8"]]
  :global-vars {*warn-on-reflection* true})
