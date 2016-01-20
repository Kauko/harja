(ns harja.palvelin.palvelut.toteumat-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.toteumat :refer :all]
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
          :urakan-erilliskustannukset (component/using
                                        (->Toteumat)
                                        [:http-palvelin :db])
          :tallenna-erilliskustannus (component/using
                                       (->Toteumat)
                                       [:http-palvelin :db])
          :tallenna-muiden-toiden-toteuma (component/using
                                            (->Toteumat)
                                            [:http-palvelin :db])
          :tallenna-urakan-toteuma-ja-yksikkohintaiset-tehtavat (component/using
                                                                  (->Toteumat)
                                                                  [:http-palvelin :db])
          :tallenna-toteuma-ja-toteumamateriaalit (component/using
                                                    (->Toteumat)
                                                    [:http-palvelin :db])
          :hae-urakan-suolasakot-ja-lampotilat (component/using
                                                 (->Toteumat)
                                                 [:http-palvelin :db])
          :urakan-kokonaishintaisten-toteumien-reitit (component/using
                                                        (->Toteumat)
                                                        [:http-palvelin :db])
          :urakan-yksikkohintaisten-toteumien-reitit (component/using
                                                        (->Toteumat)
                                                        [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))

;; käyttää testidata.sql:stä tietoa
(deftest erilliskustannukset-haettu-oikein
         (let [alkupvm (java.sql.Date. 105 9 1)
               loppupvm (java.sql.Date. 106 10 30)
               res (kutsu-palvelua (:http-palvelin jarjestelma)
                                     :urakan-erilliskustannukset +kayttaja-jvh+
                                     {:urakka-id @oulun-alueurakan-2005-2010-id
                                      :alkupvm alkupvm
                                      :loppupvm loppupvm})
               oulun-alueurakan-toiden-lkm (ffirst (q
                                                     (str "SELECT count(*)
                                                       FROM erilliskustannus
                                                      WHERE sopimus IN (SELECT id FROM sopimus WHERE urakka = " @oulun-alueurakan-2005-2010-id
               ") AND pvm >= '2005-10-01' AND pvm <= '2006-09-30'")))]
           (is (= (count res) oulun-alueurakan-toiden-lkm) "Erilliskustannusten määrä")))

(deftest tallenna-erilliskustannus-testi
         (let [hoitokauden-alkupvm (java.sql.Date. 105 9 1) ;;1.10.2005
               hoitokauden-loppupvm (java.sql.Date. 106 10 30) ;;30.9.2006
               toteuman-pvm (java.sql.Date. 105 11 12)
               toteuman-lisatieto "Testikeissin lisätieto"
               ek {:urakka-id @oulun-alueurakan-2005-2010-id
                   :alkupvm hoitokauden-alkupvm
                   :loppupvm hoitokauden-loppupvm
                   :pvm toteuman-pvm :rahasumma 20000.0
                   :indeksin_nimi "MAKU 2005" :toimenpideinstanssi 1 :sopimus 1
                   :tyyppi "asiakastyytyvaisyysbonus" :lisatieto toteuman-lisatieto}
               maara-ennen-lisaysta (ffirst (q
                                       (str "SELECT count(*)
                                                       FROM erilliskustannus
                                                      WHERE sopimus IN (SELECT id FROM sopimus WHERE urakka = " @oulun-alueurakan-2005-2010-id
                                         ") AND pvm >= '2005-10-01' AND pvm <= '2006-09-30'")))
               res (kutsu-palvelua (:http-palvelin jarjestelma)
                     :tallenna-erilliskustannus +kayttaja-jvh+ ek)
               lisatty (first (filter #(and
                                  (= (:pvm %) toteuman-pvm)
                                  (= (:lisatieto %) toteuman-lisatieto)) res))]
           (is (= (:pvm lisatty) toteuman-pvm) "Tallennetun erilliskustannuksen pvm")
           (is (= (:lisatieto lisatty) toteuman-lisatieto) "Tallennetun erilliskustannuksen lisätieto")
           (is (= (:indeksin_nimi lisatty) "MAKU 2005") "Tallennetun erilliskustannuksen indeksin nimi")
           (is (= (:rahasumma lisatty) 20000.0) "Tallennetun erilliskustannuksen pvm")
           (is (= (:toimenpideinstanssi lisatty) 1) "Tallennetun erilliskustannuksen tp")
           (is (= (count res) (+ 1 maara-ennen-lisaysta)) "Tallennuksen jälkeen erilliskustannusten määrä")
           (u
             (str "DELETE FROM erilliskustannus
                    WHERE pvm = '2005-12-12' AND lisatieto = '" toteuman-lisatieto "'"))))


(deftest tallenna-muut-tyot-toteuma-testi
  (let [tyon-pvm (konv/sql-timestamp (java.util.Date. 105 11 24)) ;;24.12.2005
        hoitokausi-aloituspvm (java.sql.Date. 105 9 1)      ; 1.10.2005
        hoitokausi-lopetuspvm (java.sql.Date. 106 8 30)     ;30.9.2006
        toteuman-lisatieto "Testikeissin lisätieto2"
        tyo {:urakka-id @oulun-alueurakan-2005-2010-id :sopimus-id @oulun-alueurakan-2005-2010-paasopimuksen-id
             :alkanut tyon-pvm :paattynyt tyon-pvm
             :hoitokausi-aloituspvm hoitokausi-aloituspvm :hoitokausi-lopetuspvm hoitokausi-lopetuspvm
             :suorittajan-nimi "Alihankkijapaja Ky" :suorittajan-ytunnus "123456-Y"
             :tyyppi :muutostyo
             :lisatieto toteuman-lisatieto
             :tehtava {:paivanhinta 456, :maara 2, :toimenpidekoodi 1368}}
        maara-ennen-lisaysta (ffirst (q
                                       (str "SELECT count(*)
                                               FROM toteuma
                                              WHERE urakka = " @oulun-alueurakan-2005-2010-id "
                                                     AND sopimus = " @oulun-alueurakan-2005-2010-paasopimuksen-id "
                                                    AND tyyppi IN ('muutostyo', 'lisatyo', 'akillinen-hoitotyo', 'vahinkojen-korjaukset')
                                                    AND alkanut >= to_date('1-10-2005', 'DD-MM-YYYY')
                                                    AND paattynyt <= to_date('30-09-2006', 'DD-MM-YYYY');;")))
        res (kutsu-palvelua (:http-palvelin jarjestelma)
                            :tallenna-muiden-toiden-toteuma +kayttaja-jvh+ tyo)
        lisatty (first (filter #(and
                                 (= (:lisatieto %) toteuman-lisatieto)) res))]
    (is (= (count res) (+ 1 maara-ennen-lisaysta)) "Tallennuksen jälkeen muiden töiden määrä")
    (is (= (:alkanut lisatty) tyon-pvm) "Tallennetun muun työn alkanut pvm")
    (is (= (:paattynyt lisatty) tyon-pvm) "Tallennetun muun työn paattynyt pvm")
    (is (= (:tyyppi lisatty) :muutostyo) "Tallennetun muun työn tyyppi")
    (is (= (:lisatieto lisatty) toteuman-lisatieto) "Tallennetun erilliskustannuksen lisätieto")
    (is (= (get-in lisatty [:tehtava :paivanhinta]) 456.0) "Tallennetun muun työn päivänhinta")
    (is (= (get-in lisatty [:tehtava :maara]) 2.0) "Tallennetun muun työn määrä")
    (is (= (get-in lisatty [:tehtava :toimenpidekoodi]) 1368) "Tallennetun muun työn toimenpidekoodi")

    ;; siivotaan lisätyt rivit pois
    (u
      (str "DELETE FROM toteuma_tehtava
                    WHERE toteuma = " (get-in lisatty [:toteuma :id])))
    (u
      (str "DELETE FROM toteuma
                    WHERE id = "(get-in lisatty [:toteuma :id])))))


(deftest tallenna-yksikkohintainen-toteuma-testi
  (let [tyon-pvm (konv/sql-timestamp (java.util.Date. 105 11 24)) ;;24.12.2005
        hoitokausi-aloituspvm (java.sql.Date. 105 9 1)      ; 1.10.2005
        hoitokausi-lopetuspvm (java.sql.Date. 106 8 30)     ;30.9.2006
        toteuman-lisatieto "Testikeissin lisätieto4"
        tyo {:urakka-id @oulun-alueurakan-2005-2010-id :sopimus-id @oulun-alueurakan-2005-2010-paasopimuksen-id
             :alkanut tyon-pvm :paattynyt tyon-pvm
             :hoitokausi-aloituspvm hoitokausi-aloituspvm :hoitokausi-lopetuspvm hoitokausi-lopetuspvm
             :suorittajan-nimi "Alihankkijapaja Ky" :suorittajan-ytunnus "123456-Y"
             :tyyppi :yksikkohintainen
             :toteuma-id nil
             :lisatieto toteuman-lisatieto
             :tehtavat [{:toimenpidekoodi 1368 :maara 333}]}
        maara-ennen-lisaysta (ffirst (q
                                       (str "SELECT count(*)
                                               FROM toteuma
                                              WHERE urakka = " @oulun-alueurakan-2005-2010-id "
                                                    AND sopimus = " @oulun-alueurakan-2005-2010-paasopimuksen-id "
                                                    AND tyyppi IN ('yksikkohintainen') AND lisatieto = '" toteuman-lisatieto "';")))
        lisatty (kutsu-palvelua (:http-palvelin jarjestelma)
                            :tallenna-urakan-toteuma-ja-yksikkohintaiset-tehtavat +kayttaja-jvh+ tyo)
        ;;FIXME: tässä testissä voitaisiin testata myös hakupalvelu, ja varmistaa
        ;; että määrä lisäyksen jälkeen on yhden suurempi kuin ennen lisäystä
        #_maara-lisayksen-jalkeen #_(kutsu-palvelua (:http-palvelin jarjestelma)
                                                :urakan-toteutuneet-tehtavat +kayttaja-jvh+ {:urakka-id @oulun-alueurakan-2005-2010-id
                                                                                             :sopimus-id @oulun-alueurakan-2005-2010-paasopimuksen-id
                                                                                             :alkupvm hoitokausi-aloituspvm
                                                                                             :loppupvm hoitokausi-lopetuspvm
                                                                                             :tyyppi :yksikkohintainen})]
    (is (= (get-in lisatty [:toteuma :alkanut]) tyon-pvm) "Tallennetun työn alkanut pvm")
    (is (= (get-in lisatty [:toteuma :paattynyt]) tyon-pvm) "Tallennetun työn paattynyt pvm")
    (is (= (get-in lisatty [:toteuma :lisatieto]) toteuman-lisatieto) "Tallennetun työn lisätieto")
    (is (= (get-in lisatty [:toteuma :suorittajan-nimi]) "Alihankkijapaja Ky") "Tallennetun työn suorittajan nimi")
    (is (= (get-in lisatty [:toteuma :suorittajan-ytunnus]) "123456-Y") "Tallennetun työn suorittajan y-tunnus")
    (is (= (get-in lisatty [:toteuma :urakka-id]) @oulun-alueurakan-2005-2010-id) "Tallennetun työn urakan id")
    (is (= (get-in lisatty [:toteuma :urakka-id]) @oulun-alueurakan-2005-2010-paasopimuksen-id) "Tallennetun työn pääsopimuksen id")
    (is (= (get-in lisatty [:toteuma :tehtavat 0 :toimenpidekoodi]) 1368) "Tallennetun työn tehtävän toimenpidekoodi")
    (is (= (get-in lisatty [:toteuma :tehtavat 0 :maara]) 333) "Tallennetun työn tehtävän määrä")
    (is (= (get-in lisatty [:toteuma :tyyppi]) :yksikkohintainen) "Tallennetun työn toteuman tyyppi")

    (u
      (str "DELETE FROM toteuma_tehtava
                    WHERE toteuma = " (get-in lisatty [:toteuma :toteuma-id]) ";"))
    (u
      (str "DELETE FROM toteuma
                    WHERE id = "(get-in lisatty [:toteuma :toteuma-id])))))

;; TODO implementoi..
(deftest tallenna-toteuma-ja-toteumamateriaalit-test
  (let [[urakka sopimus] (first (q (str "SELECT urakka, id FROM sopimus WHERE urakka="@oulun-alueurakan-2005-2010-id)))
        toteuma (atom {:id -5, :urakka urakka :sopimus sopimus :alkanut (java.util.Date. 105 11 24) :paattynyt (java.util.Date. 105 11 24)
                 :tyyppi "yksikkohintainen" :suorittajan-nimi "UNIT TEST" :suorittajan-ytunnus 1234 :lisatieto "Unit test teki tämän"})
        tmt (atom [{:id -1 :materiaalikoodi 1 :maara 192837} {:materiaalikoodi 1 :maara 192837}])]

    (is (= 0 (ffirst (q "SELECT count(*) FROM toteuma_materiaali WHERE maara=192837 AND poistettu IS NOT TRUE"))))
    (is (= 0 (ffirst (q "SELECT count(*) FROM toteuma WHERE suorittajan_nimi='UNIT TEST' AND poistettu IS NOT TRUE"))))
    (is (nil? (kutsu-palvelua (:http-palvelin jarjestelma) :tallenna-toteuma-ja-toteumamateriaalit +kayttaja-jvh+
                              {:toteuma @toteuma
                               :toteumamateriaalit @tmt
                               ;:hoitokausi [(java.sql.Date. 105 9 1) (java.sql.Date. 106 8 30)]
                               :sopimus sopimus})))

    (let [tmidt (flatten (q "SELECT id FROM toteuma_materiaali WHERE maara=192837"))
          tid (ffirst (q "SELECT id from toteuma WHERE suorittajan_nimi='UNIT TEST'"))
          uusi-lisatieto "NYT PITÄIS OLLA MUUTTUNUT."]

      (is (= 2 (ffirst (q "SELECT count(*) FROM toteuma_materiaali WHERE maara=192837 AND poistettu IS NOT TRUE"))))
      (is (= 1 (ffirst (q "SELECT count(*) FROM toteuma WHERE suorittajan_nimi='UNIT TEST' AND poistettu IS NOT TRUE"))))

      (reset! tmt
              [(-> (assoc (first @tmt) :id (first tmidt))
                   (assoc :poistettu true))
               (-> (assoc (second @tmt) :id (second tmidt))
                   (assoc :maara 8712))])

      (reset! toteuma (-> (assoc @toteuma :id tid)
                          (assoc :lisatieto uusi-lisatieto)))

      (is (not (nil? (kutsu-palvelua (:http-palvelin jarjestelma) :tallenna-toteuma-ja-toteumamateriaalit +kayttaja-jvh+
                                     {:toteuma @toteuma
                                      :toteumamateriaalit @tmt
                                      :hoitokausi [(java.sql.Date. 105 9 1) (java.sql.Date. 106 8 30)]
                                      :sopimus sopimus}))))

      (is (= 1 (ffirst (q "SELECT count(*) FROM toteuma WHERE suorittajan_nimi='UNIT TEST' AND poistettu IS NOT TRUE"))))
      (is (= 1 (ffirst (q "SELECT count(*) FROM toteuma_materiaali WHERE maara=192837 AND poistettu IS TRUE"))))
      (is (= 1 (ffirst (q "SELECT count(*) FROM toteuma_materiaali WHERE maara=8712 AND poistettu IS NOT TRUE"))))

      (is (= uusi-lisatieto (ffirst (q "SELECT lisatieto FROM toteuma WHERE id="tid))))
      (is (= 8712 (int (ffirst (q "SELECT maara FROM toteuma_materiaali WHERE id="(second tmidt))))))

      (u "DELETE FROM toteuma_materiaali WHERE id in ("(clojure.string/join "," tmidt )")")
      (u "DELETE FROM toteuma WHERE id="tid))))

(deftest varustetoteumat-haettu-oikein
  (let [alkupvm (java.sql.Date. 105 9 1)
        loppupvm (java.sql.Date. 106 10 30)
        varustetoteumat (kutsu-palvelua (:http-palvelin jarjestelma)
                                        :urakan-varustetoteumat +kayttaja-jvh+
                                        {:urakka-id  @oulun-alueurakan-2005-2010-id
                                         :sopimus-id @oulun-alueurakan-2005-2010-paasopimuksen-id
                                         :alkupvm    alkupvm
                                         :loppupvm   loppupvm})]
    (is (>= (count varustetoteumat) 1))
    (is (contains? (first varustetoteumat) :reitti))))

(deftest kok-hint-toteumien-reitit-haettu-oikein
  (let [alkupvm (java.sql.Date. 100 9 1)
        loppupvm (java.sql.Date. 110 10 30)
        toteumat (kutsu-palvelua (:http-palvelin jarjestelma)
                                 :urakan-kokonaishintaisten-toteumien-reitit +kayttaja-jvh+
                                 {:urakka-id  @oulun-alueurakan-2005-2010-id
                                  :sopimus-id @oulun-alueurakan-2005-2010-paasopimuksen-id
                                  :alkupvm    alkupvm
                                  :loppupvm   loppupvm
                                  :toimenpide nil
                                  :tehtava    nil})]
    (is (>= (count toteumat) 3))
    (is (= :line (get-in toteumat [0 :reitti :type])))))

(deftest yks-hint-toteumien-reitit-haettu-oikein
  (let [alkupvm (java.sql.Date. 100 9 1)
        loppupvm (java.sql.Date. 110 10 30)
        toteumat (kutsu-palvelua (:http-palvelin jarjestelma)
                                 :urakan-yksikkohintaisten-toteumien-reitit +kayttaja-jvh+
                                 {:urakka-id  @oulun-alueurakan-2005-2010-id
                                  :sopimus-id @oulun-alueurakan-2005-2010-paasopimuksen-id
                                  :alkupvm    alkupvm
                                  :loppupvm   loppupvm
                                  :toimenpide nil
                                  :tehtava    nil})]
    (is (>= (count toteumat) 2))
    (is (>= (count (:reittipisteet (first toteumat))) 3))))
