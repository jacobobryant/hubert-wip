(ns hub.tests.core
  (:require
    [clojure.test :as t :refer [deftest is]]))

(deftest test-foo
  (is (= 4 (+ 2 2))))

(defn run [_]
  (t/run-all-tests #"hub.tests.*"))
