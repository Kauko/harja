(ns harja.pvm-test
  "Harjan pvm-namespacen backendin testit"
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [harja.pvm :as pvm]
    [clj-time.core :as t]))

(deftest ennen?
  (is (false? (pvm/ennen? nil nil)))
  (is (false? (pvm/ennen? (t/now) nil)))
  (is (false? (pvm/ennen? nil (t/now))))
  (is (false? (pvm/ennen? (t/now) (t/now))))
  (is (false? (pvm/ennen? (t/plus (t/now) (t/hours 4))
                          (t/now))))
  (is (true? (pvm/ennen? (t/now)
                         (t/plus (t/now) (t/hours 4))))))

(deftest sama-tai-ennen?
  (is (false? (pvm/sama-tai-ennen? nil nil)))
  (is (false? (pvm/sama-tai-ennen? (t/now) nil)))
  (is (false? (pvm/sama-tai-ennen? nil (t/now))))
  (is (false? (pvm/sama-tai-ennen? (t/now) (t/now))))
  (is (false? (pvm/sama-tai-ennen? (t/plus (t/now) (t/hours 4))
                                   (t/now)
                                   false)))
  (is (true? (pvm/sama-tai-ennen? (t/plus (t/now) (t/hours 4))
                                   (t/now))))
  (is (true? (pvm/sama-tai-ennen? (t/now)
                                  (t/now))))
  (is (true? (pvm/sama-tai-ennen? (t/now)
                                  (t/plus (t/now) (t/hours 4))))))

(deftest jalkeen?
  (is (false? (pvm/jalkeen? nil nil)))
  (is (false? (pvm/jalkeen? (t/now) nil)))
  (is (false? (pvm/jalkeen? nil (t/now))))
  (is (false? (pvm/jalkeen? (t/now) (t/now))))
  (is (false? (pvm/jalkeen? (t/now)
                            (t/plus (t/now) (t/hours 4)))))
  (is (true? (pvm/jalkeen? (t/plus (t/now) (t/hours 4))
                           (t/now)))))

(deftest sama-tai-jalkeen?
  (is (false? (pvm/sama-tai-jalkeen? nil nil)))
  (is (false? (pvm/sama-tai-jalkeen? (t/now) nil)))
  (is (false? (pvm/sama-tai-jalkeen? nil (t/now))))
  (is (false? (pvm/sama-tai-jalkeen? (t/now) (t/now))))
  (is (false? (pvm/sama-tai-jalkeen? (t/now)
                                     (t/plus (t/now) (t/hours 4))
                                     false)))
  (is (true? (pvm/sama-tai-jalkeen? (t/now)
                                     (t/plus (t/now) (t/hours 4))
                                     true)))
  (is (true? (pvm/sama-tai-jalkeen? (t/now)
                                    (t/now))))
  (is (true? (pvm/sama-tai-jalkeen? (t/plus (t/now) (t/hours 4))
                                    (t/now)))))

(deftest valissa?
  (is (true? (pvm/valissa? (t/now) (t/now) (t/now))))
  (is (true? (pvm/valissa? (t/now)
                           (t/minus (t/now) (t/minutes 5))
                           (t/plus (t/now) (t/minutes 5)))))
  (is (true? (pvm/valissa? (t/plus (t/now) (t/minutes 8))
                           (t/minus (t/now) (t/minutes 5))
                           (t/plus (t/now) (t/minutes 5)))))
  (is (true? (pvm/valissa? (t/plus (t/now) (t/minutes 15))
                            (t/minus (t/now) (t/minutes 5))
                            (t/plus (t/now) (t/minutes 5))
                            true)))
  (is (false? (pvm/valissa? (t/plus (t/now) (t/minutes 15))
                           (t/minus (t/now) (t/minutes 5))
                           (t/plus (t/now) (t/minutes 5))
                            false))))