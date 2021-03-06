(ns harja.palvelin.raportointi.sanktioraportti-test
  (:require [clojure.test :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.toimenpidekoodit :refer :all]
            [harja.palvelin.palvelut.urakat :refer :all]
            [harja.testi :refer :all]
            [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]
            [harja.pvm :as pvm]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [harja.palvelin.komponentit.pdf-vienti :as pdf-vienti]
            [harja.palvelin.raportointi :as raportointi]
            [harja.palvelin.palvelut.raportit :as raportit]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :pdf-vienti (component/using
                                      (pdf-vienti/luo-pdf-vienti)
                                      [:http-palvelin])
                        :raportointi (component/using
                                       (raportointi/luo-raportointi)
                                       [:db :pdf-vienti])
                        :raportit (component/using
                                    (raportit/->Raportit)
                                    [:http-palvelin :db :raportointi :pdf-vienti])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :once (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))

(deftest raportin-suoritus-urakalle-toimii
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :suorita-raportti
                                +kayttaja-jvh+
                                {:nimi :sanktioraportti
                                 :konteksti "urakka"
                                 :urakka-id (hae-oulun-alueurakan-2014-2019-id)
                                 :parametrit {:alkupvm (c/to-date (t/local-date 2011 10 1))
                                              :loppupvm (c/to-date (t/local-date 2016 10 1))}})]
    (is (vector? vastaus))
    (is (= vastaus [:raportti {:nimi "Sanktioiden yhteenveto" :orientaatio :landscape} [:taulukko {:sheet-nimi "Sanktioiden yhteenveto" :otsikko "Oulun alueurakka 2014-2019, Sanktioiden yhteenveto ajalta 01.10.2011 - 01.10.2016" :oikealle-tasattavat-kentat #{1}} [{:otsikko "", :leveys 12} {:otsikko "Oulun alueurakka 2014-2019", :leveys 15}] [{:otsikko "Talvihoito"} ["Muistutukset (kpl)" 1] ["Sakko A (€)" "1000,00"] ["- Päätiet (€)" "1000,00"] ["- Muut tiet (€)" "0,00"] ["Sakko B (€)" "666,67"] ["- Päätiet (€)" "666,67"] ["- Muut tiet (€)" "0,00"] ["Talvihoito, sakot yht. (€)" "1666,67"] ["Talvihoito, indeksit yht. (€)" "1674,11"] {:otsikko "Muut tuotteet"} ["Muistutukset (kpl)" 1] ["Sakko A (€)" "0,00"] ["- Liikenneymp. hoito (€)" "0,00"] ["- Sorateiden hoito (€)" "0,00"] ["Sakko B (€)" "111,00"] ["- Liikenneymp. hoito (€)" "110,00"] ["- Sorateiden hoito (€)" "0,00"] ["Muut tuotteet, sakot yht. (€)" "111,00"] ["Muut tuotteet, indeksit yht. (€)" "111,50"] {:otsikko "Ryhmä C"} ["Ryhmä C, sakot yht. (€)" "123,00"] ["Ryhmä C, indeksit yht. (€)" "123,55"] {:otsikko "Yhteensä"} ["Muistutukset yht. (kpl)" 2] ["Indeksit yht. (€)" "1909,16"] ["Kaikki sakot yht. (€)" "1900,67"] ["Kaikki yht. (€)" "3809,83"]]]]))))


(deftest raportin-suoritus-hallintayksikolle-toimii-usean-vuoden-aikavalilla
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :suorita-raportti
                                +kayttaja-jvh+
                                {:nimi :sanktioraportti
                                 :konteksti "hallintayksikko"
                                 :hallintayksikko-id (hae-pohjois-pohjanmaan-hallintayksikon-id)
                                 :parametrit {:alkupvm (c/to-date (t/local-date 2011 10 1))
                                              :loppupvm (c/to-date (t/local-date 2016 10 1))
                                              :urakkatyyppi "hoito"}})]
    (is (vector? vastaus))
    (is (= vastaus [:raportti {:nimi "Sanktioiden yhteenveto"  :orientaatio :landscape} [:taulukko {:sheet-nimi "Sanktioiden yhteenveto" :oikealle-tasattavat-kentat #{1 2 3 4 5} :otsikko "Pohjois-Pohjanmaa, Sanktioiden yhteenveto ajalta 01.10.2011 - 01.10.2016"} [{:leveys 12 :otsikko ""} {:leveys 15 :otsikko "Kajaanin alueurakka 2014-2019"} {:leveys 15 :otsikko "Oulun alueurakka 2005-2012"} {:leveys 15 :otsikko "Oulun alueurakka 2014-2019"} {:leveys 15 :otsikko "Pudasjärven alueurakka 2007-2012"} {:leveys 15 :otsikko "Yh­teen­sä"}] [{:otsikko "Talvihoito"} ["Muistutukset (kpl)" 0 0 1 1 2] ["Sakko A (€)" "0,00" "0,00" "1000,00" "10000,00" "11000,00"] ["- Päätiet (€)" "0,00" "0,00" "1000,00" "10000,00" "11000,00"] ["- Muut tiet (€)" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Sakko B (€)" "0,00" "0,00" "666,67" "6660,00" "7326,67"] ["- Päätiet (€)" "0,00" "0,00" "666,67" "6660,00" "7326,67"] ["- Muut tiet (€)" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Talvihoito, sakot yht. (€)" "0,00" "0,00" "1666,67" "16660,00" "18326,67"] ["Talvihoito, indeksit yht. (€)" "0,00" "0,00" "1674,11" "0,00" "1674,11"] {:otsikko "Muut tuotteet"} ["Muistutukset (kpl)" 0 0 1 1 2] ["Sakko A (€)" "0,00" "0,00" "0,00" "0,00" "0,00"] ["- Liikenneymp. hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00"] ["- Sorateiden hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Sakko B (€)" "0,00" "0,00" "111,00" "1110,00" "1221,00"] ["- Liikenneymp. hoito (€)" "0,00" "0,00" "110,00" "1100,00" "1210,00"] ["- Sorateiden hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Muut tuotteet, sakot yht. (€)" "0,00" "0,00" "111,00" "1110,00" "1221,00"] ["Muut tuotteet, indeksit yht. (€)" "0,00" "0,00" "111,50" "0,00" "111,50"] {:otsikko "Ryhmä C"} ["Ryhmä C, sakot yht. (€)" "0,00" "0,00" "123,00" "1230,00" "1353,00"] ["Ryhmä C, indeksit yht. (€)" "0,00" "0,00" "123,55" "0,00" "123,55"] {:otsikko "Yhteensä"} ["Muistutukset yht. (kpl)" 0 0 2 2 4] ["Indeksit yht. (€)" "0,00" "0,00" "1909,16" "0,00" "1909,16"] ["Kaikki sakot yht. (€)" "0,00" "0,00" "1900,67" "19000,00" "20900,67"] ["Kaikki yht. (€)" "0,00" "0,00" "3809,83" "19000,00" "22809,83"]]]]))))

(deftest raportin-suoritus-hallintayksikolle-toimii-vuoden-aikavalilla
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :suorita-raportti
                                +kayttaja-jvh+
                                {:nimi :sanktioraportti
                                 :konteksti "hallintayksikko"
                                 :hallintayksikko-id (hae-pohjois-pohjanmaan-hallintayksikon-id)
                                 :parametrit {:alkupvm (c/to-date (t/local-date 2015 1 1))
                                              :loppupvm (c/to-date (t/local-date 2015 12 31))
                                              :urakkatyyppi "hoito"}})]
    (is (vector? vastaus))
    (is (= vastaus [:raportti {:nimi "Sanktioiden yhteenveto"  :orientaatio :landscape} [:taulukko {:sheet-nimi "Sanktioiden yhteenveto" :oikealle-tasattavat-kentat #{1 2 3} :otsikko "Pohjois-Pohjanmaa, Sanktioiden yhteenveto ajalta 01.01.2015 - 31.12.2015"} [{:leveys 12 :otsikko ""} {:leveys 15 :otsikko "Kajaanin alueurakka 2014-2019"} {:leveys 15 :otsikko "Oulun alueurakka 2014-2019"} {:leveys 15 :otsikko "Yh­teen­sä"}] [{:otsikko "Talvihoito"} ["Muistutukset (kpl)" 0 1 1] ["Sakko A (€)" "0,00" "1000,00" "1000,00"] ["- Päätiet (€)" "0,00" "1000,00" "1000,00"] ["- Muut tiet (€)" "0,00" "0,00" "0,00"] ["Sakko B (€)" "0,00" "666,67" "666,67"] ["- Päätiet (€)" "0,00" "666,67" "666,67"] ["- Muut tiet (€)" "0,00" "0,00" "0,00"] ["Talvihoito, sakot yht. (€)" "0,00" "1666,67" "1666,67"] ["Talvihoito, indeksit yht. (€)" "0,00" "1674,11" "1674,11"] {:otsikko "Muut tuotteet"} ["Muistutukset (kpl)" 0 1 1] ["Sakko A (€)" "0,00" "0,00" "0,00"] ["- Liikenneymp. hoito (€)" "0,00" "0,00" "0,00"] ["- Sorateiden hoito (€)" "0,00" "0,00" "0,00"] ["Sakko B (€)" "0,00" "111,00" "111,00"] ["- Liikenneymp. hoito (€)" "0,00" "110,00" "110,00"] ["- Sorateiden hoito (€)" "0,00" "0,00" "0,00"] ["Muut tuotteet, sakot yht. (€)" "0,00" "111,00" "111,00"] ["Muut tuotteet, indeksit yht. (€)" "0,00" "111,50" "111,50"] {:otsikko "Ryhmä C"} ["Ryhmä C, sakot yht. (€)" "0,00" "123,00" "123,00"] ["Ryhmä C, indeksit yht. (€)" "0,00" "123,55" "123,55"] {:otsikko "Yhteensä"} ["Muistutukset yht. (kpl)" 0 2 2] ["Indeksit yht. (€)" "0,00" "1909,16" "1909,16"] ["Kaikki sakot yht. (€)" "0,00" "1900,67" "1900,67"] ["Kaikki yht. (€)" "0,00" "3809,83" "3809,83"]]]]))))


(deftest raportin-suoritus-koko-maalle-toimii
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :suorita-raportti
                                +kayttaja-jvh+
                                {:nimi :sanktioraportti
                                 :konteksti "koko maa"
                                 :parametrit {:alkupvm (c/to-date (t/local-date 2015 1 1))
                                              :loppupvm (c/to-date (t/local-date 2015 12 31))
                                              :urakkatyyppi "hoito"}})]
    (is (vector? vastaus))
    (is (= vastaus [:raportti {:nimi "Sanktioiden yhteenveto" :orientaatio :landscape} [:taulukko {:sheet-nimi "Sanktioiden yhteenveto" :otsikko "KOKO MAA, Sanktioiden yhteenveto ajalta 01.01.2015 - 31.12.2015", :oikealle-tasattavat-kentat #{7 1 4 6 3 2 9 5 10 8}} [{:otsikko "", :leveys 12} {:otsikko "1 Uusimaa", :leveys 15} {:otsikko "2 Varsinais-Suomi", :leveys 15} {:otsikko "3 Kaakkois-Suomi", :leveys 15} {:otsikko "4 Pirkanmaa", :leveys 15} {:otsikko "8 Pohjois-Savo", :leveys 15} {:otsikko "9 Keski-Suomi", :leveys 15} {:otsikko "10 Etelä-Pohjanmaa", :leveys 15} {:otsikko "12 Pohjois-Pohjanmaa", :leveys 15} {:otsikko "14 Lappi", :leveys 15} {:otsikko "Yh­teen­sä", :leveys 15}] [{:otsikko "Talvihoito"} ["Muistutukset (kpl)" 0 0 0 0 0 0 0 1 0 1] ["Sakko A (€)" "3,50" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "1000,00" "0,00" "1003,50"] ["- Päätiet (€)" "3,50" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "1000,00" "0,00" "1003,50"] ["- Muut tiet (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Sakko B (€)" "3,50" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "666,67" "0,00" "670,17"] ["- Päätiet (€)" "3,50" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "666,67" "0,00" "670,17"] ["- Muut tiet (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Talvihoito, sakot yht. (€)" "7,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "1666,67" "0,00" "1673,67"] ["Talvihoito, indeksit yht. (€)" "7,03" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "1674,11" "0,00" "1681,14"] {:otsikko "Muut tuotteet"} ["Muistutukset (kpl)" 0 0 0 0 0 0 0 1 0 1] ["Sakko A (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00"] ["- Liikenneymp. hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00"] ["- Sorateiden hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Sakko B (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "111,00" "0,00" "111,00"] ["- Liikenneymp. hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "110,00" "0,00" "110,00"] ["- Sorateiden hoito (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00"] ["Muut tuotteet, sakot yht. (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "111,00" "0,00" "111,00"] ["Muut tuotteet, indeksit yht. (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "111,50" "0,00" "111,50"] {:otsikko "Ryhmä C"} ["Ryhmä C, sakot yht. (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "123,00" "0,00" "123,00"] ["Ryhmä C, indeksit yht. (€)" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "123,55" "0,00" "123,55"] {:otsikko "Yhteensä"} ["Muistutukset yht. (kpl)" 0 0 0 0 0 0 0 2 0 2] ["Indeksit yht. (€)" "7,03" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "1909,16" "0,00" "1916,19"] ["Kaikki sakot yht. (€)" "7,00" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "1900,67" "0,00" "1907,67"] ["Kaikki yht. (€)" "14,03" "0,00" "0,00" "0,00" "0,00" "0,00" "0,00" "3809,83" "0,00" "3823,86"]]]]))))