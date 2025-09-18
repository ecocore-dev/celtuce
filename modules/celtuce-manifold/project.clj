(load-file "../../.deps-versions.clj")
(defproject dev.ecocore/celtuce-manifold celtuce-version
  :description "Manifold async integration for Celtuce Redis client"
  :url "https://github.com/ecocore-dev/celtuce"
  :scm {:dir ".."}
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [dev.ecocore/celtuce-core ~celtuce-version]
                 [manifold "0.4.3"]]
  :global-vars {*warn-on-reflection* true})
