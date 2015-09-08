(ns harja.palvelin.palvelut.laskutusyhteenveto_test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.raportit :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.kyselyt.konversio :as konv]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (apply tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :hae-laskutusyhteenvedon-tiedot (component/using
                                                          (->Raportit)
                                                          [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures
                      tietokanta-fixture
                      (compose-fixtures jarjestelma-fixture urakkatieto-fixture)))

(deftest laskutusyhteenvedon-tietojen-haku
  (testing "laskutusyhteenvedon-tietojen-haku"
    (let [payload {:urakka-id          @oulun-alueurakan-2014-2019-id
                   :hk-alkupvm         (java.sql.Date. 114 9 1)
                   :hk-loppupvm        (java.sql.Date. 115 8 30)
                   :aikavali-alkupvm   (java.sql.Date. 115 7 1)
                    :aikavali-loppupvm (java.sql.Date. 115 7 30)}
          haetut-tiedot (kutsu-palvelua (:http-palvelin jarjestelma)
                                        :hae-laskutusyhteenvedon-tiedot
                                        +kayttaja-jvh+
                                        payload)
          haetut-tiedot-talvihoito (first (filter #(= (:tuotekoodi %) "23100") haetut-tiedot))
          haetut-tiedot-liikenneymparisto (first (filter #(= (:tuotekoodi %) "23110") haetut-tiedot))
          haetut-tiedot-soratiet (first (filter #(= (:tuotekoodi %) "23120") haetut-tiedot))
          _ (log/debug "haetut-tiedot-talvihoito" haetut-tiedot-talvihoito)
          _ (log/debug "haetut-tiedot-liikenneymparisto" haetut-tiedot-liikenneymparisto)
          _ (log/debug "haetut-tiedot-soratiet" haetut-tiedot-soratiet)

          odotetut-talvihoito {:erilliskustannukset_laskutetaan                 1000.0
                               :erilliskustannukset_laskutetaan_ind_korotettuna 1062.0
                               :erilliskustannukset_laskutetaan_ind_korotus     62.0
                               :erilliskustannukset_laskutettu                  1000.0
                               :erilliskustannukset_laskutettu_ind_korotettuna  990.0
                               :erilliskustannukset_laskutettu_ind_korotus      -10.0
                               :indeksit_kaikki_laskutetaan                     2362.70000000000000000
                               :indeksit_kaikki_laskutettu                      1735.70000000000000000
                               :indeksit_muu_kuin_kokhint_laskutetaan           36735.70000000000000000
                               :indeksit_muu_kuin_kokhint_laskutettu            62.70000000000000000
                               :kaikki_yhteensa_laskutetaan                     3887.680000000000076000000
                               :kaikki_yhteensa_laskutettu                      170.680000000000076000000
                               :kht_laskutetaan                        3500.0
                               :kht_laskutetaan_ind_korotettuna        3717.0
                               :kht_laskutetaan_ind_korotus            217.0
                               :kht_laskutettu                         35000.0
                               :kht_laskutettu_ind_korotettuna         36673.0
                               :kht_laskutettu_ind_korotus             1673.0
                               :muu_kuin_kokhint_laskutetaan                    3450.680000000000076000000
                               :muu_kuin_kokhint_laskutettu                     387.680000000000076000000
                               :muutostyot_laskutetaan                 0.0
                               :muutostyot_laskutetaan_ind_korotettuna 0.0
                               :muutostyot_laskutetaan_ind_korotus     0.0
                               :muutostyot_laskutettu                  1000.0
                               :muutostyot_laskutettu_ind_korotettuna  1059.0
                               :muutostyot_laskutettu_ind_korotus      59.0
                               :nimi                                   "Talvihoito"
                               :sakot_laskutetaan                      0.0
                               :sakot_laskutetaan_ind_korotettuna      0.0
                               :sakot_laskutetaan_ind_korotus          0.0
                               :sakot_laskutettu                       100.0
                               :sakot_laskutettu_ind_korotettuna       103.9
                               :sakot_laskutettu_ind_korotus           3.9
                               :suolasakot_laskutetaan                 2280.0
                               :suolasakot_laskutetaan_ind_korotettuna 2388.6800000000003
                               :suolasakot_laskutetaan_ind_korotus     108.68000000000008
                               :suolasakot_laskutettu                  0.0
                               :suolasakot_laskutettu_ind_korotettuna  0.0
                               :suolasakot_laskutettu_ind_korotus      0.0
                               :tuotekoodi                             "23100"
                               :yht_laskutetaan                        0.0
                               :yht_laskutetaan_ind_korotettuna        0.0
                               :yht_laskutetaan_ind_korotus            0.0
                               :yht_laskutettu                         200.0
                               :yht_laskutettu_ind_korotettuna         209.8
                               :yht_laskutettu_ind_korotus             9.8}

          odotetut-liikenneymparisto {:erilliskustannukset_laskutetaan                 0.0
                                      :erilliskustannukset_laskutetaan_ind_korotettuna 0.0
                                      :erilliskustannukset_laskutetaan_ind_korotus     0.0
                                      :erilliskustannukset_laskutettu                  0.0
                                      :erilliskustannukset_laskutettu_ind_korotettuna  0.0
                                      :erilliskustannukset_laskutettu_ind_korotus      0.0
                                      :indeksit_kaikki_laskutetaan                     4265.0000000000000000
                                      :indeksit_kaikki_laskutettu                      65.0000000000000000
                                      :indeksit_muu_kuin_kokhint_laskutetaan           65.0000000000000000
                                      :indeksit_muu_kuin_kokhint_laskutettu            65.0000000000000000
                                      :kaikki_yhteensa_laskutetaan                     186.00000000000000000
                                      :kaikki_yhteensa_laskutettu                      186.00000000000000000
                                      :kht_laskutetaan                        0.0
                                      :kht_laskutetaan_ind_korotettuna        0.0
                                      :kht_laskutetaan_ind_korotus            0.0
                                      :kht_laskutettu                         0.0
                                      :kht_laskutettu_ind_korotettuna         0.0
                                      :kht_laskutettu_ind_korotus             0.0
                                      :muu_kuin_kokhint_laskutetaan                    7186.00000000000000000
                                      :muu_kuin_kokhint_laskutettu                     186.00000000000000000
                                      :muutostyot_laskutetaan                 5000.0
                                      :muutostyot_laskutetaan_ind_korotettuna 5062.0
                                      :muutostyot_laskutetaan_ind_korotus     62.0
                                      :muutostyot_laskutettu                  2000.0
                                      :muutostyot_laskutettu_ind_korotettuna  2000.0
                                      :muutostyot_laskutettu_ind_korotus      0.0
                                      :nimi                                   "Liikenneympäristön hoito"
                                      :sakot_laskutetaan                      0.0
                                      :sakot_laskutetaan_ind_korotettuna      0.0
                                      :sakot_laskutetaan_ind_korotus          0.0
                                      :sakot_laskutettu                       1200.0
                                      :sakot_laskutettu_ind_korotettuna       1226.0
                                      :sakot_laskutettu_ind_korotus           26.0
                                      :suolasakot_laskutetaan                 0.0
                                      :suolasakot_laskutetaan_ind_korotettuna 0.0
                                      :suolasakot_laskutetaan_ind_korotus     0.0
                                      :suolasakot_laskutettu                  0.0
                                      :suolasakot_laskutettu_ind_korotettuna  0.0
                                      :suolasakot_laskutettu_ind_korotus      0.0
                                      :tuotekoodi                             "23110"
                                      :yht_laskutetaan                        5000.0
                                      :yht_laskutetaan_ind_korotettuna        2124.0
                                      :yht_laskutetaan_ind_korotus            124.0
                                      :yht_laskutettu                         1000.0
                                      :yht_laskutettu_ind_korotettuna         1039.0
                                      :yht_laskutettu_ind_korotus             39.0}

          odotetut-soratiet {:erilliskustannukset_laskutetaan                 0.0
                             :erilliskustannukset_laskutetaan_ind_korotettuna 0.0
                             :erilliskustannukset_laskutetaan_ind_korotus     0.0
                             :erilliskustannukset_laskutettu                  0.0
                             :erilliskustannukset_laskutettu_ind_korotettuna  0.0
                             :erilliskustannukset_laskutettu_ind_korotus      0.0
                             :indeksit_kaikki_laskutetaan                     0.0
                             :indeksit_kaikki_laskutettu                      4780.0000000000000000
                             :indeksit_muu_kuin_kokhint_laskutetaan           104780.0000000000000000
                             :indeksit_muu_kuin_kokhint_laskutettu            0.0
                             :kaikki_yhteensa_laskutetaan                     10669.6000000000000000
                             :kaikki_yhteensa_laskutettu                      49.6000000000000000
                             :kht_laskutetaan                        10000.0
                             :kht_laskutetaan_ind_korotettuna        10620.0
                             :kht_laskutetaan_ind_korotus            620.0
                             :kht_laskutettu                         100000.0
                             :kht_laskutettu_ind_korotettuna         104780.0
                             :kht_laskutettu_ind_korotus             4780.0
                             :muu_kuin_kokhint_laskutetaan                    849.6000000000000000
                             :muu_kuin_kokhint_laskutettu                     669.6000000000000000
                             :muutostyot_laskutetaan                 0.0
                             :muutostyot_laskutetaan_ind_korotettuna 0.0
                             :muutostyot_laskutetaan_ind_korotus     0.0
                             :muutostyot_laskutettu                  0.0
                             :muutostyot_laskutettu_ind_korotettuna  0.0
                             :muutostyot_laskutettu_ind_korotus      0.0
                             :nimi                                   "Soratien hoito"
                             :sakot_laskutetaan                      800.0
                             :sakot_laskutetaan_ind_korotettuna      849.6
                             :sakot_laskutetaan_ind_korotus          49.6
                             :sakot_laskutettu                       0.0
                             :sakot_laskutettu_ind_korotettuna       0.0
                             :sakot_laskutettu_ind_korotus           0.0
                             :suolasakot_laskutetaan                 0.0
                             :suolasakot_laskutetaan_ind_korotettuna 0.0
                             :suolasakot_laskutetaan_ind_korotus     0.0
                             :suolasakot_laskutettu                  0.0
                             :suolasakot_laskutettu_ind_korotettuna  0.0
                             :suolasakot_laskutettu_ind_korotus      0.0
                             :tuotekoodi                             "23120"
                             :yht_laskutetaan                        0.0
                             :yht_laskutetaan_ind_korotettuna        0.0
                             :yht_laskutetaan_ind_korotus            0.0
                             :yht_laskutettu                         0.0
                             :yht_laskutettu_ind_korotettuna         0.0
                             :yht_laskutettu_ind_korotus             0.0}

          ]

      (is (= odotetut-talvihoito haetut-tiedot-talvihoito) "laskutusyhteenvedon-tiedot talvihoito")
      (is (= odotetut-liikenneymparisto haetut-tiedot-liikenneymparisto) "laskutusyhteenvedon-tiedot liikenneympäristön hoito")
      (is (= odotetut-soratiet haetut-tiedot-soratiet) "laskutusyhteenvedon-tiedot sorateiden hoito"))))





