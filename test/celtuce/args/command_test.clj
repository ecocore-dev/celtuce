(ns celtuce.args.command-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [celtuce.args.command :as sut])
  (:import
    (io.lettuce.core.protocol CommandType)))


(set! *warn-on-reflection* true)


(deftest ^:unit test-command-type->kw
  (testing "with a simple command"
    (is (= :get (sut/command-type->kw CommandType/GET))))

  (testing "replaces underscores with dashes"
    (is (= :sort-ro (sut/command-type->kw CommandType/SORT_RO))))

  (testing "replaces dots with dashes (JSON commands)"
    (is (= :json-get (sut/command-type->kw CommandType/JSON_GET))))

  (testing "with nil"
    (is (nil? (sut/command-type->kw nil)))))
