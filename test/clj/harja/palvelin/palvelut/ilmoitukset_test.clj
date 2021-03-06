(ns harja.palvelin.palvelut.ilmoitukset-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.domain.ilmoitukset :refer [+ilmoitustyypit+ ilmoitustyypin-nimi +ilmoitustilat+]]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.ilmoitukset :as ilmoitukset]
            [harja.domain.ilmoitukset :as ilmoitukset-domain]
            [harja.pvm :as pvm]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.kyselyt.konversio :as konv]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :hae-ilmoitukset (component/using
                                                (ilmoitukset/->Ilmoitukset)
                                                [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))

(deftest hae-ilmoitukset-sarakkeet
  (let []
    (is (oikeat-sarakkeet-palvelussa?
          [:id :urakka :ilmoitusid :ilmoitettu :valitetty :yhteydenottopyynto :otsikko :paikankuvaus :lisatieto
           :ilmoitustyyppi :selitteet :urakkatyyppi :sijainti :uusinkuittaus :tila

           [:tr :numero] [:tr :alkuosa] [:tr :loppuosa] [:tr :alkuetaisyys] [:tr :loppuetaisyys]
           [:ilmoittaja :etunimi] [:ilmoittaja :sukunimi] [:ilmoittaja :tyopuhelin] [:ilmoittaja :matkapuhelin]
           [:ilmoittaja :sahkoposti] [:ilmoittaja :tyyppi]
           [:lahettaja :etunimi] [:lahettaja :sukunimi] [:lahettaja :puhelinnumero] [:lahettaja :sahkoposti]

           [:kuittaukset 0 :id] [:kuittaukset 0 :kuitattu] [:kuittaukset 0 :vapaateksti] [:kuittaukset 0 :kuittaustyyppi]
           [:kuittaukset 0 :kuittaaja :etunimi] [:kuittaukset 0 :kuittaaja :sukunimi] [:kuittaukset 0 :kuittaaja :matkapuhelin]
           [:kuittaukset 0 :kuittaaja :tyopuhelin] [:kuittaukset 0 :kuittaaja :sahkoposti]
           [:kuittaukset 0 :kuittaaja :organisaatio] [:kuittaukset 0 :kuittaaja :ytunnus]
           [:kuittaukset 0 :kasittelija :etunimi] [:kuittaukset 0 :kasittelija :sukunimi]
           [:kuittaukset 0 :kasittelija :matkapuhelin] [:kuittaukset 0 :kasittelija :tyopuhelin]
           [:kuittaukset 0 :kasittelija :sahkoposti] [:kuittaukset 0 :kasittelija :organisaatio]
           [:kuittaukset 0 :kasittelija :ytunnus]]

          :hae-ilmoitukset
          {:hallintayksikko nil
           :urakka nil
           :tilat nil
           :tyypit [:kysely :toimepidepyynto :ilmoitus]
           :kuittaustyypit #{:kuittaamaton :vastaanotto :aloitus :lopetus}
           :aikavali nil
           :aloituskuittauksen-ajankohta :kaikki
           :hakuehto nil}))))

(deftest hae-ilmoituksia
  (let [parametrit {:hallintayksikko nil
                    :urakka          nil
                    :hoitokausi      nil
                    :aikavali        [(java.util.Date. 0 0 0) (java.util.Date.)]
                    :tyypit          +ilmoitustyypit+
                    :kuittaustyypit #{:kuittaamaton :vastaanotto :aloitus :lopetus}
                    :tilat           +ilmoitustilat+
                    :aloituskuittauksen-ajankohta :kaikki
                    :hakuehto        ""}
        ilmoitusten-maara-suoraan-kannasta (ffirst (q
                                                     (str "SELECT count(*) FROM ilmoitus;")))
        kuittausten-maara-suoraan-kannasta (ffirst (q
                                                     (str "SELECT count(*) FROM ilmoitustoimenpide;")))
        ilmoitusid-12347-kuittaukset-maara-suoraan-kannasta
        (ffirst (q (str "SELECT count(*) FROM ilmoitustoimenpide WHERE ilmoitusid = 12347;")))
        ilmoitukset-palvelusta (kutsu-palvelua (:http-palvelin jarjestelma)
                                               :hae-ilmoitukset +kayttaja-jvh+ parametrit)
        kuittaukset-palvelusta (mapv :kuittaukset ilmoitukset-palvelusta)
        kuittaukset-palvelusta-lkm (apply + (map count kuittaukset-palvelusta))
        ilmoitusid-12348 (first (filter #(= 12348 (:ilmoitusid %)) ilmoitukset-palvelusta))
        ilmoitusid-12348-kuittaukset (:kuittaukset ilmoitusid-12348)
        ilmoitusid-12347 (first (filter #(= 12347 (:ilmoitusid %)) ilmoitukset-palvelusta))
        ilmoitusid-12347-kuittaukset (:kuittaukset ilmoitusid-12347)
        uusin-kuittaus-ilmoitusidlle-12347 (:uusinkuittaus ilmoitusid-12347)
        uusin-kuittaus-ilmoitusidlle-12347-testidatassa (pvm/aikana (pvm/->pvm "18.12.2007") 19 17 30 000)]
    (doseq [i ilmoitukset-palvelusta]
      (is (#{:toimenpidepyynto :tiedoitus :kysely}
            (:ilmoitustyyppi i)) "ilmoitustyyppi"))
    (is (= 0 (count ilmoitusid-12348-kuittaukset)) "12348:lla ei kuittauksia")
    (is (= ilmoitusten-maara-suoraan-kannasta (count ilmoitukset-palvelusta)) "Ilmoitusten lukumäärä")
    (is (= kuittausten-maara-suoraan-kannasta kuittaukset-palvelusta-lkm) "Kuittausten lukumäärä")
    (is (= ilmoitusid-12347-kuittaukset-maara-suoraan-kannasta (count ilmoitusid-12347-kuittaukset)) "Ilmoitusidn 123347 kuittausten määrä")
    (is (= uusin-kuittaus-ilmoitusidlle-12347-testidatassa uusin-kuittaus-ilmoitusidlle-12347) "uusinkuittaus ilmoitukselle 12347")))

(deftest ilmoitus-myohassa-ilman-kuittauksia
  (let [myohastynyt-kysely {:ilmoitustyyppi :kysely :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/days 7))) :kuittaukset []}
        myohastynyt-toimenpidepyynto {:ilmoitustyyppi :toimenpidepyynto :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/days 7))) :kuittaukset []}
        myohastynyt-tiedoitus {:ilmoitustyyppi :tiedoitus :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/days 7))) :kuittaukset []}]
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-kysely)))
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-toimenpidepyynto)))
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-tiedoitus)))))

(deftest ilmoitus-myohassa-kun-kuittaus-myohassa
  (let [myohastynyt-kysely {:ilmoitustyyppi :kysely :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/hours 73)))
                            :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :lopetus}]}
        myohastynyt-toimenpidepyynto {:ilmoitustyyppi :toimenpidepyynto :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/minutes 11)))
                                      :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :vastaanotto}]}
        myohastynyt-tiedoitus {:ilmoitustyyppi :tiedoitus :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/hours 2)))
                               :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :vastaanotto}]}]
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-kysely)))
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-toimenpidepyynto)))
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-tiedoitus)))))

(deftest ilmoitus-myohassa-kun-kuittaus-vaaraa-tyyppia
  (let [myohastynyt-kysely {:ilmoitustyyppi :kysely :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/hours 75)))
                            :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :vastaanotto}]}
        myohastynyt-toimenpidepyynto {:ilmoitustyyppi :toimenpidepyynto :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/minutes 15)))
                                      :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :aloitus}]}
        myohastynyt-tiedoitus {:ilmoitustyyppi :tiedoitus :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/hours 2)))
                               :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :aloitus}]}]
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-kysely)))
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-toimenpidepyynto)))
    (is (true? (ilmoitukset/ilmoitus-myohassa? myohastynyt-tiedoitus)))))

(deftest ilmoitus-ei-myohassa
  (let [myohastynyt-kysely {:ilmoitustyyppi :kysely :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/hours 71)))
                            :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :lopetus}]}
        myohastynyt-toimenpidepyynto {:ilmoitustyyppi :toimenpidepyynto :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/minutes 9)))
                                      :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :vastaanotto}]}
        myohastynyt-tiedoitus {:ilmoitustyyppi :tiedoitus :ilmoitettu (c/to-sql-time (t/minus (t/now) (t/minutes 40)))
                               :kuittaukset    [{:kuitattu (c/to-sql-time (t/now)) :kuittaustyyppi :vastaanotto}]}]
    (is (false? (ilmoitukset/ilmoitus-myohassa? myohastynyt-kysely)))
    (is (false? (ilmoitukset/ilmoitus-myohassa? myohastynyt-toimenpidepyynto)))
    (is (false? (ilmoitukset/ilmoitus-myohassa? myohastynyt-tiedoitus)))))

(deftest ilmoituksen-tyyppi
 (let [kuittaamaton-ilmoitus {:aloitettu false :lopetettu false :vastaanotettu false}
       vastaanotettu-ilmoitus {:aloitettu false :lopetettu false :vastaanotettu true}
       aloitettu-ilmoitus {:aloitettu true :lopetettu false :vastaanotettu true}
       lopetettu-ilmoitus {:aloitettu true :lopetettu true :vastaanotettu true}]
   (is (= (:tila (ilmoitukset-domain/lisaa-ilmoituksen-tila kuittaamaton-ilmoitus)) :kuittaamaton))
   (is (= (:tila (ilmoitukset-domain/lisaa-ilmoituksen-tila vastaanotettu-ilmoitus)) :vastaanotto))
   (is (= (:tila (ilmoitukset-domain/lisaa-ilmoituksen-tila aloitettu-ilmoitus)) :aloitus))
   (is (= (:tila (ilmoitukset-domain/lisaa-ilmoituksen-tila lopetettu-ilmoitus)) :lopetus))))

(deftest aloituskuittausta-ei-annettu-alle-tunnissa
  (let [ilmoitus1 {:ilmoitettu (c/to-sql-time (t/now)) :kuittaukset [{:kuitattu       (c/to-sql-time (t/plus (t/now) (t/minutes 80)))
                                                                      :kuittaustyyppi :aloitus}]}
        ilmoitus2 {:ilmoitettu (c/to-sql-time (t/now)) :kuittaukset [{:kuitattu       (c/to-sql-time (t/plus (t/now) (t/minutes 55)))
                                                                      :kuittaustyyppi :vastaanotto}]}
        ilmoitus3 {:ilmoitettu (c/to-sql-time (t/now)) :kuittaukset []}]
    (is (false? (#'ilmoitukset/sisaltaa-aloituskuittauksen-aikavalilla? ilmoitus1 (t/hours 1))))
    (is (false? (#'ilmoitukset/sisaltaa-aloituskuittauksen-aikavalilla? ilmoitus2 (t/hours 1))))
    (is (false? (#'ilmoitukset/sisaltaa-aloituskuittauksen-aikavalilla? ilmoitus3 (t/hours 1))))))

(deftest aloituskuittaus-annettu-alle-tunnissa
  (let [ilmoitus {:ilmoitettu (c/to-sql-time (t/now)) :kuittaukset [{:kuitattu       (c/to-sql-time (t/plus (t/now) (t/minutes 25)))
                                                                     :kuittaustyyppi :aloitus}]}]
    (is (true? (#'ilmoitukset/sisaltaa-aloituskuittauksen-aikavalilla? ilmoitus (t/hours 1))))))