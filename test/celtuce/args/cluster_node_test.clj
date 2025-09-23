(ns celtuce.args.cluster-node-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [celtuce.args.cluster-node :as sut])
  (:import
    (clojure.lang ExceptionInfo)
    (java.util.concurrent TimeUnit)))


(deftest ^:unit test-enum->kw
  (let [time-unit->kw {TimeUnit/MILLISECONDS :milliseconds}]
    (testing "valid enum mapping returns correct keyword"
      (is (= :milliseconds
             (sut/enum->kw time-unit->kw TimeUnit/MILLISECONDS))))
    (testing "missing enum throws exception with descriptive message"
      (is (thrown-with-msg? ExceptionInfo #"Unknown enum member: MINUTES"
                            (sut/enum->kw time-unit->kw TimeUnit/MINUTES))))))
