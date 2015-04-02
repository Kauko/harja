(ns harja.palvelin.palvelut.kokonaishintaiset-tyot-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            
            [harja.kyselyt.urakat :as urk-q]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.kokonaishintaiset-tyot :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]))

(def jarjestelma nil)

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start 
                     (component/system-map
                      :db (apply tietokanta/luo-tietokanta testitietokanta)
                      :http-palvelin (testi-http-palvelin)
                      :kokonaishintaiset-tyot (component/using
                                  (->kokonaishintaiset-tyot)
                                  [:http-palvelin :db])))))
  
  (testit)
  (alter-var-root #'jarjestelma component/stop))



  
(use-fixtures :once jarjestelma-fixture)

;; käyttää testidata.sql:stä tietoa
(deftest kaikki-kokonaishintaiset-tyot-haettu-oikein []
  (let [oulun-alueurakan-id (:id (first (urk-q/hae-urakoita (:db jarjestelma) (str "%Oulun alueurakka 2005-2010%"))))
        oulun-alueurakan-sopimus (ffirst (q jarjestelma 
                                            (str "SELECT id 
                                                    FROM sopimus 
                                                   WHERE urakka = " oulun-alueurakan-id
                                                         " AND paasopimus IS null")))
        kokonaishintaiset-tyot (kutsu-palvelua (:http-palvelin jarjestelma)
                                 :kokonaishintaiset-tyot +kayttaja-tero+ oulun-alueurakan-id)
        oulun-alueurakan-toiden-lkm (ffirst (q jarjestelma (str "SELECT count(*) 
                                                                  FROM kokonaishintainen_tyo kt 
                                                                       LEFT JOIN toimenpideinstanssi tpi ON kt.toimenpideinstanssi = tpi.id
                                                                  WHERE tpi.urakka = " oulun-alueurakan-id)))]
    (is (= (count kokonaishintaiset-tyot) oulun-alueurakan-toiden-lkm))
    (is (= "Oulu Talvihoito TP" (:tpi_nimi (first kokonaishintaiset-tyot))))
    (is (= "Oulu Talvihoito TP" (:tpi_nimi (last kokonaishintaiset-tyot))))
    (is (= oulun-alueurakan-sopimus (:sopimus (first kokonaishintaiset-tyot))))
    (is (= oulun-alueurakan-sopimus (:sopimus (last kokonaishintaiset-tyot))))
    (is (= 3500.0 (:summa (first kokonaishintaiset-tyot))))
    (is (= 3500.0 (:summa (last kokonaishintaiset-tyot))))))