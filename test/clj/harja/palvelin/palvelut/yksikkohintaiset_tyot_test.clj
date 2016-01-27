(ns harja.palvelin.palvelut.yksikkohintaiset-tyot-test
  (:require [clojure.test :refer :all]
            
            [harja.kyselyt.urakat :as urk-q]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.yksikkohintaiset-tyot :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start 
                     (component/system-map
                      :db (tietokanta/luo-tietokanta testitietokanta)
                      :http-palvelin (testi-http-palvelin)
                      :yksikkohintaiset-tyot (component/using
                                  (->Yksikkohintaiset-tyot)
                                  [:http-palvelin :db])))))
  
  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))

;; käyttää testidata.sql:stä tietoa
(deftest kaikki-yksikkohintaiset-tyot-haettu-oikein 
  (let [yksikkohintaiset-tyot (kutsu-palvelua (:http-palvelin jarjestelma)
                                              :yksikkohintaiset-tyot (oulun-urakan-tilaajan-urakanvalvoja)
                                @oulun-alueurakan-2005-2010-id)
        oulun-alueurakan-toiden-lkm (ffirst (q 
                                             (str "SELECT count(*)
                                                       FROM yksikkohintainen_tyo
                                                      WHERE urakka = " @oulun-alueurakan-2005-2010-id)))]
    (is (= (count yksikkohintaiset-tyot) oulun-alueurakan-toiden-lkm))))




