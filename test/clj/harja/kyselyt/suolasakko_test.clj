(ns harja.kyselyt.suolasakko-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [harja.testi :refer :all]
            [clojure.test.check :as tc]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))


(use-fixtures :each urakkatieto-fixture)

(deftest laske-urakan-suolasakko
  (let [ur @oulun-alueurakan-2014-2019-id]
    (testing "Testidatan Oulun alueurakka 2014 - 2019 lasketaan oikein"
      (is (= -2280.0M
             (ffirst (q (str "SELECT hoitokauden_suolasakko(" ur ", '2014-10-01','2015-09-30')"))))))))

(defn suolasakko [ur lampotila lampotila-pitka sakko-per-tonni sallittu-maara kaytetty-maara]
  ;; Muokkaa oulun alueurakan testidatan toteumia
  (u (str "UPDATE lampotilat SET keskilampotila = " lampotila ", pitka_keskilampotila = " lampotila-pitka " WHERE urakka = " ur " AND alkupvm='2014-10-01'"))
  (u (str "UPDATE suolasakko SET maara=" sakko-per-tonni ", talvisuolaraja=" sallittu-maara " WHERE urakka=" ur " AND hoitokauden_alkuvuosi=2014"))
  (u (str "UPDATE toteuma_materiaali SET maara = " kaytetty-maara " WHERE toteuma IN (SELECT id FROM toteuma WHERE urakka = " ur ")"))
  (Math/abs (double (ffirst (q (str "SELECT hoitokauden_suolasakko(" ur ", '2014-10-01','2015-09-30')"))))))

  
(defspec muuta-sakon-maaraa
  100
  ;; Muuta sakon laskennassa käytettyjä arvoja oulun alueurakkaan:
  ;; - lämpötila ja  pitkä lämpötila
  ;; - sakko per ylittävä tonni
  ;; - sallittua käyttömäärää
  ;; - toteumaa
  ;; varmista, että sakko on aina oikein laskettu
  (prop/for-all [;; luodaan lämpötilat -40.0 ja +5.0 välillä
                 lampotila  (gen/fmap #(/ % 10.0) (gen/choose -400 50))
                 lampotila-pitka (gen/fmap #(/ % 10.0) (gen/choose -400 50))
                 sakko-per-tonni (gen/choose 1 100) ; gen/s-pos-int
                 sallittu-maara (gen/choose 1 10000)
                 kaytetty-maara (gen/choose 1 10000)]

                (let [lampotila (bigdec lampotila)
                      lampotila-pitka (bigdec lampotila-pitka)
                      erotus (- lampotila lampotila-pitka)
                      sal (cond
                            (>= erotus 4) (* sallittu-maara 1.30)
                            (>= erotus 3) (* sallittu-maara 1.20)
                            (> erotus 2) (* sallittu-maara 1.10)
                            :default sallittu-maara)]
                  (=marginaalissa?
                                (if (> kaytetty-maara (* sal 1.05))
                                    (* sakko-per-tonni (- kaytetty-maara (* sal 1.05)))
                                    0.0)
                                  (suolasakko @oulun-alueurakan-2014-2019-id
                                              lampotila
                                              lampotila-pitka
                                              sakko-per-tonni
                                              sallittu-maara
                                              kaytetty-maara)
                                0.01))))


