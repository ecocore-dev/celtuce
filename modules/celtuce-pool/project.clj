(load-file "../../.deps-versions.clj")
(defproject dev.ecocore/celtuce-pool celtuce-version
  :url "https://github.com/ecocore-dev/celtuce"
  :scm {:dir ".."}
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure ~clj-version]
                 [dev.ecocore/celtuce-core ~celtuce-version]
                 [org.apache.commons/commons-pool2 "2.9.0"]]
  :global-vars {*warn-on-reflection* true})
